package dev.cfaz.timeclicker.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.round
import androidx.compose.foundation.layout.offset
import dev.cfaz.timeclicker.data.TrackerGroup
import dev.cfaz.timeclicker.ui.theme.pressedIn
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import dev.cfaz.timeclicker.ui.components.TapSound
import dev.cfaz.timeclicker.ui.components.rememberPressAmount
import dev.cfaz.timeclicker.ui.theme.raised
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.cfaz.timeclicker.R
import dev.cfaz.timeclicker.data.PendingUndo
import dev.cfaz.timeclicker.data.Rhythm
import dev.cfaz.timeclicker.data.Tracker
import dev.cfaz.timeclicker.ui.components.NeuButton
import dev.cfaz.timeclicker.ui.components.NeuIconButton
import dev.cfaz.timeclicker.ui.components.PillButton
import dev.cfaz.timeclicker.ui.theme.AppIcons
import dev.cfaz.timeclicker.ui.theme.TimeClickerTheme
import dev.cfaz.timeclicker.ui.theme.rememberTileIcon
import dev.cfaz.timeclicker.widget.TileWidgets
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.absoluteValue

private val TileHeight = 156.dp
private val TileSpacing = 16.dp
private const val SwayDegrees = 0.8f
private const val SwayMillis = 240

/** Springy motion shared by page switches and sections moving in the list. */
private val PageSpring = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
private val SectionSpring = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset(1, 1))

@Composable
fun HomeScreen(
    onManageGroups: () -> Unit,
    onOpenIconPalette: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val undoable by viewModel.undoable.collectAsStateWithLifecycle()
    val palette = TimeClickerTheme.palette
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // The tile open in the edit sheet, by id: the sheet follows its count and last time as they change.
    var editingId by remember { mutableStateOf<Long?>(null) }
    var creating by remember { mutableStateOf(false) }
    // The group whose chip was held, open in the rename dialog.
    var renamingGroupId by remember { mutableStateOf<Long?>(null) }
    // While on, tiles are dragged to a new place or group instead of tapped.
    var reordering by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = reordering) { reordering = false }
    val canReorder = state?.let(::canReorder) == true

    val clickSound = TimeClickerTheme.settings.clickSound
    // Loaded ahead of the first tap, which would otherwise be silent.
    LaunchedEffect(clickSound) { if (clickSound) TapSound.preload(context) }

    Box(Modifier.fillMaxSize().background(palette.ground)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            TitleBar(
                reordering = reordering,
                onDoneReordering = { reordering = false },
                menu = {
                    HomeMenu(
                        canReorder = canReorder,
                        onReorder = { reordering = true },
                        onManageGroups = onManageGroups,
                        onOpenIconPalette = onOpenIconPalette,
                        onOpenSettings = onOpenSettings,
                        onOpenAbout = onOpenAbout,
                    )
                },
            )
            state?.let { s ->
                GroupPager(
                    state = s,
                    undoable = undoable,
                    reordering = reordering,
                    onReorder = viewModel::reorder,
                    onMove = viewModel::move,
                    onMoveToGroup = viewModel::moveToGroup,
                    onSelect = viewModel::select,
                    onEditGroup = { renamingGroupId = it },
                    photoFile = viewModel::photoFile,
                    onClick = { tracker ->
                        if (clickSound) TapSound.play(context)
                        viewModel.press(tracker)
                    },
                    onLongClick = { editingId = it.id },
                )
            }
        }

        AnimatedVisibility(
            visible = !reordering,
            enter = fadeIn() + scaleIn(initialScale = 0.6f),
            exit = fadeOut() + scaleOut(targetScale = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 20.dp),
        ) {
            AddButton(onClick = { creating = true })
        }
    }

    if (creating) {
        NewTileSheet(
            onCreate = { viewModel.create(it); creating = false },
            onDismiss = { creating = false },
        )
    }
    editingId?.let { id -> state?.trackers?.find { it.id == id } }?.let { saved ->
        val canPin = remember { TileWidgets.canPin(context) }
        // Read again when a press changes the tile's history.
        val rhythm by produceState<Rhythm?>(null, saved.id, saved.lastDoneAt) { value = viewModel.rhythm(saved.id) }
        EditTileSheet(
            saved = saved,
            rhythm = rhythm,
            groups = state?.groups.orEmpty(),
            photoFile = viewModel::photoFile,
            importPhoto = viewModel::importPhoto,
            createGroup = viewModel::createGroup,
            onSave = { viewModel.save(it, saved); editingId = null },
            onDiscard = { viewModel.discard(); editingId = null },
            onDelete = { viewModel.delete(saved.id); editingId = null },
            onRevertLastPress = { viewModel.revertLastPress(saved) },
            onRestorePress = { viewModel.restorePress(saved, it) },
            onAddWidget = if (canPin) ({ scope.launch { TileWidgets.requestPin(context, saved.id) } }) else null,
        )
    }
    renamingGroupId?.let { id -> state?.groups?.find { it.id == id } }?.let { group ->
        GroupDialog(
            title = stringResource(R.string.dialog_edit_group_title),
            initialName = group.name,
            initialIcon = group.icon,
            onConfirm = { name, icon -> viewModel.updateGroup(group, name, icon); renamingGroupId = null },
            onDismiss = { renamingGroupId = null },
        )
    }
}

