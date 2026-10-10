package fr.tbrun.djkids.audio

import android.content.Context
import android.net.Uri
import fr.tbrun.djkids.midi.ControlFunction
import fr.tbrun.djkids.midi.MidiAction
import fr.tbrun.djkids.model.DeckId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Le "mixeur" : deux platines + crossfader + master. Applique les actions MIDI résolues
 * ou les gestes tactiles de l'UI.
 */
class DjEngine(context: Context, scope: CoroutineScope) {

    val deckA = Deck(context, DeckId.A, scope)
    val deckB = Deck(context, DeckId.B, scope)

    private val _crossfader = MutableStateFlow(0.5f) // 0 = A seul, 1 = B seul
    val crossfader: StateFlow<Float> = _crossfader
    private val _master = MutableStateFlow(1f)
    val master: StateFlow<Float> = _master

    /** L'UI écoute ça pour ouvrir le sélecteur de morceau quand on presse LOAD. */
    val loadRequests = MutableSharedFlow<DeckId>(extraBufferCapacity = 4)

    init {
        setCrossfader(0.5f)
        setMaster(1f)
    }

    fun deck(id: DeckId) = if (id == DeckId.A) deckA else deckB

    fun loadTrack(id: DeckId, uri: Uri, title: String) = deck(id).load(uri, title)

    fun setCrossfader(v: Float) {
        val x = v.coerceIn(0f, 1f)
        _crossfader.value = x
        // Courbe équal-power : pas de creux de volume au centre.
        val gainA = cos(x * PI / 2).toFloat()
        val gainB = sin(x * PI / 2).toFloat()
        deckA.setCrossfaderGain(gainA)
        deckB.setCrossfaderGain(gainB)
    }

    fun setMaster(v: Float) {
        val m = v.coerceIn(0f, 1f)
        _master.value = m
        deckA.setMasterGain(m)
        deckB.setMasterGain(m)
    }

    private fun sync(target: DeckId) {
        val other = deck(if (target == DeckId.A) DeckId.B else DeckId.A)
        deck(target).setTempo(other.tempo.value)
    }

    /** Point d'entrée des actions issues du mapping MIDI. */
    fun apply(action: MidiAction) {
        when (action) {
            is MidiAction.Button -> {
                val d = action.deck
                when (action.fn) {
                    ControlFunction.PLAY -> if (action.pressed && d != null) deck(d).togglePlay()
                    ControlFunction.CUE -> if (d != null) {
                        if (action.pressed) deck(d).cuePressed() else deck(d).cueReleased()
                    }
                    ControlFunction.SYNC -> if (action.pressed && d != null) sync(d)
                    ControlFunction.LOAD -> if (action.pressed && d != null) loadRequests.tryEmit(d)
                    ControlFunction.PAD -> if (d != null) {
                        if (action.pressed) deck(d).padPressed(action.pad)
                    }
                    ControlFunction.JOG_TOUCH -> if (d != null) deck(d).jogTouch(action.pressed)
                    else -> {}
                }
            }
            is MidiAction.Continuous -> {
                val d = action.deck
                when (action.fn) {
                    ControlFunction.VOLUME -> if (d != null) deck(d).setChannelVolume(action.value)
                    ControlFunction.TEMPO -> if (d != null) deck(d).setTempo(action.value)
                    ControlFunction.EQ_LOW -> if (d != null) deck(d).setEqLow(action.value)
                    ControlFunction.EQ_MID -> if (d != null) deck(d).setEqMid(action.value)
                    ControlFunction.EQ_HIGH -> if (d != null) deck(d).setEqHigh(action.value)
                    ControlFunction.FILTER -> { /* phase 4 : filtre balayage DSP */ }
                    ControlFunction.CROSSFADER -> setCrossfader(action.value)
                    ControlFunction.MASTER -> setMaster(action.value)
                    else -> {}
                }
            }
            is MidiAction.Jog -> action.deck?.let { deck(it).jogNudge(action.delta) }
            MidiAction.Unmapped -> {}
        }
    }

    fun release() {
        deckA.release()
        deckB.release()
    }
}
