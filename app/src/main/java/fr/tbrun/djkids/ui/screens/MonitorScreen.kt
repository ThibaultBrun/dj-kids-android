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
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tbrun.djkids.APP_VERSION
import fr.tbrun.djkids.ui.DjViewModel
import fr.tbrun.djkids.ui.theme.TextLo

@Composable
fun MonitorScreen(vm: DjViewModel) {
    val log by vm.monitor.collectAsStateWithLifecycle()
    val summary by vm.monitorSummary.collectAsStateWithLifecycle()
    val status by vm.midi.status.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(summary.size) {
        if (summary.isNotEmpty()) listState.animateScrollToItem(summary.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("MIDI Monitor", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Text(
            "Branche la MK3, bouge chaque contrôle UNE fois : il apparaît dans le résumé ci-dessous (1 ligne par contrôle, le jog ne noie plus le reste). Puis Exporter.",
            color = TextLo, fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(status, color = TextLo)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.connect() }) { Text("Connecter") }
            OutlinedButton(onClick = { vm.clearMonitor() }) { Text("Vider") }
            OutlinedButton(
                enabled = summary.isNotEmpty(),
                onClick = {
                    val report = buildString {
                        appendLine("DJ Kids — relevé MIDI MK3 (v$APP_VERSION)")
                        appendLine(status)
                        appendLine("— ${summary.size} contrôles distincts —")
                        summary.forEach {
                            appendLine("${it.type} ch${it.channel} num=${it.number}  val ${it.minVal}..${it.maxVal} (x${it.count})")
                        }
                    }
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Mapping MIDI Hercules Inpulse 200 MK3")
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    context.startActivity(Intent.createChooser(send, "Partager le relevé MIDI"))
                },
            ) { Text("Exporter") }
        }
        Spacer(Modifier.height(8.dp))
        Text("Résumé (${summary.size} contrôles) :", color = TextLo, fontSize = 12.sp)
        HorizontalDivider()
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(summary) { c ->
                Text(
                    "${c.type}  ch${c.channel}  num=${c.number}   ${c.minVal}..${c.maxVal}  (x${c.count})",
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                )
            }
        }
    }
}