/** Whether the page shown has a tile that can go somewhere else: two tiles to swap, or another group to move one to. */
private fun canReorder(state: HomeUiState): Boolean {
    val page = state.pages.getOrNull(state.selectedPage) ?: return false
    val tiles = page.sections.sumOf { it.trackers.size }
    // From All, a grouped tile can leave its group and the others can join one; from a group, only another group's chip.
    val otherGroups = if (page.chip.filter == GroupFilter.All) state.groups.size else state.groups.size - 1
    return tiles > 1 || (tiles == 1 && otherGroups > 0)
}

private val AddButtonSize = 64.dp

/** The main action, floating at the bottom centre: a new tile, filed in the group shown. */
@Composable
private fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = TimeClickerTheme.palette
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressAmount(interaction)
    Box(
        modifier
            .size(AddButtonSize)
            .graphicsLayer { val s = 1f - 0.06f * press(); scaleX = s; scaleY = s }
            .raised(CircleShape, palette.shadow, palette.highlight, distance = 6.dp, blur = 16.dp, pressed = press)
            .clip(CircleShape)
            .background(palette.accent)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.Add, stringResource(R.string.action_add_tile), tint = palette.onAccent, modifier = Modifier.size(28.dp))
    }
}

/**
 * The chips and one page per chip. Swiping left/right moves between groups; tapping a chip slides there.
 * Neighbouring pages shrink and fade a little while they slide in.
 */
