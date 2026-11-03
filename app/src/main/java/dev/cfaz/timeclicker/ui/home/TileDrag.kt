package dev.cfaz.timeclicker.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import dev.cfaz.timeclicker.data.Tracker
import dev.cfaz.timeclicker.ui.theme.TimeClickerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** How a held tile grows, to show it is lifted off the grid. */
internal const val HeldScale = 1.05f
private val LiftSpring = spring<Float>(stiffness = Spring.StiffnessMedium)
private val SettleSpring = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = Offset(0.5f, 0.5f))
private const val VanishMillis = 220

/** Where one section of a page is on screen, from its last layout: where a dragged tile can be dropped. */
class SectionGeometry {
    /** The whole section, heading included, in the window; clipped to the list, so empty once scrolled out of it. */
    var bounds = Rect.Zero
    /** The tiles' top-left corner in the window; null while the section has none. */
    var gridOrigin: Offset? = null
    val grid = BentoGeometry()
}

/** A tile under a finger: where it is in the window, and how big. */
class TileHit(val tracker: Tracker, val topLeft: Offset, val size: IntSize)

/** The tile of [sections] under [point] (in the window), laid out as [geometry] (by section key) says; null if none. */
internal fun tileAt(sections: List<SectionUi>, geometry: Map<String, SectionGeometry>, point: Offset): TileHit? {
    for (section in sections) {
        val g = geometry[section.key] ?: continue
        val origin = g.gridOrigin ?: continue
        if (!g.bounds.contains(point)) continue
        val x = g.grid.columnAt(point.x - origin.x)
        val y = g.grid.rowAt(point.y - origin.y)
        val index = g.grid.cells.indexOfFirst { it.contains(x, y) }
        val tracker = section.trackers.getOrNull(index) ?: continue
        val cell = g.grid.cells[index]
        return TileHit(tracker, origin + g.grid.origin(cell), g.grid.size(cell))
    }
    return null
}

/** The sections with the tile [trackerId] taken out of its own and put at [index] of section [to] (or dropped, if not there). */
internal fun List<SectionUi>.withTile(trackerId: Long, to: String, index: Int): List<SectionUi> {
    val tracker = firstNotNullOfOrNull { s -> s.trackers.firstOrNull { it.id == trackerId } } ?: return this
    return map { section ->
        val others = section.trackers.filter { it.id != trackerId }
        section.copy(trackers = if (section.key == to) others.inserted(index.coerceIn(0, others.size), tracker) else others)
    }
}

/** Whether both show the same tiles in the same sections, in the same order. */
internal fun List<SectionUi>.sameTiles(other: List<SectionUi>): Boolean =
    map { s -> s.key to s.trackers.map { it.id } } == other.map { s -> s.key to s.trackers.map { it.id } }

/**
 * How far to scroll the list this frame with the finger at [y]: within [zone] of its [top] or [bottom] edge,
 * faster the closer to the edge, up to [max]; nothing elsewhere, outside the list included.
 */
internal fun autoScrollStep(y: Float, top: Float, bottom: Float, zone: Float, max: Float): Float = when {
    y < top || y > bottom -> 0f
    y < top + zone -> -max * (top + zone - y) / zone
    y > bottom - zone -> max * (y - (bottom - zone)) / zone
    else -> 0f
}

/**
 * A tile picked up in reorder mode, and dragged to another place in its group, into another group's section,
 * or onto a group's chip. Shared by the page it is dragged on, the chips, and the floating copy that follows the finger
 * (it can go over the chips, which the list would clip). Its place in the grids is left as an empty slot.
 */
