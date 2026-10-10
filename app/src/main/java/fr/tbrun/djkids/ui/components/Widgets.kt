package fr.tbrun.djkids.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Platine vinyle : le repère blanc tourne selon [spinDegrees] (dérivé de la position de lecture),
 * donc il suit le scratch (avant/arrière) et la lecture normale.
 */
@Composable
fun Platter(
    playing: Boolean,
    progress: Float,
    color: Color,
    spinDegrees: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val angle = spinDegrees
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val r = size.minDimension / 2
            val c = Offset(size.width / 2, size.height / 2)
            drawCircle(color = Color(0xFF17171F), radius = r, center = c)
            drawCircle(color = color.copy(alpha = 0.6f), radius = r, center = c,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
            // Sillons
            for (i in 1..4) {
                drawCircle(color = Color(0xFF262633), radius = r * i / 5f, center = c,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
            }
            // Pastille centrale + repère tournant
            drawCircle(color = color, radius = r * 0.16f, center = c)
            val a = Math.toRadians(angle.toDouble())
            val mark = Offset(c.x + (r * 0.75f) * cos(a).toFloat(), c.y + (r * 0.75f) * sin(a).toFloat())
            drawCircle(color = Color.White, radius = r * 0.06f, center = mark)
            // Arc de progression
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(c.x - r * 0.92f, c.y - r * 0.92f),
                size = androidx.compose.ui.geometry.Size(r * 1.84f, r * 1.84f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
            )
        }
    }
}

/** Pad coloré. onDown/onUp pour gérer presser/relâcher (hot cue). */
@Composable
fun Pad(
    index: Int,
    active: Boolean,
    color: Color,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) color else color.copy(alpha = 0.18f))
            .border(1.5.dp, color.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                awaitPointerEventScopeLoop(onDown, onUp)
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${index + 1}",
            color = if (active) Color.Black else color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

/** Gros bouton rond (play, cue). */
@Composable
fun RoundButton(
    label: String,
    color: Color,
    active: Boolean,
    onDown: () -> Unit,
    onUp: () -> Unit = {},
    sizeDp: Int = 64,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(if (active) color else color.copy(alpha = 0.2f))
            .border(2.dp, color, CircleShape)
            .pointerInput(Unit) {
                awaitPointerEventScopeLoop(onDown, onUp)
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (active) Color.Black else color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/** Boucle d'attente press/release pour un geste bouton (momentané). */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.awaitPointerEventScopeLoop(
    onDown: () -> Unit,
    onUp: () -> Unit,
) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitPointerEvent()
            if (down.changes.any { it.pressed }) {
                onDown()
                // attendre le relâchement
                do {
                    val e = awaitPointerEvent()
                } while (e.changes.any { it.pressed })
                onUp()
            }
        }
    }
}
