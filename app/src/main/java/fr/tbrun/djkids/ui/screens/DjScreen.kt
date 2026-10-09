package fr.tbrun.djkids.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tbrun.djkids.audio.Deck
import fr.tbrun.djkids.model.DeckId
import fr.tbrun.djkids.ui.DjViewModel
import fr.tbrun.djkids.ui.components.HorizontalFader
import fr.tbrun.djkids.ui.components.Knob
import fr.tbrun.djkids.ui.components.Pad
import fr.tbrun.djkids.ui.components.Platter
import fr.tbrun.djkids.ui.components.RoundButton
import fr.tbrun.djkids.ui.components.VerticalFader
import fr.tbrun.djkids.ui.theme.DeckAColor
import fr.tbrun.djkids.ui.theme.DeckBColor
import fr.tbrun.djkids.ui.theme.Lime
import fr.tbrun.djkids.ui.theme.TextLo
import fr.tbrun.djkids.util.queryDisplayName
import androidx.compose.runtime.LaunchedEffect

@Composable
fun DjScreen(vm: DjViewModel) {
    val context = LocalContext.current
    var pendingDeck by remember { mutableStateOf<DeckId?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument) { uri ->
        val d = pendingDeck
        if (uri != null && d != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            vm.loadTrack(d, uri, queryDisplayName(context, uri))
        }
        pendingDeck = null
    }
    fun openPicker(d: DeckId) {
        pendingDeck = d
        picker.launch(arrayOf("audio/*"))
    }

    // La touche LOAD du contrôleur ouvre aussi le sélecteur.
    LaunchedEffect(Unit) {
        vm.engine.loadRequests.collect { openPicker(it) }
    }

    Row(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        DeckColumn(vm.engine.deckA, DeckAColor, { openPicker(DeckId.A) }, Modifier.weight(1f))
        MixerColumn(vm, Modifier.width(120.dp))
        DeckColumn(vm.engine.deckB, DeckBColor, { openPicker(DeckId.B) }, Modifier.weight(1f))
    }
}

@Composable
private fun DeckColumn(deck: Deck, color: Color, onLoad: () -> Unit, modifier: Modifier = Modifier) {
    val playing by deck.playing.collectAsStateWithLifecycle()
    val title by deck.title.collectAsStateWithLifecycle()
    val pos by deck.positionMs.collectAsStateWithLifecycle()
    val dur by deck.durationMs.collectAsStateWithLifecycle()
    val tempo by deck.tempo.collectAsStateWithLifecycle()
    val low by deck.eqLow.collectAsStateWithLifecycle()
    val mid by deck.eqMid.collectAsStateWithLifecycle()
    val high by deck.eqHigh.collectAsStateWithLifecycle()
    val vol by deck.channelVolume.collectAsStateWithLifecycle()
    val cues by deck.hotCues.collectAsStateWithLifecycle()
    val progress = if (dur > 0) pos.toFloat() / dur else 0f

    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title ?: "Platine ${deck.id} — vide",
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("${fmt(pos)} / ${fmt(dur)}", color = TextLo, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Platter(playing, progress, color, modifier = Modifier.size(110.dp))
            Spacer(Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Knob(high, deck::setEqHigh, color, "AIGU", size = 42)
                Knob(mid, deck::setEqMid, color, "MED", size = 42)
                Knob(low, deck::setEqLow, color, "GRAVE", size = 42)
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton("CUE", color, false, onDown = { deck.cuePressed() }, onUp = { deck.cueReleased() }, sizeDp = 56)
            RoundButton(if (playing) "❚❚" else "►", Lime, playing, onDown = { deck.togglePlay() }, sizeDp = 64)
        }

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            VerticalFader(tempo, deck::setTempo, color, "TEMPO", heightDp = 120)
            VerticalFader(vol, deck::setChannelVolume, color, "VOL", heightDp = 120)
        }

        Spacer(Modifier.height(8.dp))
        // 8 pads (hot cues), 2 rangées de 4.
        for (row in 0 until 2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (col in 0 until 4) {
                    val i = row * 4 + col
                    Pad(
                        index = i,
                        active = cues[i] >= 0,
                        color = color,
                        onDown = { deck.padPressed(i) },
                        onUp = { },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(5.dp))
        }

        Button(onClick = onLoad, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.MusicNote, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Charger")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MixerColumn(vm: DjViewModel, modifier: Modifier = Modifier) {
    val crossfader by vm.engine.crossfader.collectAsStateWithLifecycle()
    val master by vm.engine.master.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxHeight().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("MASTER", color = TextLo, fontSize = 11.sp)
        VerticalFader(master, vm.engine::setMaster, Lime, "", heightDp = 150)
        Spacer(Modifier.height(20.dp))
        Text("CROSSFADER", color = TextLo, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        HorizontalFader(crossfader, vm.engine::setCrossfader, DeckAColor, DeckBColor)
    }
}

private fun fmt(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
