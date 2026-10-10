package fr.tbrun.djkids.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.OutlinedButton
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

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
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

    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)) {
        DeckPanel(vm.engine.deckA, DeckId.A, DeckAColor, { openPicker(DeckId.A) }, Modifier.weight(1f))
        MixerColumn(vm, Modifier.width(88.dp))
        DeckPanel(vm.engine.deckB, DeckId.B, DeckBColor, { openPicker(DeckId.B) }, Modifier.weight(1f))
    }
}

/** Une platine façon appli DJ moderne : jog central dimensionné par la hauteur, faders sur le côté
 *  extérieur, EQ près du centre, transport (cue/play) en bas. Tient sans scroll en paysage. */
@Composable
private fun DeckPanel(deck: Deck, deckId: DeckId, color: Color, onLoad: () -> Unit, modifier: Modifier = Modifier) {
    val playing by deck.playing.collectAsStateWithLifecycle()
    val title by deck.title.collectAsStateWithLifecycle()
    val pos by deck.positionMs.collectAsStateWithLifecycle()
    val dur by deck.durationMs.collectAsStateWithLifecycle()
    val tempo by deck.tempo.collectAsStateWithLifecycle()
    val low by deck.eqLow.collectAsStateWithLifecycle()
    val mid by deck.eqMid.collectAsStateWithLifecycle()
    val high by deck.eqHigh.collectAsStateWithLifecycle()
    val vol by deck.channelVolume.collectAsStateWithLifecycle()
    val progress = if (dur > 0) pos.toFloat() / dur else 0f
    val isA = deckId == DeckId.A

    Column(modifier = modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Bandeau titre + temps (une ligne, tronqué).
        Text(
            text = title ?: "PLATINE $deckId",
            color = color, fontWeight = FontWeight.Bold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(),
        )
        Text("${fmt(pos)} / ${fmt(dur)}", color = TextLo, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(4.dp))

        // Zone centrale : jog + faders + EQ, prend toute la hauteur restante.
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val eq: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Knob(high, deck::setEqHigh, color, "AIGU", size = 34)
                    Knob(mid, deck::setEqMid, color, "MED", size = 34)
                    Knob(low, deck::setEqLow, color, "GRAVE", size = 34)
                }
            }
            val volF: @Composable () -> Unit = { VerticalFader(vol, deck::setChannelVolume, color, "VOL", heightDp = 132) }
            val tempoF: @Composable () -> Unit = { VerticalFader(tempo, deck::setTempo, color, "PITCH", heightDp = 132) }
            val platter: @Composable () -> Unit = {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Platter(playing, progress, color, modifier = Modifier.fillMaxHeight().aspectRatio(1f))
                }
            }
            if (isA) { volF(); platter(); tempoF(); eq() } else { eq(); tempoF(); platter(); volF() }
        }

        Spacer(Modifier.height(4.dp))
        // Transport en bas.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton("CUE", color, false, onDown = { deck.cuePressed() }, onUp = { deck.cueReleased() }, sizeDp = 46)
            RoundButton(if (playing) "❚❚" else "►", Lime, playing, onDown = { deck.togglePlay() }, sizeDp = 56)
            OutlinedButton(onClick = onLoad, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)) {
                Icon(Icons.Filled.MusicNote, contentDescription = "Charger")
            }
        }
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
