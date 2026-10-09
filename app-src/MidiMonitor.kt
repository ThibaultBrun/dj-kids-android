package fr.tbrun.djkids

import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Phase 1 : se connecte à la Hercules Inpulse 200 MK3 (ou tout contrôleur USB-MIDI) et
 * décode en clair chaque message MIDI reçu. Sert à CAPTURER le mapping du contrôleur :
 * on presse chaque pad/jog/fader et on lit le (type, canal, numéro, valeur).
 *
 * La "sortie MIDI" du contrôleur = l'ENTRÉE de l'appli -> on ouvre son outputPort.
 */
class MidiMonitor(private val context: Context) {

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log

    private val _status = MutableStateFlow("Non connecté")
    val status: StateFlow<String> = _status

    private val manager: MidiManager? =
        context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
    private val handler = Handler(Looper.getMainLooper())

    fun connect() {
        val mgr = manager ?: run { _status.value = "MIDI non supporté sur cet appareil"; return }
        val devices = mgr.devices
        if (devices.isEmpty()) {
            _status.value = "Aucun contrôleur MIDI détecté (branche la MK3 en USB-OTG)"
            return
        }
        // On privilégie un appareil dont le nom évoque la Hercules, sinon le 1er avec une sortie.
        val target = devices.firstOrNull { info ->
            val name = (info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "").lowercase()
            ("hercules" in name || "inpulse" in name || "djcontrol" in name) && info.outputPortCount > 0
        } ?: devices.firstOrNull { it.outputPortCount > 0 }

        if (target == null) { _status.value = "Contrôleur sans sortie MIDI trouvée"; return }
        val name = target.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "contrôleur"

        mgr.openDevice(target, { device ->
            if (device == null) { _status.value = "Échec ouverture du contrôleur"; return@openDevice }
            val port = device.openOutputPort(0)
            if (port == null) { _status.value = "Impossible d'ouvrir le port MIDI"; return@openDevice }
            port.connect(Receiver())
            _status.value = "Connecté : $name"
            push("— Connecté à $name. Presse pads / jogs / faders —")
        }, handler)
    }

    private inner class Receiver : MidiReceiver() {
        override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
            var i = offset
            val end = offset + count
            while (i < end) {
                val status = msg[i].toInt() and 0xFF
                if (status < 0x80) { i++; continue }          // octet de données isolé -> on saute
                val type = status and 0xF0
                val ch = (status and 0x0F) + 1
                val d1 = if (i + 1 < end) msg[i + 1].toInt() and 0x7F else 0
                val d2 = if (i + 2 < end) msg[i + 2].toInt() and 0x7F else 0
                val line = when (type) {
                    0x90 -> if (d2 > 0) "NOTE ON  ch$ch  note=$d1  vel=$d2" else "NOTE OFF ch$ch  note=$d1"
                    0x80 -> "NOTE OFF ch$ch  note=$d1"
                    0xB0 -> "CC       ch$ch  num=$d1  val=$d2"
                    0xE0 -> "PITCH    ch$ch  val=${(d2 shl 7) or d1}"
                    else -> "0x%02X     ch$ch  d1=$d1  d2=$d2".format(type)
                }
                push(line)
                i += 3
            }
        }
    }

    private fun push(line: String) {
        handler.post {
            _log.value = (_log.value + line).takeLast(200)
        }
    }
}
