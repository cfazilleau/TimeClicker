package dev.cfaz.timeclicker.ui.home

import dev.cfaz.timeclicker.data.PresetColor
import dev.cfaz.timeclicker.data.TileIcon
import dev.cfaz.timeclicker.data.TileSize
import dev.cfaz.timeclicker.data.Tracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TileDragTest {

    private fun tile(id: Long, groupId: Long?) =
        Tracker(id, "Tile $id", Instant.EPOCH, groupId, PresetColor.pickable.first(), TileIcon.NONE, TileSize.SMALL, photo = null)

    private val sections = listOf(
        SectionUi("ungrouped", SectionTitle.None, null, listOf(tile(1, null))),
        SectionUi("group-7", SectionTitle.Group("Home", TileIcon.NONE), 7, listOf(tile(2, 7), tile(3, 7))),
        SectionUi("group-8", SectionTitle.Group("Empty", TileIcon.NONE), 8, emptyList(), onlyWhileReordering = true),
    )

    private fun List<SectionUi>.ids() = associate { s -> s.key to s.trackers.map { it.id } }

    @Test
    fun tileMovesIntoAnotherSectionAtThePlaceGiven() {
        assertEquals(
            mapOf("ungrouped" to emptyList(), "group-7" to listOf(2L, 1L, 3L), "group-8" to emptyList<Long>()),
            sections.withTile(1, "group-7", 1).ids(),
        )
    }

    @Test
    fun tileMovesIntoAnEmptyGroup() {
        assertEquals(
            mapOf("ungrouped" to listOf(1L), "group-7" to listOf(2L), "group-8" to listOf(3L)),
            sections.withTile(3, "group-8", 0).ids(),
        )
    }

    @Test
    fun tileMovesWithinItsSection() {
        assertEquals(listOf(3L, 2L), sections.withTile(3, "group-7", 0).ids()["group-7"])
    }

    @Test
    fun placePastTheEndPutsTheTileLast() {
        assertEquals(listOf(3L, 2L), sections.withTile(2, "group-7", Int.MAX_VALUE).ids()["group-7"])
    }

    @Test
    fun movingBackWhereItWasChangesNothing() {
        val there = sections.withTile(2, "ungrouped", 0)
        assertFalse(there.sameTiles(sections))
        assertTrue(there.withTile(2, "group-7", 0).sameTiles(sections))
    }

    @Test
    fun listScrollsOnlyNearItsEdges() {
        // A list from 100 to 1100, scrolling within 80 of its edges, up to 10 a frame.
        assertEquals(0f, autoScrollStep(600f, 100f, 1100f, 80f, 10f), 0f)
        assertEquals(-10f, autoScrollStep(100f, 100f, 1100f, 80f, 10f), 0.001f)
        assertEquals(-5f, autoScrollStep(140f, 100f, 1100f, 80f, 10f), 0.001f)
        assertEquals(5f, autoScrollStep(1060f, 100f, 1100f, 80f, 10f), 0.001f)
        // Over the chips above the list: no scrolling.
        assertEquals(0f, autoScrollStep(60f, 100f, 1100f, 80f, 10f), 0f)
    }
}
