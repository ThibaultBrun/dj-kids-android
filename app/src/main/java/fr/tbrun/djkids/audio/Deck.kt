package fr.tbrun.djkids.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import fr.tbrun.djkids.model.DeckId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Fabrique de renderers injectant notre EQ 3 bandes dans la chaîne audio d'ExoPlayer. */
private class EqRenderersFactory(
    context: Context,
    private val eq: ThreeBandEqProcessor,
) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink {
        // Float désactivé : notre EQ travaille en PCM 16 bits.
        return DefaultAudioSink.Builder(context)
            .setAudioProcessors(arrayOf<AudioProcessor>(eq))
            .setEnableFloatOutput(false)
            .build()
    }
}

/**
 * Une platine : lecture d'un morceau du téléphone avec play/pause, cue, pitch/tempo (mode vinyle),
 * volume de voie, EQ 3 bandes et 8 hot-cues (pads).
 */
class Deck(
    context: Context,
    val id: DeckId,
    private val scope: CoroutineScope,
) {
    private val eq = ThreeBandEqProcessor()
    private val player: ExoPlayer =
        ExoPlayer.Builder(context, EqRenderersFactory(context, eq))
            .setHandleAudioBecomingNoisy(true)
            .build()

    // --- État observable (UI) ---
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
    private val _tempo = MutableStateFlow(0.5f) // 0.5 = vitesse normale
    val tempo: StateFlow<Float> = _tempo
    private val _hotCues = MutableStateFlow(LongArray(8) { -1L })
    val hotCues: StateFlow<LongArray> = _hotCues
    val eqLow = MutableStateFlow(0.5f)
    val eqMid = MutableStateFlow(0.5f)
    val eqHigh = MutableStateFlow(0.5f)
    val channelVolume = MutableStateFlow(1f)

    // Composantes de gain combinées.
    private var crossfaderGain = 1f
    private var masterGain = 1f
    private var cuePointMs = 0L
    private var pollJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playing.value = isPlaying
                if (isPlaying) startPolling() else {
                    pollJob?.cancel()
                    _positionMs.value = player.currentPosition.coerceAtLeast(0)
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _durationMs.value = player.duration.coerceAtLeast(0)
                }
            }
        })
    }

    fun load(uri: Uri, title: String) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        _title.value = title
        _loaded.value = true
        cuePointMs = 0L
        _hotCues.value = LongArray(8) { -1L }
        _positionMs.value = 0L
    }

    fun togglePlay() { if (player.isPlaying) player.pause() else player.play() }
    fun play() = player.play()
    fun pause() = player.pause()

    /** Cue style DJ : relâché à un point, maintien = preview depuis le cue. */
    fun cuePressed() {
        if (!_loaded.value) return
        if (player.isPlaying) {
            // En lecture : retour au point de cue + pause.
            player.pause()
            player.seekTo(cuePointMs)
        } else {
            // À l'arrêt : (re)pose le point de cue à la position courante, puis preview.
            cuePointMs = player.currentPosition.coerceAtLeast(0)
            player.play()
        }
    }

    fun cueReleased() {
        if (!_loaded.value) return
        // Si on previewait (maintien depuis l'arrêt), on revient au cue et on stoppe.
        if (player.isPlaying && player.currentPosition >= cuePointMs) {
            player.pause()
            player.seekTo(cuePointMs)
            _positionMs.value = cuePointMs
        }
    }

    /** Pad hot-cue : pose si vide, sinon saute et joue. */
    fun padPressed(index: Int) {
        if (!_loaded.value || index !in 0..7) return
        val cues = _hotCues.value.copyOf()
        if (cues[index] < 0) {
            cues[index] = player.currentPosition.coerceAtLeast(0)
            _hotCues.value = cues
        } else {
            player.seekTo(cues[index])
            player.play()
        }
    }

    fun clearPad(index: Int) {
        if (index !in 0..7) return
        val cues = _hotCues.value.copyOf()
        cues[index] = -1L
        _hotCues.value = cues
    }

    /** Tempo/pitch façon vinyle (la vitesse modifie aussi la hauteur). v: 0..1, 0.5 = normal. */
    fun setTempo(v: Float) {
        val value = v.coerceIn(0f, 1f)
        _tempo.value = value
        val rate = (0.5f + value).coerceIn(0.5f, 1.5f)
        player.playbackParameters = PlaybackParameters(rate, rate)
    }

    private var jogRestoreJob: Job? = null

    /**
     * Jog en mode DJ : en lecture, on « bend » la vitesse (effet scratch/nudge) sans mettre en pause
     * ni re-seek en boucle (ce qui coupait le son) ; à l'arrêt, le jog fait une recherche fine.
     * Le vrai scratch vinyle (lecture arrière, basse latence) viendra via le moteur Oboe.
     */
    fun jogNudge(delta: Float) {
        if (!_loaded.value) return
        if (player.isPlaying) {
            val bend = (1f + delta * 1.6f).coerceIn(0.3f, 2.5f)
            player.playbackParameters = PlaybackParameters(bend, bend)
            jogRestoreJob?.cancel()
            jogRestoreJob = scope.launch(Dispatchers.Main) {
                delay(90)
                val base = (0.5f + _tempo.value).coerceIn(0.5f, 1.5f)
                player.playbackParameters = PlaybackParameters(base, base)
            }
        } else {
            val target = (player.currentPosition + (delta * 120).toLong())
                .coerceIn(0L, player.duration.coerceAtLeast(0L))
            player.seekTo(target)
        }
    }

    fun setEqLow(v: Float)  { eqLow.value = v;  eq.setLow(v) }
    fun setEqMid(v: Float)  { eqMid.value = v;  eq.setMid(v) }
    fun setEqHigh(v: Float) { eqHigh.value = v; eq.setHigh(v) }

    fun setChannelVolume(v: Float) { channelVolume.value = v.coerceIn(0f, 1f); applyGain() }
    fun setCrossfaderGain(g: Float) { crossfaderGain = g.coerceIn(0f, 1f); applyGain() }
    fun setMasterGain(g: Float) { masterGain = g.coerceIn(0f, 1f); applyGain() }

    private fun applyGain() {
        player.volume = channelVolume.value * crossfaderGain * masterGain
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch(Dispatchers.Main) {
            while (true) {
                _positionMs.value = player.currentPosition.coerceAtLeast(0)
                val d = player.duration
                if (d > 0) _durationMs.value = d
                delay(60)
            }
        }
    }

    fun release() {
        pollJob?.cancel()
        player.release()
    }
}
