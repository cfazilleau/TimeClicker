package dev.cfaz.timeclicker.ui.home

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.cfaz.timeclicker.data.TileSize
import kotlin.math.abs

/** Where a tile sits in the bento grid, in cells. */
data class BentoCell(val column: Int, val row: Int, val columns: Int, val rows: Int) {
    /** Whether the point, in cells (fractional), is on this tile. */
    fun contains(x: Float, y: Float): Boolean = x >= column && x < column + columns && y >= row && y < row + rows
}

/** Tiles are at least this wide: as many columns fit as can, two on a phone, more on a tablet or in landscape. */
val BentoMinCellWidth = 150.dp

object Bento {
    /** A phone's two columns, however narrow the screen. */
    const val MIN_COLUMNS = 2

    /** How many columns at least [minCellWidth] wide fit in [width], with [gap] between them (all in pixels). */
    fun columnCount(width: Int, minCellWidth: Int, gap: Int): Int =
        ((width + gap) / (minCellWidth + gap)).coerceAtLeast(MIN_COLUMNS)

    /**
     * Places tiles in order on a grid of [columnCount] columns, each at the first free spot
     * (top to bottom, left to right). Later small tiles fill holes left by wide/tall ones.
     */
    fun pack(sizes: List<TileSize>, columnCount: Int = 2): List<BentoCell> {
        val taken = mutableListOf<BooleanArray>()
        fun isFree(row: Int, column: Int) = row >= taken.size || !taken[row][column]
        fun take(cell: BentoCell) {
            while (taken.size < cell.row + cell.rows) taken.add(BooleanArray(columnCount))
            for (r in cell.row until cell.row + cell.rows) {
                for (c in cell.column until cell.column + cell.columns) taken[r][c] = true
            }
        }
        fun place(columns: Int, rows: Int): BentoCell {
            var row = 0
            while (true) {
                for (column in 0..columnCount - columns) {
                    val fits = (row until row + rows).all { r -> (column until column + columns).all { c -> isFree(r, c) } }
                    if (fits) return BentoCell(column, row, columns, rows).also(::take)
                }
                row++
            }
        }
        return sizes.map { place(it.columns.coerceAtMost(columnCount), it.rows) }
    }

    /**
     * Where to move the tile at [from] so it lands under the point ([x], [y], in cells), or null to leave it:
     * it is already there, or no place in the order puts it there. Of the places that do, the nearest one wins.
     *
     * Only accepting a move that puts the tile under the finger keeps a dragged tile from flipping back and forth
     * between two places as the others repack around it.
     */
    fun dropIndex(sizes: List<TileSize>, from: Int, x: Float, y: Float, columnCount: Int = 2): Int? {
        if (pack(sizes, columnCount)[from].contains(x, y)) return null
        return sizes.indices
            .filter { it != from }
            .sortedBy { abs(it - from) }
            .firstOrNull { to -> pack(sizes.moved(from, to), columnCount)[to].contains(x, y) }
    }

    /**
     * Where to insert a tile of [size], coming from another grid, so it lands under the point ([x], [y], in cells).
     * When no place does: first when the point is above the tiles (over a heading), else last.
     */
    fun insertIndex(sizes: List<TileSize>, size: TileSize, x: Float, y: Float, columnCount: Int = 2): Int =
        (0..sizes.size).firstOrNull { to -> pack(sizes.inserted(to, size), columnCount)[to].contains(x, y) }
            ?: if (y < 0) 0 else sizes.size
}

internal fun <T> List<T>.inserted(index: Int, item: T): List<T> = toMutableList().apply { add(index, item) }

internal fun <T> List<T>.moved(from: Int, to: Int): List<T> = toMutableList().apply { add(to, removeAt(from)) }

/** Tiles glide (with a little overshoot) to a new spot or size instead of jumping there. */
@OptIn(ExperimentalSharedTransitionApi::class)
private val ElasticBounds = BoundsTransform { _, _ ->
    spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = Rect.VisibilityThreshold)
}