@Composable
private fun GroupPager(
    state: HomeUiState,
    undoable: Map<Long, PendingUndo>,
    reordering: Boolean,
    onReorder: (List<Long>) -> Unit,
    onMove: (trackerId: Long, groupId: Long?, orderedIds: List<Long>) -> Unit,
    onMoveToGroup: (trackerId: Long, groupId: Long?) -> Unit,
    onSelect: (GroupFilter) -> Unit,
    onEditGroup: (Long) -> Unit,
    photoFile: (String) -> File,
    onClick: (Tracker) -> Unit,
    onLongClick: (Tracker) -> Unit,
) {
    val pages = state.pages
    val pagerState = rememberPagerState(initialPage = state.selectedPage) { pages.size }
    val scope = rememberCoroutineScope()

    // The settled page is the selected group (new tiles are filed in it).
    val currentPages by rememberUpdatedState(pages)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            currentPages.getOrNull(page)?.let { onSelect(it.chip.filter) }
        }
    }
    // Follow the selection when it moves without a swipe: its group was deleted, or groups were reordered.
    LaunchedEffect(state.selectedPage) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != state.selectedPage) {
            pagerState.animateScrollToPage(state.selectedPage, animationSpec = PageSpring)
        }
    }

    // A tile dragged in reorder mode floats above the chips and the page, in this box.
    val drag = rememberTileDrag()
    val origin = remember { arrayOf(Offset.Zero) }
    Box(Modifier.onGloballyPositioned { origin[0] = it.positionInRoot() }) {
        Column {
            // Without groups there is only "All": nothing to choose between.
            AnimatedVisibility(visible = pages.size > 1) {
                FilterChips(
                    pages = pages,
                    selected = pagerState.currentPage,
                    onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index, animationSpec = PageSpring) } },
                    onEditGroup = onEditGroup,
                    dropTarget = drag.overChip,
                    chipBounds = drag.chips,
                )
            }
            HorizontalPager(
                state = pagerState,
                // Lazy layout keys must be saveable in a Bundle: a string, not the GroupFilter itself.
                key = { pages[it].chip.filter.key },
                beyondViewportPageCount = 1,
                // A sideways drag moves the held tile, not the page.
                userScrollEnabled = !reordering,
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                val page = pages[index]
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val distance = pageDistance(pagerState, index).coerceIn(0f, 1f)
                            val scale = lerp(1f, 0.92f, distance)
                            scaleX = scale
                            scaleY = scale
                            alpha = lerp(1f, 0.35f, distance)
                        },
                ) {
                    if (state.trackers.isEmpty()) {
                        EmptyState(Modifier.fillMaxSize())
                    } else {
                        PageList(
                            page = page,
                            groups = state.groups,
                            undoable = undoable,
                            reordering = reordering,
                            drag = drag,
                            // Only the page in view, once settled, takes drags.
                            draggable = reordering && pagerState.settledPage == index,
                            onReorder = onReorder,
                            onMove = onMove,
                            onMoveToGroup = onMoveToGroup,
                            photoFile = photoFile,
                            onClick = onClick,
                            onLongClick = onLongClick,
                        )
                    }
                }
            }
        }
        drag.tracker?.let { tracker ->
            FloatingTile(drag, tracker, origin = { origin[0] }, undoUntil = undoable[tracker.id]?.until, photoFile = photoFile)
        }
    }
}

/** The tile being dragged, following the finger above everything; it shrinks into a chip it is dropped on. */
@Composable
private fun FloatingTile(drag: TileDrag, tracker: Tracker, origin: () -> Offset, undoUntil: Long?, photoFile: (String) -> File) {
    val density = LocalDensity.current
    val size = with(density) { DpSize(drag.size.width.toDp(), drag.size.height.toDp()) }
    // Smaller over a chip, so the chip it would go to shows.
    val overChip by animateFloatAsState(if (drag.overChip != null) 1f else 0f, label = "overChip")
    TileCard(
        tracker = tracker,
        undoUntil = undoUntil,
        photoFile = photoFile,
        onClick = {},
        onLongClick = null,
        enabled = false,
        modifier = Modifier
            .offset { (drag.topLeft - origin()).round() }
            .size(size)
            .graphicsLayer {
                val scale = (1f + (HeldScale - 1f) * drag.lift.value) * lerp(1f, 0.35f, overChip) * (1f - 0.7f * drag.vanish.value)
                // Around the finger, which stays on the same spot of the tile.
                if (drag.size.width > 0 && drag.size.height > 0) {
                    transformOrigin = TransformOrigin(drag.grab.x / drag.size.width, drag.grab.y / drag.size.height)
                }
                scaleX = scale
                scaleY = scale
                alpha = (1f - 0.25f * overChip) * (1f - drag.vanish.value)
            }
            // A copy of the tile left in the grid, which screen readers already read.
            .clearAndSetSemantics {},
    )
}

/** Where a tile being dragged would go: a hollow in the grid. */
@Composable
private fun TileSlot() {
    val palette = TimeClickerTheme.palette
    Box(
        Modifier
            .fillMaxSize()
            .clip(SlotShape)
            .background(palette.ground)
            .pressedIn(SlotShape, palette.shadow, palette.highlight),
    )
}

