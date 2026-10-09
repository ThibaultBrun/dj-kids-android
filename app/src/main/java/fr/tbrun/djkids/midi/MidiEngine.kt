package fr.tbrun.djkids.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Connexion USB-MIDI à la Hercules DJControl Inpulse 200 MK3.
 *
 * - On ouvre l'**outputPort** du contrôleur (= sa sortie MIDI = notre entrée) pour recevoir
 *   pads/jogs/faders → [messages].
 * - On ouvre l'**inputPort** du contrôleur (= son entrée MIDI) pour renvoyer du feedback LED
 *   (pads allumés, VU-mètres) via [send].
 *
 * Aucune permission runtime n'est requise par `android.media.midi` ; le système gère l'accès USB.
 */
class MidiEngine(private val context: Context) {

    private val _status = MutableStateFlow("Non connecté")
    val status: StateFlow<String> = _status

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName

    /** Flux de tous les messages décodés (monitor, MIDI learn, moteur DJ). */
    val messages = MutableSharedFlow<MidiMessage>(extraBufferCapacity = 512)

    private val manager: MidiManager? =
        context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
    private val handler = Handler(Looper.getMainLooper())

    private var device: MidiDevice? = null
    private var inputPort: MidiInputPort? = null

    /** Tente la connexion au premier contrôleur Hercules (ou tout périphérique MIDI). */
    fun connect() {
        val mgr = manager ?: run {
            _status.value = "MIDI non supporté sur cet appareil"; return
        }
        @Suppress("DEPRECATION")
        val devices = mgr.devices
        if (devices.isEmpty()) {
            _status.value = "Aucun contrôleur détecté (branche la MK3 en USB-OTG)"
            _connected.value = false
            return
        }
        val target = devices.firstOrNull { info ->
            val name = (info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "").lowercase()
            ("hercules" in name || "inpulse" in name || "djcontrol" in name) && info.outputPortCount > 0
        } ?: devices.firstOrNull { it.outputPortCount > 0 }

        if (target == null) {
            _status.value = "Contrôleur sans sortie MIDI"; return
        }
        val name = target.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "contrôleur"

        mgr.openDevice(target, { dev ->
            if (dev == null) { _status.value = "Échec ouverture du contrôleur"; return@openDevice }
            device = dev
            val out = dev.openOutputPort(0)
            if (out == null) { _status.value = "Impossible d'ouvrir le port MIDI"; return@openDevice }
            out.connect(Receiver())
            // Port d'entrée (feedback LED) si disponible.
            if (target.inputPortCount > 0) {
                inputPort = dev.openInputPort(0)
            }
            _deviceName.value = name
            _connected.value = true
            _status.value = "Connecté : $name"
        }, handler)
    }

    fun disconnect() {
        runCatching { inputPort?.close() }
        runCatching { device?.close() }
        inputPort = null
        device = null
        _connected.value = false
        _status.value = "Déconnecté"
    }

    /** Envoie des octets MIDI bruts au contrôleur (feedback LED). */
    fun send(bytes: ByteArray) {
        val port = inputPort ?: return
        runCatching { port.send(bytes, 0, bytes.size) }
    }

    /** Allume/éteint une LED (note on/off) sur un canal donné. */
    fun setLed(channel: Int, note: Int, on: Boolean, velocity: Int = 127) {
        val status = (if (on) 0x90 else 0x80) or ((channel - 1) and 0x0F)
        send(byteArrayOf(status.toByte(), (note and 0x7F).toByte(), (velocity and 0x7F).toByte()))
    }

    private inner class Receiver : MidiReceiver() {
        override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
            var i = offset
            val end = offset + count
            while (i < end) {
                val status = msg[i].toInt() and 0xFF
                if (status < 0x80) { i++; continue } // octet de données isolé
                val type = MidiType.from(status)
                val ch = (status and 0x0F) + 1
                val d1 = if (i + 1 < end) msg[i + 1].toInt() and 0x7F else 0
                val d2 = if (i + 2 < end) msg[i + 2].toInt() and 0x7F else 0
                messages.tryEmit(MidiMessage(type, ch, d1, d2, status))
                i += if (type == MidiType.PITCH_BEND || type == MidiType.NOTE_ON ||
                         type == MidiType.NOTE_OFF || type == MidiType.CC) 3 else 1
            }
        }
    }
}