/** The bento grid's columns, for a lazy grid of small tiles. */
object BentoColumns : GridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val count = Bento.columnCount(availableSize, BentoMinCellWidth.roundToPx(), spacing)
        return with(GridCells.Fixed(count)) { calculateCrossAxisCellSizes(availableSize, spacing) }
    }
}

/** The grid's columns and cell size in pixels, and where the tiles were put, from its last layout. */
class BentoGeometry {
    var columnCount = Bento.MIN_COLUMNS
        internal set
    var cells: List<BentoCell> = emptyList()
        internal set
    var cellWidth = 0
        internal set
    var rowHeight = 0
        internal set
    var gap = 0
        internal set

    /** The cell's top-left corner, from the grid's. */
    fun origin(cell: BentoCell) = Offset((cell.column * (cellWidth + gap)).toFloat(), (cell.row * (rowHeight + gap)).toFloat())
    fun size(cell: BentoCell) = IntSize(cell.columns * cellWidth + (cell.columns - 1) * gap, cell.rows * rowHeight + (cell.rows - 1) * gap)
    /** Where a point in the grid is, in cells (fractional, each with the gap after it). */
    fun columnAt(x: Float) = if (cellWidth == 0) 0f else x / (cellWidth + gap)
    fun rowAt(y: Float) = if (rowHeight == 0) 0f else y / (rowHeight + gap)
}

/**
 * Lays out [items] as a bento grid of as many columns as fit (see [BentoMinCellWidth]); each item's size comes from [sizeOf].
 * When tiles are added, removed, resized or reordered, or the screen rotates, the others move to their new place.
 * Each layout is written to [geometry], for finding the tile under a finger.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <T> BentoGrid(
    items: List<T>,
    key: (T) -> Any,
    sizeOf: (T) -> TileSize,
    cellHeight: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
    geometry: BentoGeometry = remember { BentoGeometry() },
    content: @Composable (T) -> Unit,
) {
    LookaheadScope {
        BentoLayout(
            sizes = items.map(sizeOf),
            cellHeight = cellHeight,
            spacing = spacing,
            geometry = geometry,
            modifier = modifier,
        ) {
            items.forEach { item ->
                key(key(item)) {
                    Box(
                        Modifier
                            .animateBounds(this@LookaheadScope, boundsTransform = ElasticBounds)
                            .fillMaxSize(),
                        propagateMinConstraints = true,
                    ) {
                        content(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoLayout(
    sizes: List<TileSize>,
    cellHeight: Dp,
    spacing: Dp,
    geometry: BentoGeometry,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    // Packed once per column count, not on every measure while tiles glide.
    val packings = remember(sizes) { HashMap<Int, List<BentoCell>>() }
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val columnCount = Bento.columnCount(constraints.maxWidth, BentoMinCellWidth.roundToPx(), gap)
        val cells = packings.getOrPut(columnCount) { Bento.pack(sizes, columnCount) }
        val cellWidth = (constraints.maxWidth - (columnCount - 1) * gap) / columnCount
        val rowHeight = cellHeight.roundToPx()
        geometry.columnCount = columnCount
        geometry.cells = cells
        geometry.cellWidth = cellWidth
        geometry.rowHeight = rowHeight
        geometry.gap = gap
        val rowCount = cells.maxOfOrNull { it.row + it.rows } ?: 0
        val placeables = measurables.mapIndexed { index, measurable ->
            val cell = cells[index]
            // Loose, so a tile can be smaller than its cell while it grows into it.
            measurable.measure(
                Constraints(
                    maxWidth = cell.columns * cellWidth + (cell.columns - 1) * gap,
                    maxHeight = cell.rows * rowHeight + (cell.rows - 1) * gap,
                )
            )
        }
        val height = if (rowCount == 0) 0 else rowCount * rowHeight + (rowCount - 1) * gap
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                val cell = cells[index]
                placeable.place(cell.column * (cellWidth + gap), cell.row * (rowHeight + gap))
            }
        }
    }
}