private val SlotShape = RoundedCornerShape(28.dp)
private val DropZoneHeight = 96.dp
private val AutoScrollZone = 80.dp
private val AutoScrollMax = 14.dp

private fun pageDistance(state: PagerState, page: Int): Float =
    ((state.currentPage - page) + state.currentPageOffsetFraction).absoluteValue

/**
 * One page's sections. In reorder mode, while it is the page in view ([draggable]), a drag on a tile moves it:
 * within its section, into another, or onto a chip; the list scrolls while the tile is held near its top or bottom.
 */
@Composable
private fun PageList(
    page: PageUi,
    groups: List<TrackerGroup>,
    undoable: Map<Long, PendingUndo>,
    reordering: Boolean,
    drag: TileDrag,
    draggable: Boolean,
    onReorder: (List<Long>) -> Unit,
    onMove: (trackerId: Long, groupId: Long?, orderedIds: List<Long>) -> Unit,
    onMoveToGroup: (trackerId: Long, groupId: Long?) -> Unit,
    photoFile: (String) -> File,
    onClick: (Tracker) -> Unit,
    onLongClick: (Tracker) -> Unit,
) {
    val listState = rememberLazyListState()
    val pageKey = page.chip.filter.key
    // Empty sections are only there to drop tiles into.
    val saved = page.sections.filter { reordering || !it.onlyWhileReordering }
    val onThisPage = drag.page == pageKey
    val shown = drag.sections?.takeIf { onThisPage } ?: saved
    val heldId = drag.tracker?.id?.takeIf { onThisPage }
    // The saved order caught up after a drop (or changed some other way): show it.
    val idle = drag.tracker == null
    LaunchedEffect(page.sections) { if (drag.tracker == null && drag.page == pageKey) drag.sections = null }
    LaunchedEffect(idle) { if (idle && onThisPage && drag.sections?.sameTiles(saved) == true) drag.sections = null }

    // Where each section is, by key, and where the list is: written by their layouts, read by the drag.
    val geometry = remember { HashMap<String, SectionGeometry>() }
    val listCoordinates = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val latestShown by rememberUpdatedState(shown)
    val latestOnMove by rememberUpdatedState(onMove)
    val latestOnMoveToGroup by rememberUpdatedState(onMoveToGroup)
    val density = LocalDensity.current
    val following = draggable && onThisPage && drag.dragging
    LaunchedEffect(following) {
        if (!following) return@LaunchedEffect
        val zone = with(density) { AutoScrollZone.toPx() }
        val max = with(density) { AutoScrollMax.toPx() }
        while (true) {
            withFrameNanos {}
            val list = listCoordinates[0]?.takeIf { it.isAttached }?.boundsInRoot() ?: continue
            val step = autoScrollStep(drag.finger.y, list.top, list.bottom, zone, max)
            if (step != 0f) listState.scrollBy(step)
            drag.follow(geometry, list)
        }
    }

    // Lazy lists keep the first visible item in place when items are inserted above it;
    // when already at the top, stay there so a newly created section is visible.
    // SideEffect runs before the list is measured, so it never shows a frame scrolled away from the top.
    val sectionKeys = shown.map { it.key }
    val seenKeys = remember { arrayOfNulls<List<String>>(1) }
    SideEffect {
        if (seenKeys[0] != sectionKeys) {
            seenKeys[0] = sectionKeys
            if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                listState.requestScrollToItem(0)
            }
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { listCoordinates[0] = it }
            .then(
                if (draggable) {
                    Modifier.pointerInput(Unit) {
                        detectTileDrag(
                            toWindow = { listCoordinates[0]?.takeIf { c -> c.isAttached }?.localToRoot(it) ?: it },
                            tileAt = { tileAt(latestShown, geometry, it) },
                            onPickUp = { hit, down, at -> drag.pickUp(pageKey, latestShown, hit, down, at) },
                            onMove = drag::moveTo,
                            onDrop = { drag.drop(geometry, latestOnMove) { id, group -> latestOnMoveToGroup(id, group) } },
                        )
                    }
                } else {
                    Modifier
                }
            ),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            // Room for the tiles' glow, which the list would otherwise clip.
            top = 22.dp,
            bottom = 120.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        items(shown, key = { it.key }) { section ->
            val sectionGeometry = remember { SectionGeometry() }
            DisposableEffect(section.key) {
                geometry[section.key] = sectionGeometry
                onDispose { geometry.remove(section.key, sectionGeometry) }
            }
            Section(
                section = section,
                geometry = sectionGeometry,
                heldId = heldId,
                // Tiles without a group get a heading of their own while they can be dragged to and from groups.
                ungroupedTitle = reordering && shown.size > 1,
                groups = groups,
                undoable = undoable,
                reordering = reordering,
                onReorder = onReorder,
                onMoveToGroup = onMoveToGroup,
                photoFile = photoFile,
                onClick = onClick,
                onLongClick = onLongClick,
                modifier = Modifier.animateItem(placementSpec = SectionSpring),
            )
        }
    }
}

