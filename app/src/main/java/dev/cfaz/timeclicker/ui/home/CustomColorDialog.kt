package dev.cfaz.timeclicker.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.cfaz.timeclicker.R
import dev.cfaz.timeclicker.data.ColorLevel
import dev.cfaz.timeclicker.data.CustomColor
import dev.cfaz.timeclicker.data.TileColor
import dev.cfaz.timeclicker.ui.components.PillButton
import dev.cfaz.timeclicker.ui.components.SegmentedControl
import dev.cfaz.timeclicker.ui.theme.TimeClickerTheme
import dev.cfaz.timeclicker.ui.theme.spectrumColor
import kotlin.math.roundToInt

/**
 * Picks a [CustomColor]: a panel sliding down to the middle of the screen, with the tile previewed in the colour, the
 * hue spectrum to drag along, the three levels and a button confirming the choice. Back or a tap outside cancels.
 */
@Composable
fun CustomColorDialog(
    initial: CustomColor,
    /** The tile drawn in the colour being picked. */
    preview: @Composable (TileColor) -> Unit,
    onConfirm: (CustomColor) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = TimeClickerTheme.palette
    var hue by remember { mutableFloatStateOf(initial.hue.toFloat()) }
    var level by remember { mutableStateOf(initial.level) }
    val color = CustomColor(hue.roundToInt().mod(360), level)

    // Slides in on opening, and out before reporting how it was closed.
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    var result by remember { mutableStateOf<CustomColor?>(null) }
    fun close(confirmed: CustomColor?) {
        result = confirmed
        visible.targetState = false
    }
    LaunchedEffect(visible.isIdle, visible.currentState) {
        if (visible.isIdle && !visible.currentState) result?.let(onConfirm) ?: onDismiss()
    }

    Dialog(onDismissRequest = { close(null) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visibleState = visible,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
            ) {
                Column(
                    Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(palette.sheet)
                        .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.color_custom),
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.text,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    preview(color)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel(stringResource(R.string.color_hue))
                        HueSlider(hue = hue, onChange = { hue = it })
                    }
                    SegmentedControl(
                        options = ColorLevel.entries,
                        selected = level,
                        label = { stringResource(levelName(it)) },
                        onSelect = { level = it },
                        icon = { option, _ ->
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(palette.tile(CustomColor(color.hue, option)).background),
                            )
                        },
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { close(null) }) {
                            Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.titleSmall, color = palette.muted)
                        }
                        PillButton(text = stringResource(R.string.color_custom_confirm), onClick = { close(color) })
                    }
                }
            }
        }
    }
}

/** The full hue circle as a track: tap or drag along it to pick a hue in degrees. */
@Composable
private fun HueSlider(hue: Float, onChange: (Float) -> Unit) {
    val palette = TimeClickerTheme.palette
    val spectrum = remember { Brush.horizontalGradient((0..360 step 15).map { spectrumColor(it.toFloat()) }) }
    val description = stringResource(R.string.color_hue)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(ThumbSize)
            .semantics {
                contentDescription = description
                stateDescription = "${hue.roundToInt()}°"
                progressBarRangeInfo = ProgressBarRangeInfo(hue, 0f..359f, steps = 0)
                setProgress { onChange(it.coerceIn(0f, 359f)); true }
            }
            .pointerInput(Unit) {
                val radius = ThumbSize.toPx() / 2
                fun hueAt(x: Float) = ((x - radius) / (size.width - 2 * radius)).coerceIn(0f, 1f) * 359f
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onChange(hueAt(down.position.x))
                    drag(down.id) { change ->
                        change.consume()
                        onChange(hueAt(change.position.x))
                    }
                }
            },
    ) {
        val radius = size.height / 2
        val trackHeight = size.height * 0.6f
        drawRoundRect(
            brush = spectrum,
            topLeft = Offset(0f, (size.height - trackHeight) / 2),
            size = size.copy(height = trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2),
        )
        val center = Offset(radius + hue / 359f * (size.width - 2 * radius), size.height / 2)
        drawCircle(palette.shadow, radius = radius, center = center + Offset(0f, 2.dp.toPx()))
        drawCircle(spectrumColor(hue), radius = radius - ThumbBorder.toPx(), center = center)
        drawCircle(Color.White, radius = radius - ThumbBorder.toPx() / 2, center = center, style = Stroke(ThumbBorder.toPx()))
    }
}

private val ThumbSize = 32.dp
private val ThumbBorder = 4.dp

private fun levelName(level: ColorLevel) = when (level) {
    ColorLevel.LIGHT -> R.string.color_level_light
    ColorLevel.NATURAL -> R.string.color_level_natural
    ColorLevel.DARK -> R.string.color_level_dark
}
