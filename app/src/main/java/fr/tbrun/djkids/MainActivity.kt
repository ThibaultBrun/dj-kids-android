package fr.tbrun.djkids

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.tbrun.djkids.ui.DjViewModel
import fr.tbrun.djkids.ui.theme.Cyan
import fr.tbrun.djkids.ui.theme.Surface1
import fr.tbrun.djkids.ui.theme.TextLo
import fr.tbrun.djkids.ui.screens.DjScreen
import fr.tbrun.djkids.ui.screens.MappingScreen
import fr.tbrun.djkids.ui.screens.MonitorScreen
import fr.tbrun.djkids.ui.screens.TrainerScreen
import fr.tbrun.djkids.ui.theme.DJKidsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // On garde l'écran allumé pendant le mix.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            DJKidsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

const val APP_VERSION = "1.7.0"

private enum class Tab(val label: String) { DJ("Mix"), LEARN("Scratch"), MAPPING("Mapping"), MONITOR("Monitor") }

@Composable
private fun AppRoot() {
    val vm: DjViewModel = viewModel()
    var tab by rememberSaveable { mutableStateOf(0) }

    // Tentative de connexion auto au lancement (si la MK3 est déjà branchée).
    LaunchedEffect(Unit) { vm.connect() }

    Scaffold(
        topBar = { SlimTabs(selected = tab, onSelect = { tab = it }) }
    ) { inner ->
        Surface(modifier = Modifier.fillMaxSize().padding(inner)) {
            when (Tab.entries[tab]) {
                Tab.DJ -> DjScreen(vm)
                Tab.LEARN -> TrainerScreen(vm)
                Tab.MAPPING -> MappingScreen(vm)
                Tab.MONITOR -> MonitorScreen(vm)
            }
        }
    }
}

/** Barre de navigation fine en haut (remplace la grosse NavigationBar du bas). */
@Composable
private fun SlimTabs(selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = Surface1) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .statusBarsPadding()
                .height(44.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEachIndexed { i, t ->
                val sel = selected == i
                val tint = if (sel) Cyan else TextLo
                Row(
                    modifier = Modifier
                        .clickable { onSelect(i) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        when (t) {
                            Tab.DJ -> Icons.Filled.Album
                            Tab.LEARN -> Icons.Filled.School
                            Tab.MAPPING -> Icons.Filled.Settings
                            Tab.MONITOR -> Icons.Filled.Timeline
                        },
                        contentDescription = t.label,
                        tint = tint,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(t.label, color = tint, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("v$APP_VERSION", color = Color(0x66FFFFFF), fontSize = 9.sp)
        }
    }
}
