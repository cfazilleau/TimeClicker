package dev.cfaz.timeclicker.ui.home

import dev.cfaz.timeclicker.data.TileSize.SMALL
import dev.cfaz.timeclicker.data.TileSize.TALL
import dev.cfaz.timeclicker.data.TileSize.WIDE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BentoTest {

    @Test
    fun smallTilesFillRowsLeftToRight() {
        assertEquals(
            listOf(BentoCell(0, 0, 1, 1), BentoCell(1, 0, 1, 1), BentoCell(0, 1, 1, 1)),
            Bento.pack(listOf(SMALL, SMALL, SMALL)),
        )
    }

    @Test
    fun tallTileLeavesRoomForTwoSmallOnesBesideIt() {
        assertEquals(
            listOf(BentoCell(0, 0, 1, 2), BentoCell(1, 0, 1, 1), BentoCell(1, 1, 1, 1), BentoCell(0, 2, 2, 1)),
            Bento.pack(listOf(TALL, SMALL, SMALL, WIDE)),
        )
    }

    @Test
    fun laterSmallTileFillsHoleBeforeAWideOne() {
        assertEquals(
            listOf(BentoCell(0, 0, 1, 1), BentoCell(0, 1, 2, 1), BentoCell(1, 0, 1, 1)),
            Bento.pack(listOf(SMALL, WIDE, SMALL)),
        )
    }

    @Test
    fun tallTileOnTheRightWhenLeftIsTaken() {
        assertEquals(
            listOf(BentoCell(0, 0, 1, 1), BentoCell(1, 0, 1, 2), BentoCell(0, 1, 1, 1)),
            Bento.pack(listOf(SMALL, TALL, SMALL)),
        )
    }

    @Test
    fun emptyListPacksToNothing() {
        assertEquals(emptyList<BentoCell>(), Bento.pack(emptyList()))
    }

    @Test
    fun fourColumnsFitSmallTilesBesideAWideAndATallOne() {
        assertEquals(
            listOf(BentoCell(0, 0, 2, 1), BentoCell(2, 0, 1, 2), BentoCell(3, 0, 1, 1), BentoCell(0, 1, 1, 1)),
            Bento.pack(listOf(WIDE, TALL, SMALL, SMALL), columnCount = 4),
        )
    }

    @Test
    fun phonesGetTwoColumnsEvenWhenNarrow() {
        // Widths in dp at density 1: tiles at least 150 wide, 16 apart.
        assertEquals(2, Bento.columnCount(width = 280, minCellWidth = 150, gap = 16))
        assertEquals(2, Bento.columnCount(width = 372, minCellWidth = 150, gap = 16))
    }

    @Test
    fun widerScreensGetAsManyColumnsAsFit() {
        assertEquals(3, Bento.columnCount(width = 560, minCellWidth = 150, gap = 16))
        assertEquals(4, Bento.columnCount(width = 760, minCellWidth = 150, gap = 16))
        // Exactly four tiles of 150 and three gaps.
        assertEquals(4, Bento.columnCount(width = 648, minCellWidth = 150, gap = 16))
        assertEquals(7, Bento.columnCount(width = 1240, minCellWidth = 150, gap = 16))
    }

    @Test
    fun draggedTileStaysWhileTheFingerIsOnIt() {
        assertNull(Bento.dropIndex(listOf(SMALL, SMALL, SMALL), from = 0, x = 0.9f, y = 0.5f))
    }

    @Test
    fun draggedTileMovesToWhereTheFingerIs() {
        // Bottom right of a 2x2 grid of small tiles is the last place.
        assertEquals(3, Bento.dropIndex(listOf(SMALL, SMALL, SMALL, SMALL), from = 0, x = 1.5f, y = 1.5f))
        // And back to the top left.
        assertEquals(0, Bento.dropIndex(listOf(SMALL, SMALL, SMALL, SMALL), from = 3, x = 0.2f, y = 0.2f))
    }

    @Test
    fun wideTileMovesUpToTheTop() {
        assertEquals(0, Bento.dropIndex(listOf(SMALL, SMALL, WIDE), from = 2, x = 1.5f, y = 0.5f))
    }

    @Test
    fun noMoveWhenNoOrderPutsTheTileUnderTheFinger() {
        // A small tile after the wide one would fill the hole above it, never land beside the finger below.
        assertNull(Bento.dropIndex(listOf(SMALL, SMALL, WIDE), from = 0, x = 0.5f, y = 2.5f))
    }

    @Test
    fun draggedTileMovesToTheThirdColumnOnAWiderGrid() {
        assertEquals(2, Bento.dropIndex(listOf(SMALL, SMALL, SMALL), from = 0, x = 2.5f, y = 0.5f, columnCount = 3))
    }

    @Test
    fun tileFromAnotherGroupGoesUnderTheFinger() {
        // Beside the first tile, then on the next row.
        assertEquals(1, Bento.insertIndex(listOf(SMALL, SMALL), SMALL, x = 1.5f, y = 0.5f))
        assertEquals(2, Bento.insertIndex(listOf(SMALL, SMALL), SMALL, x = 0.5f, y = 1.5f))
        assertEquals(0, Bento.insertIndex(listOf(SMALL, SMALL), SMALL, x = 0.5f, y = 0.5f))
    }

    @Test
    fun tileFromAnotherGroupGoesFirstOverTheHeadingAndLastBelowTheTiles() {
        assertEquals(0, Bento.insertIndex(listOf(SMALL, SMALL, SMALL), SMALL, x = 1.5f, y = -0.3f))
        assertEquals(3, Bento.insertIndex(listOf(SMALL, SMALL, SMALL), WIDE, x = 1.5f, y = 4f))
    }

    @Test
    fun tileDroppedOnAnEmptyGroupIsItsOnlyOne() {
        assertEquals(0, Bento.insertIndex(emptyList(), TALL, x = 0f, y = -1f))
    }
}
