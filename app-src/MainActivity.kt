package fr.tbrun.djkids

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Phase 1 : écran "MIDI monitor". Tape « Connecter la MK3 » puis presse chaque contrôle
 * du Hercules : chaque message s'affiche en clair (type, canal, numéro, valeur).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val monitor = MidiMonitor(applicationContext)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MonitorScreen(monitor)
                }
            }
        }
    }
}

@Composable
fun MonitorScreen(monitor: MidiMonitor) {
    val log by monitor.log.collectAsStateWithLifecycle()
    val status by monitor.status.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("🎧 DJ Kids — MIDI Monitor", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { monitor.connect() }) { Text("Connecter la MK3") }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(log) { line ->
                Text(line, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            }
        }
    }
}
