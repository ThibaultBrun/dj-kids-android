package fr.tbrun.djkids

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.tbrun.djkids.ui.DjViewModel
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

const val APP_VERSION = "0.4.0"

private enum class Tab(val label: String) { DJ("Mix"), LEARN("Scratch"), MAPPING("Mapping"), MONITOR("Monitor") }

@Composable
private fun AppRoot() {
    val vm: DjViewModel = viewModel()
    var tab by rememberSaveable { mutableStateOf(0) }

    // Tentative de connexion auto au lancement (si la MK3 est déjà branchée).
    LaunchedEffect(Unit) { vm.connect() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {
                            Icon(
                                when (t) {
                                    Tab.DJ -> Icons.Filled.Album
                                    Tab.LEARN -> Icons.Filled.School
                                    Tab.MAPPING -> Icons.Filled.Settings
                                    Tab.MONITOR -> Icons.Filled.Timeline
                                },
                                contentDescription = t.label,
                            )
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        }
    ) { inner ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(inner)) {
          androidx.compose.material3.Text(
              "v$APP_VERSION",
              color = androidx.compose.ui.graphics.Color(0x66FFFFFF),
              fontSize = 9.sp,
              modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(4.dp),
          )
          Surface(modifier = Modifier.fillMaxSize()) {
            when (Tab.entries[tab]) {
                Tab.DJ -> DjScreen(vm)
                Tab.LEARN -> TrainerScreen(vm)
                Tab.MAPPING -> MappingScreen(vm)
                Tab.MONITOR -> MonitorScreen(vm)
            }
          }
        }
    }
}
