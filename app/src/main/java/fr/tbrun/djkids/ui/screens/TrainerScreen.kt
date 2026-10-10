package fr.tbrun.djkids.ui.screens

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tbrun.djkids.game.Curriculum
import fr.tbrun.djkids.game.Judgement
import fr.tbrun.djkids.game.Level
import fr.tbrun.djkids.game.NoteValue
import fr.tbrun.djkids.game.Technique
import fr.tbrun.djkids.ui.DjViewModel
import fr.tbrun.djkids.ui.theme.Amber
import fr.tbrun.djkids.ui.theme.Cyan
import fr.tbrun.djkids.ui.theme.Lime
import fr.tbrun.djkids.ui.theme.Pink
import fr.tbrun.djkids.ui.theme.Surface1
import fr.tbrun.djkids.ui.theme.Surface2
import fr.tbrun.djkids.ui.theme.TextLo
import fr.tbrun.djkids.ui.theme.Violet
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TrainerScreen(vm: DjViewModel) {
    var technique by remember { mutableStateOf<Technique?>(null) }
    var level by remember { mutableStateOf<Level?>(null) }

    val tech = technique
    val lvl = level
    when {
        tech == null -> ChapterPicker { technique = it }
        lvl == null -> LevelPicker(vm, tech, onBack = { technique = null }, onPick = {
            level = it
            vm.trainerSelect(tech, it)
        })
        else -> PlayView(vm, tech, lvl, onQuit = { vm.trainerStop(); level = null }, onNext = {
            val next = tech.levels.getOrNull(lvl.index) // index est 1-based → élément suivant
            vm.trainerStop()
            if (next != null) { level = next; vm.trainerSelect(tech, next) } else { technique = null; level = null }
        })
    }
}

