package fr.tbrun.djkids.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tbrun.djkids.ui.DjViewModel
import fr.tbrun.djkids.ui.theme.TextLo

@Composable
fun MonitorScreen(vm: DjViewModel) {
    val log by vm.monitor.collectAsStateWithLifecycle()
    val status by vm.midi.status.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("MIDI Monitor", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Text(
            "Branche la MK3, presse chaque pad/jog/fader : le code s'affiche. Sert à vérifier le mapping.",
            color = TextLo, fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(status, color = TextLo)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.connect() }) { Text("Connecter") }
            OutlinedButton(onClick = { vm.clearMonitor() }) { Text("Vider") }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(log) { line ->
                Text(line, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            }
        }
    }
}