/** The app's name and the [menu]; while reordering, how to reorder and a button to finish. */
@Composable
private fun TitleBar(reordering: Boolean, onDoneReordering: () -> Unit, menu: @Composable () -> Unit) {
    val palette = TimeClickerTheme.palette
    AnimatedContent(
        targetState = reordering,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentAlignment = Alignment.CenterStart,
        label = "titleBar",
    ) { isReordering ->
        Row(
            // As tall as the menu button either way, so the chips below don't move.
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isReordering) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reorder_tiles), style = MaterialTheme.typography.titleLarge, color = palette.text)
                    Text(stringResource(R.string.reorder_hint), style = MaterialTheme.typography.bodySmall, color = palette.muted)
                }
                PillButton(stringResource(R.string.action_done), onClick = onDoneReordering)
            } else {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.text,
                    modifier = Modifier.weight(1f),
                )
                menu()
            }
        }
    }
}

/** The gear button and what it opens: things to do with the tiles, then the app's other screens. */
@Composable
private fun HomeMenu(
    canReorder: Boolean,
    onReorder: () -> Unit,
    onManageGroups: () -> Unit,
    onOpenIconPalette: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val palette = TimeClickerTheme.palette
    var open by remember { mutableStateOf(false) }
    Box {
        NeuIconButton(
            icon = AppIcons.Settings,
            contentDescription = stringResource(R.string.action_menu),
            onClick = { open = true },
            size = 48.dp,
            shape = CircleShape,
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            offset = DpOffset(0.dp, 8.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = palette.ground,
        ) {
            @Composable
            fun Item(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
                DropdownMenuItem(
                    text = { Text(label, style = MaterialTheme.typography.titleSmall) },
                    leadingIcon = { Icon(icon, null, modifier = Modifier.size(20.dp)) },
                    enabled = enabled,
                    onClick = { open = false; onClick() },
                    colors = MenuDefaults.itemColors(
                        textColor = palette.text,
                        leadingIconColor = palette.text,
                        disabledTextColor = palette.muted,
                        disabledLeadingIconColor = palette.muted,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                )
            }
            Item(AppIcons.Reorder, stringResource(R.string.reorder_tiles), onReorder, enabled = canReorder)
            Item(AppIcons.Groups, stringResource(R.string.action_manage_groups), onManageGroups)
            Item(AppIcons.Palette, stringResource(R.string.settings_icon_palette), onOpenIconPalette)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = palette.field)
            Item(AppIcons.Settings, stringResource(R.string.action_settings), onOpenSettings)
            Item(AppIcons.Info, stringResource(R.string.about_title), onOpenAbout)
        }
    }
}

/**
 * The group chips, scrolling sideways. Holding a group's chip opens it for editing.
 * In reorder mode a tile can be dropped on a group's chip: each writes where it is to [chipBounds], and the one
 * of the [dropTarget] group grows while a tile is over it.
 */
@Composable
private fun FilterChips(
    pages: List<PageUi>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onEditGroup: (Long) -> Unit,
    dropTarget: Long?,
    chipBounds: MutableMap<Long, Rect>,
) {
    val palette = TimeClickerTheme.palette
    val haptics = LocalHapticFeedback.current
    val hapticsOn = TimeClickerTheme.settings.haptics
    val editLabel = stringResource(R.string.action_edit)
    // Padding inside the scroll area so the chips' shadows aren't clipped.
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        pages.forEachIndexed { index, page ->
            val chip = page.chip
            val isSelected = index == selected
            val label = chip.label ?: stringResource(R.string.chip_all)
            // Swiping to a group scrolls its chip into view.
            val bringIntoView = remember { BringIntoViewRequester() }
            LaunchedEffect(isSelected) { if (isSelected) bringIntoView.bringIntoView() }
            val groupId = (chip.filter as? GroupFilter.Group)?.id
            val onLongClick = groupId?.let { id -> { onEditGroup(id) } }
            val dropModifier = if (groupId != null) {
                DisposableEffect(groupId) { onDispose { chipBounds.remove(groupId) } }
                val target by animateFloatAsState(if (dropTarget == groupId) 1f else 0f, label = "chipDropTarget")
                Modifier
                    .onGloballyPositioned { chipBounds[groupId] = it.boundsInRoot() }
                    .graphicsLayer { val s = 1f + 0.12f * target; scaleX = s; scaleY = s }
            } else {
                Modifier
            }

            val content: @Composable () -> Unit = {
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val color = if (isSelected) palette.onAccent else palette.text
                    rememberTileIcon(chip.icon)?.let { Icon(it, null, tint = color, modifier = Modifier.size(18.dp)) }
                    Text(label, style = MaterialTheme.typography.titleSmall, color = color)
                    Text(chip.count.toString(), style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
                }
            }
            if (isSelected) {
                Box(
                    Modifier
                        .bringIntoViewRequester(bringIntoView)
                        .then(dropModifier)
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(palette.accent)
                        .combinedClickable(
                            role = Role.Tab,
                            onLongClickLabel = editLabel.takeIf { onLongClick != null },
                            onLongClick = onLongClick?.let {
                                {
                                    if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    it()
                                }
                            },
                            onClick = {},
                        )
                        .semantics { this.selected = true },
                    contentAlignment = Alignment.Center,
                ) { content() }
            } else {
                NeuButton(
                    onClick = { onSelect(index) },
                    shape = CircleShape,
                    // Lit up while a tile is held over it.
                    background = if (dropTarget == groupId && groupId != null) palette.field else palette.ground,
                    distance = 5.dp,
                    blur = 12.dp,
                    onLongClick = onLongClick,
                    onLongClickLabel = editLabel.takeIf { onLongClick != null },
                    modifier = Modifier.bringIntoViewRequester(bringIntoView).then(dropModifier).height(40.dp),
                ) { content() }
            }
        }
    }
}