@Composable
private fun ChapterPicker(onPick: (Technique) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Apprendre le scratch", style = MaterialTheme.typography.titleLarge, color = Lime)
        Text("Choisis une technique", color = TextLo, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Curriculum.techniques) { t ->
                Column(
                    Modifier.padding(bottom = 10.dp).clip(RoundedCornerShape(14.dp))
                        .background(Surface2).border(1.dp, Violet.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                        .pointerInput(t.id) { detectTapGestures { onPick(t) } },
                ) {
                    Text(t.emoji, fontSize = 30.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(t.name, color = Cyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(t.description, color = TextLo, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun LevelPicker(vm: DjViewModel, tech: Technique, onBack: () -> Unit, onPick: (Level) -> Unit) {
    val unlocked = vm.unlockedLevel(tech.id)
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("←") }
            Spacer(Modifier.height(0.dp))
            Text("  ${tech.emoji} ${tech.name}", style = MaterialTheme.typography.titleLarge, color = Cyan)
        }
        Text(tech.howTo, color = TextLo, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn {
            items(tech.levels) { l ->
                val locked = l.index > unlocked
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (locked) Surface1 else Surface2)
                        .padding(14.dp)
                        .then(if (locked) Modifier else Modifier.pointerInput(l.index) { detectTapGestures { onPick(l) } }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (locked) "🔒" else "${l.index}", fontSize = 18.sp, color = if (locked) TextLo else Lime)
                    Spacer(Modifier.height(0.dp))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(l.title, color = if (locked) TextLo else Cyan, fontWeight = FontWeight.Bold)
                        Text("${l.bpm} BPM · ${l.bars} mesures", color = TextLo, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayView(vm: DjViewModel, tech: Technique, lvl: Level, onQuit: () -> Unit, onNext: () -> Unit) {
    val positionBeat by vm.trainer.positionBeat.collectAsStateWithLifecycle()
    val combo by vm.trainer.combo.collectAsStateWithLifecycle()
    val running by vm.trainer.running.collectAsStateWithLifecycle()
    val lastJudgement by vm.trainer.lastJudgement.collectAsStateWithLifecycle()
    val result by vm.trainer.result.collectAsStateWithLifecycle()

    val pattern = remember(lvl.index, tech.id) { vm.trainer.currentPattern() }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${tech.emoji} ${tech.name} — N${lvl.index}", color = Cyan, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onQuit) { Text("Quitter") }
        }
        Text("Combo x$combo", color = Amber, fontSize = 13.sp)

        // Autoroute de notes
        NoteHighway(
            beats = pattern?.hits?.map { it.beat to it.value } ?: emptyList(),
            position = positionBeat,
            modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 10.dp),
        )

        // Compte à rebours pendant le count-in (positionBeat < 0)
        if (running && positionBeat < 0) {
            val countBeat = ((-positionBeat).toInt() + 1).coerceIn(1, 4)
            Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
                Text("$countBeat", color = Amber, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            }
        } else {
            // Feedback de jugement
            Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
                lastJudgement?.let {
                    Text(it.label, color = when (it) {
                        Judgement.PERFECT -> Lime; Judgement.GOOD -> Cyan; Judgement.OK -> Amber; Judgement.MISS -> Pink
                    }, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
            }
        }

        Text(
            if (tech.usesCut) "Fais tourner le son et coupe sur le rythme : crossfader de la platine 🎛️ ou bouton CUT 👇"
            else "Scratche avec le jog de la platine 🎛️ ou au doigt 👇",
            color = TextLo, fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        )
        // Platine à scratcher : jog MK3 (via MIDI) ou tactile (vinyle animé)
        ScratchPad(
            enabled = running,
            onScrub = { vm.scratchScrub(it) },
            onStroke = { vm.scratchStroke() },
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp),
        )

        if (tech.usesCut) {
            Button(
                onClick = { vm.trainerCutTap() },
                enabled = running,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) { Text("✂️  CUT") }
        }

        if (!running && result == null) {
            Button(onClick = { vm.trainerStart() }, modifier = Modifier.fillMaxWidth()) {
                Text("▶  Go !")
            }
        }
    }

    result?.let { r ->
        vm.onTrainerResult(tech.id, lvl.index)
        ResultDialog(
            accuracy = r.accuracy, passed = r.passed, maxCombo = r.maxCombo,
            hasNext = lvl.index < tech.levels.size,
            onReplay = { vm.trainerSelect(tech, lvl); vm.trainerStart() },
            onNext = onNext,
            onQuit = onQuit,
        )
    }
}

@Composable
private fun NoteHighway(beats: List<Pair<Double, NoteValue>>, position: Double, modifier: Modifier) {
    val lookBack = 1.0
    val lookAhead = 4.0
    Canvas(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Surface1)) {
        val w = size.width; val h = size.height
        val nowX = (w * (lookBack / (lookBack + lookAhead))).toFloat()
        // Ligne "maintenant"
        drawLine(Lime, Offset(nowX, 0f), Offset(nowX, h), strokeWidth = 3f)
        // Graduations des temps
        val span = lookBack + lookAhead
        var b = kotlin.math.floor(position - lookBack)
        while (b <= position + lookAhead) {
            val x = nowX + ((b - position) / span).toFloat() * w
            if (x in 0f..w) drawLine(Color(0xFF2A2A38), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            b += 1.0
        }
        // Hits
        for ((beat, value) in beats) {
            if (beat < position - lookBack || beat > position + lookAhead) continue
            val x = nowX + ((beat - position) / span).toFloat() * w
            val radius = when (value) {
                NoteValue.QUARTER -> h * 0.22f
                NoteValue.EIGHTH -> h * 0.17f
                NoteValue.SIXTEENTH -> h * 0.12f
            }
            val color = when (value) {
                NoteValue.QUARTER -> Cyan; NoteValue.EIGHTH -> Amber; NoteValue.SIXTEENTH -> Pink
            }
            val hit = abs(beat - position) < 0.12
            drawCircle(if (hit) Lime else color, radius, Offset(x, h / 2))
        }
    }
}

@Composable
private fun ScratchPad(enabled: Boolean, onScrub: (Double) -> Unit, onStroke: () -> Unit, modifier: Modifier) {
    var lastSign by remember { mutableStateOf(0) }
    // currentSpeed is set by drag gestures; the animation loop reads it to rotate the vinyl.
    var currentSpeed by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }

    // Animate vinyl rotation driven by currentSpeed (deg/frame at ~60fps).
    LaunchedEffect(enabled) {
        while (true) {
            withInfiniteAnimationFrameMillis { frameMs ->
                if (enabled && abs(currentSpeed) > 0.001f) {
                    rotation += currentSpeed * 3f
                }
            }
        }
    }

    Box(
        modifier = modifier.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectDragGestures(
                onDragEnd = { onScrub(0.0); currentSpeed = 0f; lastSign = 0 },
                onDragCancel = { onScrub(0.0); currentSpeed = 0f; lastSign = 0 },
            ) { change, drag ->
                change.consume()
                val speed = drag.x.toDouble() * 0.06
                onScrub(speed)
                currentSpeed = (drag.x * 0.06f).coerceIn(-3f, 3f)
                val sign = if (drag.x > 1f) 1 else if (drag.x < -1f) -1 else 0
                if (sign != 0 && lastSign != 0 && sign != lastSign) onStroke()
                if (sign != 0) lastSign = sign
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        // Vinyl disc drawn with Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f * 0.95f
            val labelR = r * 0.32f

            // Outer vinyl (dark grey)
            drawCircle(Color(0xFF1A1A24), r, Offset(cx, cy))

            // Groove rings (subtle, semi-transparent)
            val grooveColor = Color(0xFF2A2A38)
            for (i in 1..10) {
                drawCircle(grooveColor, r * (0.38f + i * 0.055f), Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
            }

            // Border ring
            val borderColor = if (enabled) Cyan else TextLo
            drawCircle(borderColor, r, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

            // Centre label (colored circle)
            val labelColor = if (enabled) Violet else Color(0xFF303040)
            drawCircle(labelColor, labelR, Offset(cx, cy))

            // Rotating marker line on the label — shows vinyl spin
            rotate(rotation, pivot = Offset(cx, cy)) {
                val markerAngleRad = 0f // pointing up (12 o'clock)
                val mx = cx + (labelR * 0.7f) * sin(markerAngleRad)
                val my = cy - (labelR * 0.7f) * cos(markerAngleRad)
                drawLine(
                    color = if (enabled) Lime else TextLo,
                    start = Offset(cx, cy),
                    end = Offset(mx, my),
                    strokeWidth = 4f,
                )
                // Small dot at center
                drawCircle(Color(0xFF0E0E14), 6f, Offset(cx, cy))
            }
        }

        // Hint text overlay (only shown when stopped)
        if (!enabled) {
            Text("▶ Go !", color = TextLo, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultDialog(
    accuracy: Float, passed: Boolean, maxCombo: Int, hasNext: Boolean,
    onReplay: () -> Unit, onNext: () -> Unit, onQuit: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onQuit,
        title = { Text(if (passed) "🎉 Réussi !" else "Presque !", color = if (passed) Lime else Amber) },
        text = {
            Column {
                Text("Précision : ${(accuracy * 100).toInt()} %")
                Text("Meilleur combo : x$maxCombo")
                if (!passed) Text("Il faut un peu plus de précision pour débloquer la suite.", color = TextLo, fontSize = 12.sp)
            }
        },
        confirmButton = {
            if (passed && hasNext) Button(onClick = onNext) { Text("Niveau suivant →") }
            else Button(onClick = onReplay) { Text("Rejouer") }
        },
        dismissButton = { OutlinedButton(onClick = onQuit) { Text("Quitter") } },
    )
}
