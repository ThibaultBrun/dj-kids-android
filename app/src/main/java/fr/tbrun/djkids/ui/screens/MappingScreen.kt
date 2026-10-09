package fr.tbrun.djkids.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tbrun.djkids.midi.ControlFunction
import fr.tbrun.djkids.model.DeckId
import fr.tbrun.djkids.ui.DjViewModel
import fr.tbrun.djkids.ui.theme.Amber
import fr.tbrun.djkids.ui.theme.DeckAColor
import fr.tbrun.djkids.ui.theme.DeckBColor
import fr.tbrun.djkids.ui.theme.Surface2
import fr.tbrun.djkids.ui.theme.TextLo

private data class Rowdef(val fn: ControlFunction, val deck: DeckId?, val pad: Int, val label: String)

private fun deckRows(deck: DeckId): List<Rowdef> {
    val base = listOf(
        ControlFunction.PLAY, ControlFunction.CUE, ControlFunction.SYNC, ControlFunction.LOAD,
        ControlFunction.VOLUME, ControlFunction.TEMPO,
        ControlFunction.EQ_LOW, ControlFunction.EQ_MID, ControlFunction.EQ_HIGH,
        ControlFunction.FILTER, ControlFunction.JOG,
    ).map { Rowdef(it, deck, -1, it.label) }
    val pads = (0 until 8).map { Rowdef(ControlFunction.PAD, deck, it, "Pad ${it + 1}") }
    return base + pads
}

@Composable
fun MappingScreen(vm: DjViewModel) {
    // Déclenche la recomposition quand le mapping change ou qu'on apprend.
    val version by vm.mappingVersion.collectAsStateWithLifecycle()
    val learn by vm.learn.collectAsStateWithLifecycle()
    val connected by vm.midi.connected.collectAsStateWithLifecycle()
    val status by vm.midi.status.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Mapping du contrôleur", style = MaterialTheme.typography.titleLarge)
            Text(
                "Appuie sur « Apprendre » d'une fonction puis bouge le contrôle correspondant sur la MK3. " +
                    "La MK3 étant récente, commence par le preset puis corrige ce qui ne répond pas.",
                color = TextLo, fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(status, color = if (connected) DeckAColor else TextLo, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.connect() }) { Text("Connecter") }
                OutlinedButton(onClick = { vm.applyPreset() }) { Text("Preset Inpulse 200") }
                TextButton(onClick = { vm.clearAllBindings() }) { Text("Tout effacer") }
            }
            if (learn != null) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(Amber.copy(alpha = 0.2f)).padding(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎛️ Presse un contrôle sur la MK3…", color = Amber, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.cancelLearn() }) { Text("Annuler") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Mixer global
        item { SectionHeader("Mixer") }
        items(listOf(
            Rowdef(ControlFunction.CROSSFADER, null, -1, ControlFunction.CROSSFADER.label),
            Rowdef(ControlFunction.MASTER, null, -1, ControlFunction.MASTER.label),
        )) { r -> MappingRow(vm, r, version, learn, Amber) }

        for (deck in DeckId.entries) {
            item { SectionHeader("Platine $deck") }
            items(deckRows(deck)) { r ->
                MappingRow(vm, r, version, learn, if (deck == DeckId.A) DeckAColor else DeckBColor)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Spacer(Modifier.height(10.dp))
    Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextLo)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun MappingRow(
    vm: DjViewModel,
    r: Rowdef,
    @Suppress("UNUSED_PARAMETER") version: Int,
    learn: fr.tbrun.djkids.ui.LearnTarget?,
    color: androidx.compose.ui.graphics.Color,
) {
    val key = vm.mapping.bindingFor(r.fn, r.deck, r.pad)
    val isLearning = learn != null && learn.fn == r.fn && learn.deck == r.deck && learn.pad == r.pad
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp)).background(Surface2).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(r.label, color = color, fontSize = 13.sp)
            Text(
                key ?: "non assigné",
                color = TextLo, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            )
        }
        if (key != null) {
            TextButton(onClick = { vm.clearBinding(r.fn, r.deck, r.pad) }) { Text("✕") }
        }
        OutlinedButton(onClick = {
            if (isLearning) vm.cancelLearn() else vm.startLearn(r.fn, r.deck, r.pad)
        }) {
            Text(if (isLearning) "…" else "Apprendre")
        }
    }
}
