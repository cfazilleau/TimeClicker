package dev.cfaz.timeclicker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import dev.cfaz.timeclicker.data.ColorLevel
import dev.cfaz.timeclicker.data.CustomColor

/**
 * A shade in OKLCH: lightness and chroma, the hue coming from the [CustomColor]. The natural levels match the preset
 * pastels; light and dark sit either side, in both themes.
 */
private class Shade(val lightness: Double, val chroma: Double)

private class LevelShades(val background: Shade, val content: Shade)

private val LightThemeShades = mapOf(
    ColorLevel.LIGHT to LevelShades(Shade(0.95, 0.035), Shade(0.40, 0.09)),
    ColorLevel.NATURAL to LevelShades(Shade(0.895, 0.065), Shade(0.37, 0.10)),
    ColorLevel.DARK to LevelShades(Shade(0.80, 0.10), Shade(0.28, 0.09)),
)

private val DarkThemeShades = mapOf(
    ColorLevel.LIGHT to LevelShades(Shade(0.45, 0.075), Shade(0.95, 0.04)),
    ColorLevel.NATURAL to LevelShades(Shade(0.345, 0.06), Shade(0.92, 0.055)),
    ColorLevel.DARK to LevelShades(Shade(0.285, 0.05), Shade(0.89, 0.06)),
)

/** Like the presets': a tinted shadow in the light theme, plain black in the dark one. */
private val LightThemeShadow = Shade(0.62, 0.12)
private val DarkThemeShadow = Color(0x99000000)

/** The tile colours of [color] in the light ([dark] false) or dark theme. */
fun customTileColors(color: CustomColor, dark: Boolean): TileColors {
    val hue = color.hue.toDouble()
    val shades = (if (dark) DarkThemeShades else LightThemeShades).getValue(color.level)
    return TileColors(
        background = oklch(shades.background.lightness, shades.background.chroma, hue),
        content = oklch(shades.content.lightness, shades.content.chroma, hue),
        shadow = if (dark) DarkThemeShadow else oklch(LightThemeShadow.lightness, LightThemeShadow.chroma, hue, alpha = 0.42f),
    )
}

/** [customTileColors] [darkness] of the way from the light theme to the dark one (0 or 1 outside a theme fade). */
fun customTileColors(color: CustomColor, darkness: Float): TileColors = when {
    darkness <= 0f -> customTileColors(color, dark = false)
    darkness >= 1f -> customTileColors(color, dark = true)
    else -> {
        val light = customTileColors(color, dark = false)
        val dark = customTileColors(color, dark = true)
        TileColors(
            lerp(light.background, dark.background, darkness),
            lerp(light.content, dark.content, darkness),
            lerp(light.shadow, dark.shadow, darkness),
        )
    }
}

/** The vivid colour drawn for [hue] on the picker's spectrum, the same lightness all the way along. */
fun spectrumColor(hue: Float): Color = oklch(SpectrumLightness, SpectrumChroma, hue.toDouble())

private const val SpectrumLightness = 0.72
private const val SpectrumChroma = 0.15
