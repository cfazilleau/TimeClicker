package dev.cfaz.timeclicker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class TileColorTest {

    @Test
    fun presetKeysRoundTrip() {
        PresetColor.entries.forEach { assertSame(it, TileColor.fromKey(it.key)) }
    }

    @Test
    fun customKeysRoundTrip() {
        val colors = listOf(0, 1, 180, 359).flatMap { hue -> ColorLevel.entries.map { CustomColor(hue, it) } }
        colors.forEach { assertEquals(it, TileColor.fromKey(it.key)) }
        assertEquals("custom:210:dark", CustomColor(210, ColorLevel.DARK).key)
    }

    @Test
    fun removedPresetsStillLoad() {
        // Rose and teal are no longer offered, but tiles and backups using them keep them.
        assertSame(PresetColor.ROSE, TileColor.fromKey("rose"))
        assertSame(PresetColor.TEAL, TileColor.fromKey("teal"))
        assert(PresetColor.ROSE !in PresetColor.pickable && PresetColor.TEAL !in PresetColor.pickable)
        assertEquals(10, PresetColor.pickable.size)
    }

    @Test
    fun unknownKeysFallBack() {
        assertSame(PresetColor.SAGE, TileColor.fromKey(null))
        assertSame(PresetColor.SAGE, TileColor.fromKey("mauve"))
        assertSame(PresetColor.SAGE, TileColor.fromKey("custom:"))
        assertSame(PresetColor.SAGE, TileColor.fromKey("custom:abc:light"))
        assertNull(CustomColor.fromKey("sage"))
    }

    @Test
    fun customKeysAreLenient() {
        // A hue out of range wraps, an unknown or missing level is natural.
        assertEquals(CustomColor(10, ColorLevel.LIGHT), TileColor.fromKey("custom:370:light"))
        assertEquals(CustomColor(350, ColorLevel.NATURAL), TileColor.fromKey("custom:-10:vivid"))
        assertEquals(CustomColor(42, ColorLevel.NATURAL), TileColor.fromKey("custom:42"))
    }
}
