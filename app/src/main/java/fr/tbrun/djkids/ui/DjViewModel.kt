package fr.tbrun.djkids.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.tbrun.djkids.audio.DjEngine
import fr.tbrun.djkids.midi.Binding
import fr.tbrun.djkids.midi.ControlFunction
import fr.tbrun.djkids.midi.MidiEngine
import fr.tbrun.djkids.midi.MidiMapping
import fr.tbrun.djkids.midi.MidiMessage
import fr.tbrun.djkids.model.DeckId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Cible de MIDI learn : la fonction qu'on veut associer au prochain contrôle pressé. */
data class LearnTarget(val fn: ControlFunction, val deck: DeckId?, val pad: Int = -1)

class DjViewModel(app: Application) : AndroidViewModel(app) {

    val midi = MidiEngine(app)
    val mapping = MidiMapping(app)
    val engine = DjEngine(app, viewModelScope)

    private val _monitor = MutableStateFlow<List<String>>(emptyList())
    val monitor: StateFlow<List<String>> = _monitor

    private val _learn = MutableStateFlow<LearnTarget?>(null)
    val learn: StateFlow<LearnTarget?> = _learn

    /** Incrémenté à chaque modif du mapping pour rafraîchir l'écran Mapping. */
    private val _mappingVersion = MutableStateFlow(0)
    val mappingVersion: StateFlow<Int> = _mappingVersion

    init {
        viewModelScope.launch {
            midi.messages.collect { msg -> onMidi(msg) }
        }
    }

    private fun onMidi(msg: MidiMessage) {
        _monitor.value = (_monitor.value + msg.pretty()).takeLast(300)

        val target = _learn.value
        if (target != null) {
            // On ignore les relâchements (note off / vel 0) pour ne pas capturer le mauvais event.
            val isRelease = msg.pretty().startsWith("NOTE OFF")
            if (!isRelease) {
                mapping.learn(msg.controlKey(), Binding(target.fn, target.deck, target.pad))
                _learn.value = null
                _mappingVersion.value++
            }
            return
        }
        engine.apply(mapping.resolve(msg))
    }

    // --- MIDI ---
    fun connect() = midi.connect()
    fun disconnect() = midi.disconnect()
    fun clearMonitor() { _monitor.value = emptyList() }

    // --- Learn ---
    fun startLearn(fn: ControlFunction, deck: DeckId?, pad: Int = -1) {
        _learn.value = LearnTarget(fn, deck, pad)
    }
    fun cancelLearn() { _learn.value = null }
    fun clearBinding(fn: ControlFunction, deck: DeckId?, pad: Int = -1) {
        mapping.bindingFor(fn, deck, pad)?.let { mapping.clear(it) }
        _mappingVersion.value++
    }
    fun applyPreset() { mapping.applyInpulse200Preset(); _mappingVersion.value++ }
    fun clearAllBindings() { mapping.clearAll(); _mappingVersion.value++ }

    // --- Chargement de morceaux ---
    fun loadTrack(deck: DeckId, uri: Uri, title: String) = engine.loadTrack(deck, uri, title)

    override fun onCleared() {
        engine.release()
        midi.disconnect()
        super.onCleared()
    }
}