@Stable
class TileDrag internal constructor(private val scope: CoroutineScope) {
    /** The tile held, until it has settled into its place. */
    var tracker by mutableStateOf<Tracker?>(null)
        private set
    /** The key of the page it is dragged on. */
    var page by mutableStateOf<String?>(null)
        private set
    /** That page's sections as shown, from the moment a tile is picked up until the saved ones catch up; null otherwise. */
    var sections by mutableStateOf<List<SectionUi>?>(null)
    /** While the finger is down. */
    var dragging by mutableStateOf(false)
        private set
    /** The finger, in the window. */
    var finger by mutableStateOf(Offset.Zero)
        private set
    /** The group whose chip is under the finger: let go there, the tile goes last in that group. */
    var overChip by mutableStateOf<Long?>(null)
        private set
    /** The held tile's size on the grid, in pixels. */
    var size by mutableStateOf(IntSize.Zero)
        private set
    /** 1 while the tile is lifted off the grid, 0 once it has settled. */
    val lift = Animatable(0f)
    /** From 0 to 1 while a tile dropped on a chip shrinks into it. */
    val vanish = Animatable(0f)
    /** The group chips in the window, by group id; written by the chips. */
    val chips = HashMap<Long, Rect>()

    /** Where the finger holds the tile, from its top-left corner. */
    var grab = Offset.Zero
        private set
    /** The sections when the tile was picked up: an unchanged drop saves nothing. */
    private var before: List<SectionUi> = emptyList()
    /** Once dropped, the tile glides from where it was let go into its place. */
    private var settling by mutableStateOf(false)
    private val settle = Animatable(Offset.Zero, Offset.VectorConverter)
    private var job: Job? = null
    internal var feedback: (HapticFeedbackType) -> Unit = {}

    /** The floating tile's top-left corner, in the window. */
    val topLeft: Offset get() = if (settling) settle.value else finger - grab

    /** Picks up [hit], on the page [page] showing [sections], held where the finger went [down], now at [at]. */
    fun pickUp(page: String, sections: List<SectionUi>, hit: TileHit, down: Offset, at: Offset) {
        job?.cancel()
        this.page = page
        this.sections = sections
        before = sections
        tracker = hit.tracker
        size = hit.size
        grab = down - hit.topLeft
        finger = at
        settling = false
        dragging = true
        job = scope.launch {
            vanish.snapTo(0f)
            lift.animateTo(1f, LiftSpring)
        }
        feedback(HapticFeedbackType.GestureThresholdActivate)
    }

    fun moveTo(at: Offset) {
        if (dragging) finger = at
    }

    /**
     * Moves the held tile to where the finger is, given the page's [geometry] (by section key) and its [list]'s bounds:
     * onto a chip of another group, or into the section under the finger (or the nearest one), so it lands under it.
     */
    fun follow(geometry: Map<String, SectionGeometry>, list: Rect) {
        val tracker = tracker ?: return
        val current = sections ?: return
        if (!dragging) return
        val here = current.firstOrNull { s -> s.trackers.any { it.id == tracker.id } } ?: return
        overChip = chips.entries.firstOrNull { (id, bounds) -> id != here.groupId && bounds.contains(finger) }?.key
        if (overChip != null || finger.y < list.top || finger.y > list.bottom) return

        val (section, g) = current
            .mapNotNull { s -> geometry[s.key]?.takeUnless { it.bounds.isEmpty }?.let { s to it } }
            .minByOrNull { (_, g) -> distance(g.bounds, finger.y) } ?: return
        val origin = g.gridOrigin
        // An empty section has no grid yet: the tile goes first.
        val x = if (origin == null) 0f else g.grid.columnAt(finger.x - origin.x)
        val y = if (origin == null) -1f else g.grid.rowAt(finger.y - origin.y)
        val sizes = section.trackers.map { it.size }
        val from = section.trackers.indexOfFirst { it.id == tracker.id }
        val to = if (from >= 0) {
            Bento.dropIndex(sizes, from, x, y, g.grid.columnCount) ?: return
        } else {
            Bento.insertIndex(sizes, tracker.size, x, y, g.grid.columnCount)
        }
        sections = current.withTile(tracker.id, section.key, to)
        feedback(HapticFeedbackType.SegmentTick)
    }

