package fr.tbrun.djkids.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * Moteur d'UNE platine : lit un morceau décodé en mémoire ([PcmSample]) via une tête de lecture
 * à vitesse variable. C'est ce qui permet le VRAI scratch (lecture avant/arrière, sans pause ni
 * re-seek) comme sur un vinyle. Sortie AudioTrack float en basse latence.
 *
 * EQ 3 bandes + gains (voie/crossfader/master) appliqués dans la boucle de rendu.
 */
class ScratchDeckEngine(private val outRate: Int = 48000) {

    @Volatile private var track: PcmSample? = null
    @Volatile private var playing = false
    @Volatile private var playhead = 0.0          // position en frames (source)
    @Volatile private var baseTempo = 0.5f         // 0.5 = vitesse normale
    @Volatile private var loop = false

    // Scratch (jog) : vitesse transitoire qui prime sur la lecture normale.
    @Volatile private var jogSpeed = 0.0
    @Volatile private var jogUntilMs = 0L

    // Gains.
    @Volatile private var channelVol = 1f
    @Volatile private var crossGain = 1f
    @Volatile private var masterGain = 1f

    // EQ (0..1, 0.5 = neutre).
    private val low = BiquadCoeffs(); private val mid = BiquadCoeffs(); private val high = BiquadCoeffs()
    private val sL = BiquadState(); private val sM = BiquadState(); private val sH = BiquadState()
    @Volatile private var eqLow = 0.5f; @Volatile private var eqMid = 0.5f; @Volatile private var eqHigh = 0.5f
    @Volatile private var eqDirty = true

    @Volatile private var running = false
    private var at: AudioTrack? = null
    private var renderThread: Thread? = null

    val sampleRate: Int get() = track?.sampleRate ?: outRate
    val frames: Int get() = track?.frames ?: 0
    fun positionFrame(): Double = playhead
    fun isPlaying(): Boolean = playing

    fun setTrack(s: PcmSample?) { track = s; playhead = 0.0; onEqChanged() }
    fun setLoop(v: Boolean) { loop = v }
    fun play() { playing = true }
    fun pause() { playing = false }
    fun togglePlay() { playing = !playing }
    fun seekToFrame(f: Double) { playhead = f.coerceIn(0.0, (frames - 1).coerceAtLeast(0).toDouble()) }
    fun setTempo(v: Float) { baseTempo = v.coerceIn(0f, 1f) }

    /** Impulsion de jog : vitesse de scratch (src frames / frame de sortie), négative = arrière. */
    fun jog(speed: Double, holdMs: Long = 110) {
        jogSpeed = speed
        jogUntilMs = System.currentTimeMillis() + holdMs
    }

    fun setChannelVolume(v: Float) { channelVol = v.coerceIn(0f, 1f) }
    fun setCrossfaderGain(g: Float) { crossGain = g.coerceIn(0f, 1f) }
    fun setMasterGain(g: Float) { masterGain = g.coerceIn(0f, 1f) }
    fun setEqLow(v: Float) { eqLow = v; eqDirty = true }
    fun setEqMid(v: Float) { eqMid = v; eqDirty = true }
    fun setEqHigh(v: Float) { eqHigh = v; eqDirty = true }
    private fun onEqChanged() { eqDirty = true }

    fun start() {
        if (running) return
        val minBuf = AudioTrack.getMinBufferSize(outRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT).coerceAtLeast(2048)
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(outRate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setBufferSizeInBytes(minBuf * 2)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        at = t; running = true; t.play()
        val N = 192
        val buf = FloatArray(N * 2)
        renderThread = thread(name = "deck-render", priority = Thread.MAX_PRIORITY) {
            while (running) {
                render(buf, N)
                if (t.write(buf, 0, buf.size, AudioTrack.WRITE_BLOCKING) < 0) break
            }
        }
    }

    private fun recomputeEq() {
        val sr = sampleRate
        low.lowShelf(sr, 200.0, toDb(eqLow))
        mid.peaking(sr, 1000.0, toDb(eqMid), q = 0.9)
        high.highShelf(sr, 3200.0, toDb(eqHigh))
        eqDirty = false
    }
    private fun toDb(v: Float): Double = when {
        v <= 0.03f -> -60.0
        v >= 0.5f -> (v - 0.5f) / 0.5f * 6.0
        else -> (v - 0.5f) / 0.5f * 40.0
    }

    private fun render(out: FloatArray, n: Int) {
        val s = track
        if (eqDirty) recomputeEq()
        val ratio = (s?.sampleRate ?: outRate).toDouble() / outRate
        val now = System.currentTimeMillis()
        val jogActive = now < jogUntilMs
        val data = s?.data
        val len = data?.size ?: 0
        for (i in 0 until n) {
            var v = 0f
            if (data != null && len > 1) {
                val speed = when {
                    jogActive -> jogSpeed.coerceIn(-10.0, 10.0)
                    playing -> (0.5 + baseTempo).coerceIn(0.5, 1.5)
                    else -> 0.0
                } * ratio
                if (speed != 0.0) {
                    val x = sampleAt(data, playhead)
                    var y = sH.process(sM.process(sL.process(x, low), mid), high)
                    v = (y * channelVol * crossGain * masterGain)
                    playhead += speed
                    if (playhead >= len - 1) { if (loop) playhead -= (len - 1) else { playhead = (len - 1).toDouble(); if (!jogActive) playing = false } }
                    if (playhead < 0) { if (loop) playhead += (len - 1) else playhead = 0.0 }
                }
            }
            v = v.coerceIn(-1f, 1f)
            out[i * 2] = v; out[i * 2 + 1] = v
        }
    }

    private fun sampleAt(data: FloatArray, pos: Double): Float {
        val i = pos.toInt()
        if (i < 0) return data[0]
        if (i >= data.size - 1) return data[data.size - 1]
        val frac = (pos - i).toFloat()
        return data[i] * (1 - frac) + data[i + 1] * frac
    }

    fun stop() {
        running = false
        renderThread?.join(200); renderThread = null
        at?.run { runCatching { pause(); flush(); stop(); release() } }
        at = null
    }
}