/**
 * A group's heading and tiles. Where it is goes to [geometry], for dragging tiles in and out of it;
 * the tile held ([heldId]) shows as an empty slot. [ungroupedTitle] gives tiles without a group a heading too.
 */
@Composable
private fun Section(
    section: SectionUi,
    geometry: SectionGeometry,
    heldId: Long?,
    ungroupedTitle: Boolean,
    groups: List<TrackerGroup>,
    undoable: Map<Long, PendingUndo>,
    reordering: Boolean,
    onReorder: (List<Long>) -> Unit,
    onMoveToGroup: (trackerId: Long, groupId: Long?) -> Unit,
    photoFile: (String) -> File,
    onClick: (Tracker) -> Unit,
    onLongClick: (Tracker) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = TimeClickerTheme.palette
    Column(
        modifier.onGloballyPositioned { geometry.bounds = it.boundsInRoot() },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (val title = section.title) {
            is SectionTitle.Group -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                rememberTileIcon(title.icon)?.let { Icon(it, null, tint = palette.text, modifier = Modifier.size(24.dp)) }
                Text(title.name, style = MaterialTheme.typography.titleLarge, color = palette.text)
            }
            SectionTitle.None -> if (ungroupedTitle) {
                Text(stringResource(R.string.section_no_group), style = MaterialTheme.typography.titleLarge, color = palette.muted)
            }
        }
        if (section.trackers.isEmpty()) {
            if (reordering) {
                // A hollow to drop a tile into.
                val hint = if (section.groupId == null) R.string.reorder_drop_ungrouped else R.string.reorder_drop_here
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = DropZoneHeight)
                        .clip(SlotShape)
                        .background(palette.ground)
                        .pressedIn(SlotShape, palette.shadow, palette.highlight)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(hint), style = MaterialTheme.typography.bodyMedium, color = palette.muted, textAlign = TextAlign.Center)
                }
            } else {
                Text(stringResource(R.string.empty_group), style = MaterialTheme.typography.bodyMedium, color = palette.muted)
            }
        } else {
            // Tiles sway a little while they can be moved, the way home-screen icons do.
            val sway = if (reordering) {
                rememberInfiniteTransition(label = "sway").animateFloat(
                    initialValue = -SwayDegrees,
                    targetValue = SwayDegrees,
                    animationSpec = infiniteRepeatable(tween(SwayMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "swayAngle",
                )
            } else {
                null
            }
            val ids = section.trackers.map { it.id }
            val moveEarlier = stringResource(R.string.action_move_earlier)
            val moveLater = stringResource(R.string.action_move_later)
            // The other places a tile can go, as dragging it onto a section or a chip would.
            val groupMoves = groups.filter { it.id != section.groupId }.map { it.id to stringResource(R.string.action_move_to_group, it.name) }
            val leaveGroup = stringResource(R.string.action_remove_from_group).takeIf { section.groupId != null }
            DisposableEffect(Unit) { onDispose { geometry.gridOrigin = null } }
            BentoGrid(
                items = section.trackers,
                key = { it.id },
                sizeOf = { it.size },
                cellHeight = TileHeight,
                spacing = TileSpacing,
                geometry = geometry.grid,
                modifier = Modifier.onGloballyPositioned { geometry.gridOrigin = it.positionInRoot() },
            ) { tracker ->
                val reorderModifier = if (reordering) {
                    val index = ids.indexOf(tracker.id)
                    // Neighbours sway in opposite directions.
                    val direction = if (index % 2 == 0) 1f else -1f
                    Modifier
                        .graphicsLayer { rotationZ = (sway?.value ?: 0f) * direction }
                        // Dragging isn't possible with a screen reader: the same moves, one place at a time.
                        .semantics {
                            customActions = listOfNotNull(
                                CustomAccessibilityAction(moveEarlier) { onReorder(ids.moved(index, index - 1)); true }
                                    .takeIf { index > 0 },
                                CustomAccessibilityAction(moveLater) { onReorder(ids.moved(index, index + 1)); true }
                                    .takeIf { index < ids.lastIndex },
                            ) + groupMoves.map { (groupId, label) ->
                                CustomAccessibilityAction(label) { onMoveToGroup(tracker.id, groupId); true }
                            } + listOfNotNull(
                                leaveGroup?.let { CustomAccessibilityAction(it) { onMoveToGroup(tracker.id, null); true } },
                            )
                        }
                } else {
                    Modifier
                }
                if (tracker.id == heldId) {
                    TileSlot()
                } else {
                    TileCard(
                        tracker = tracker,
                        undoUntil = undoable[tracker.id]?.until,
                        photoFile = photoFile,
                        onClick = { onClick(tracker) },
                        onLongClick = { onLongClick(tracker) },
                        enabled = !reordering,
                        modifier = reorderModifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val palette = TimeClickerTheme.palette
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleLarge, color = palette.text)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