    /**
     * Lets go of the tile. On a chip, [onMoveToGroup] files it last in that group; elsewhere, if anything changed,
     * [onMove] gets the group of the section it is in and that section's new order.
     */
    fun drop(
        geometry: Map<String, SectionGeometry>,
        onMove: (trackerId: Long, groupId: Long?, orderedIds: List<Long>) -> Unit,
        onMoveToGroup: (trackerId: Long, groupId: Long) -> Unit,
    ) {
        if (!dragging) return
        dragging = false
        val tracker = tracker ?: return
        val current = sections ?: return
        // Left lit up while the tile shrinks into it.
        val chip = overChip
        val from = finger - grab
        feedback(HapticFeedbackType.GestureEnd)
        settling = true

        if (chip != null) {
            onMoveToGroup(tracker.id, chip)
            sections = current.firstOrNull { it.groupId == chip }
                ?.let { current.withTile(tracker.id, it.key, Int.MAX_VALUE) }
                ?: current.map { s -> s.copy(trackers = s.trackers.filter { it.id != tracker.id }) }
            // Shrinks into the chip, around the finger.
            val target = chips[chip]?.center?.minus(grab) ?: from
            job = scope.launch {
                settle.snapTo(from)
                launch { vanish.animateTo(1f, tween(VanishMillis)) }
                settle.animateTo(target, tween(VanishMillis))
                done()
            }
            return
        }

        val section = current.first { s -> s.trackers.any { it.id == tracker.id } }
        if (!current.sameTiles(before)) onMove(tracker.id, section.groupId, section.trackers.map { it.id })
        val g = geometry[section.key]
        val index = section.trackers.indexOfFirst { it.id == tracker.id }
        val target = g?.gridOrigin?.let { origin ->
            origin + g.grid.origin(Bento.pack(section.trackers.map { it.size }, g.grid.columnCount)[index])
        } ?: from
        job = scope.launch {
            launch { lift.animateTo(0f, LiftSpring) }
            settle.snapTo(from)
            settle.animateTo(target, SettleSpring)
            done()
        }
    }

    private fun done() {
        overChip = null
        tracker = null
        settling = false
    }

    private fun distance(bounds: Rect, y: Float): Float = when {
        y < bounds.top -> bounds.top - y
        y > bounds.bottom -> y - bounds.bottom
        else -> 0f
    }
}

@Composable
fun rememberTileDrag(): TileDrag {
    val scope = rememberCoroutineScope()
    val drag = remember { TileDrag(scope) }
    val haptics = LocalHapticFeedback.current
    val hapticsOn = TimeClickerTheme.settings.haptics
    SideEffect { drag.feedback = { if (hapticsOn) haptics.performHapticFeedback(it) } }
    return drag
}

/**
 * A drag that starts on a tile ([tileAt], in the window, which [toWindow] converts to) moves it at once, without holding first:
 * [onPickUp] once past the touch slop, with where the finger went down and where it is, then [onMove], then [onDrop].
 * The list under it doesn't scroll while it does; drags elsewhere still scroll it.
 */
internal suspend fun PointerInputScope.detectTileDrag(
    toWindow: (Offset) -> Offset,
    tileAt: (Offset) -> TileHit?,
    onPickUp: (hit: TileHit, down: Offset, at: Offset) -> Unit,
    onMove: (Offset) -> Unit,
    onDrop: () -> Unit,
) = awaitEachGesture {
    // Seen before the list (the initial pass goes parent first), so it can keep the list from scrolling.
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    val start = toWindow(down.position)
    val hit = tileAt(start) ?: return@awaitEachGesture
    var picked = false
    var travelled = Offset.Zero
    try {
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            val delta = change.positionChangeIgnoreConsumed()
            if (delta == Offset.Zero) continue
            change.consume()
            if (picked) {
                onMove(toWindow(change.position))
            } else {
                travelled += delta
                if (travelled.getDistance() > viewConfiguration.touchSlop) {
                    picked = true
                    onPickUp(hit, start, toWindow(change.position))
                }
            }
        }
    } finally {
        if (picked) onDrop()
    }
}
