package dev.cfaz.timeclicker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.cfaz.timeclicker.data.ColorLevel
import dev.cfaz.timeclicker.data.CustomColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class CustomColorsTest {

    private fun contrast(a: Color, b: Color): Float {
        val (light, dark) = max(a.luminance(), b.luminance()) to min(a.luminance(), b.luminance())
        return (light + 0.05f) / (dark + 0.05f)
    }

    private val all = (0 until 360 step 5).flatMap { hue -> ColorLevel.entries.map { CustomColor(hue, it) } }

    @Test
    fun textIsReadableOnEveryCustomColour() {
        for (dark in listOf(false, true)) {
            all.forEach { color ->
                val tile = customTileColors(color, dark)
                val ratio = contrast(tile.background, tile.content)
                assertTrue("$color (dark $dark): contrast $ratio", ratio >= 4.5f)
            }
        }
    }

    @Test
    fun levelsGoFromLightToDarkInBothThemes() {
        for (dark in listOf(false, true)) {
            (0 until 360 step 5).forEach { hue ->
                val (light, natural, deep) = ColorLevel.entries.map {
                    customTileColors(CustomColor(hue, it), dark).background.luminance()
                }
                assertTrue("hue $hue (dark $dark)", light > natural && natural > deep)
            }
        }
    }

    @Test
    fun followsTheThemeFade() {
        // The palette's darkness: 0 in the light theme, 1 in the dark one, in between while fading.
        val color = CustomColor(200, ColorLevel.NATURAL)
        val light = customTileColors(color, dark = false)
        val dark = customTileColors(color, dark = true)
        assertEquals(light, customTileColors(color, darkness = 0f))
        assertEquals(dark, customTileColors(color, darkness = 1f))
        val halfway = customTileColors(color, darkness = 0.5f).background.luminance()
        assertTrue(halfway < light.background.luminance() && halfway > dark.background.luminance())
    }
}
