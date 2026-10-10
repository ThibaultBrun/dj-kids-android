package fr.tbrun.djkids.audio

import android.content.Context
import android.net.Uri
import fr.tbrun.djkids.model.DeckId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Une platine : lit un morceau décodé en mémoire via [ScratchDeckEngine], ce qui permet le vrai
 * scratch (lecture avant/arrière à vitesse variable, sans pause). Expose l'état pour l'UI.
 */
class Deck(
    private val context: Context,
    val id: DeckId,
    private val scope: CoroutineScope,
) {
    private val engine = ScratchDeckEngine()

    private val _title = MutableStateFlow<String?>(null)
    val title: StateFlow<String?> = _title
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded
    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs
    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs
    private val _tempo = MutableStateFlow(0.5f)
    val tempo: StateFlow<Float> = _tempo
    private val _hotCues = MutableStateFlow(LongArray(8) { -1L }) // en frames
    val hotCues: StateFlow<LongArray> = _hotCues
    val eqLow = MutableStateFlow(0.5f)
    val eqMid = MutableStateFlow(0.5f)
    val eqHigh = MutableStateFlow(0.5f)
    val channelVolume = MutableStateFlow(1f)

    private var cuePointFrame = 0.0
    private var pollJob: Job? = null

    init { engine.start(); startPolling() }

    private fun sr() = engine.sampleRate.coerceAtLeast(1)

    fun load(uri: Uri, title: String) {
        _title.value = title
        _loaded.value = false
        _playing.value = false
        scope.launch {
            val pcm = withContext(Dispatchers.IO) { AudioDecoder.decode(context, uri) }
            if (pcm == null) { _loaded.value = false; return@launch }
            // Les sons courts (samples/breakbeats) bouclent ; les morceaux longs non.
            engine.setTrack(pcm)
            engine.setLoop(pcm.frames < pcm.sampleRate * 20) // < 20 s => boucle
            cuePointFrame = 0.0
            _hotCues.value = LongArray(8) { -1L }
            _durationMs.value = pcm.frames.toLong() * 1000 / pcm.sampleRate
            _positionMs.value = 0L
            _loaded.value = true
        }
    }

    fun togglePlay() { engine.togglePlay(); _playing.value = engine.isPlaying() }
    fun play() { engine.play(); _playing.value = true }
    fun pause() { engine.pause(); _playing.value = false }

    fun cuePressed() {
        if (!_loaded.value) return
        if (engine.isPlaying()) {
            engine.pause(); engine.seekToFrame(cuePointFrame); _playing.value = false
        } else {
            cuePointFrame = engine.positionFrame(); engine.play(); _playing.value = true
        }
    }

    fun cueReleased() {
        if (!_loaded.value) return
        if (engine.isPlaying() && engine.positionFrame() >= cuePointFrame) {
            engine.pause(); engine.seekToFrame(cuePointFrame); _playing.value = false
            _positionMs.value = (cuePointFrame / sr() * 1000).toLong()
        }
    }

    fun padPressed(index: Int) {
        if (!_loaded.value || index !in 0..7) return
        val cues = _hotCues.value.copyOf()
        if (cues[index] < 0) {
            cues[index] = engine.positionFrame().toLong(); _hotCues.value = cues
        } else {
            engine.seekToFrame(cues[index].toDouble()); engine.play(); _playing.value = true
        }
    }

    fun clearPad(index: Int) {
        if (index !in 0..7) return
        val cues = _hotCues.value.copyOf(); cues[index] = -1L; _hotCues.value = cues
    }

    fun setTempo(v: Float) { _tempo.value = v.coerceIn(0f, 1f); engine.setTempo(_tempo.value) }

    /** Jog = vrai scratch : vitesse signée (arrière possible), sans pause. */
    fun jogNudge(delta: Float) {
        if (!_loaded.value) return
        engine.jog(delta.toDouble() * 2.2)
    }

    fun setEqLow(v: Float)  { eqLow.value = v;  engine.setEqLow(v) }
    fun setEqMid(v: Float)  { eqMid.value = v;  engine.setEqMid(v) }
    fun setEqHigh(v: Float) { eqHigh.value = v; engine.setEqHigh(v) }

    fun setChannelVolume(v: Float) { channelVolume.value = v.coerceIn(0f, 1f); engine.setChannelVolume(channelVolume.value) }
    fun setCrossfaderGain(g: Float) { engine.setCrossfaderGain(g) }
    fun setMasterGain(g: Float) { engine.setMasterGain(g) }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch(Dispatchers.Main) {
            while (true) {
                if (_loaded.value) {
                    _positionMs.value = (engine.positionFrame() / sr() * 1000).toLong()
                    _playing.value = engine.isPlaying()
                }
                delay(60)
            }
        }
    }

    fun release() { pollJob?.cancel(); engine.stop() }
}
