package fr.tbrun.djkids.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import fr.tbrun.djkids.ui.theme.Surface2
import fr.tbrun.djkids.ui.theme.TextLo
import kotlin.math.roundToInt

/** Knob rotatif. Glisser verticalement règle la valeur 0..1. */
@Composable
fun Knob(
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
    size: Int = 56,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Canvas(
            modifier = Modifier
                .size(size.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        val next = (value - dragAmount.y / 150f).coerceIn(0f, 1f)
                        onValueChange(next)
                    }
                }
        ) {
            val stroke = this.size.minDimension * 0.10f
            val radius = this.size.minDimension / 2 - stroke
            val center = Offset(this.size.width / 2, this.size.height / 2)
            // Arc de fond (270°, de 135° à 405°).
            drawArc(
                color = Surface2,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Arc de valeur.
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * value,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(label, color = TextLo, fontSize = 11.sp)
    }
}

/** Fader vertical (volume / tempo). Glisser vers le haut augmente. */
@Composable
fun VerticalFader(
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
    heightDp: Int = 160,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(heightDp.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Surface2)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        val next = (value - dragAmount.y / heightDp.dp.toPx()).coerceIn(0f, 1f)
                        onValueChange(next)
                    }
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((heightDp * value).dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color),
            )
            // Repère du curseur.
            Box(
                modifier = Modifier
                    .padding(bottom = (heightDp * value - 4).coerceAtLeast(0f).dp)
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White),
            )
        }
        Text(label, color = TextLo, fontSize = 11.sp)
        Text("${(value * 100).roundToInt()}", color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

/** Fader horizontal (crossfader). */
@Composable
fun HorizontalFader(
    value: Float,
    onValueChange: (Float) -> Unit,
    leftColor: Color,
    rightColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    val next = (value + dragAmount.x / size.width).coerceIn(0f, 1f)
                    onValueChange(next)
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            val knobW = 26.dp.toPx()
            val x = (size.width - knobW) * value
            // demi-teintes gauche/droite
            drawRect(leftColor.copy(alpha = 0.25f), size = androidx.compose.ui.geometry.Size(size.width / 2, size.height))
            drawRect(rightColor.copy(alpha = 0.25f), topLeft = Offset(size.width / 2, 0f), size = androidx.compose.ui.geometry.Size(size.width / 2, size.height))
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(x, size.height * 0.12f),
                size = androidx.compose.ui.geometry.Size(knobW, size.height * 0.76f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
            )
        }
    }
}
