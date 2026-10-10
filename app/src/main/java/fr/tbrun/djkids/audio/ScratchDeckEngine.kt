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

    // Scratch (jog) : modèle vinyle à VÉLOCITÉ CONTINUE (évite le son haché entre deux ticks).
    // Le jog MK3 envoie ±1 par message ; la vitesse vient de la cadence des ticks.
    @Volatile private var scratchVel = 0.0      // frames source / frame de sortie
    @Volatile private var touchHeld = false     // doigt sur le jog (contact), prime sur le timeout
    @Volatile private var scratchUntilMs = 0L   // filet de sécurité si le contact n'est pas câblé
    @Volatile private var lastTickNanos = 0L
    private val ticksPerRev = 248.0
    private val vinylRpm = 33.0 + 1.0 / 3.0
    /** frames audio par tick de jog → à la vitesse naturelle du vinyle, lecture = 1×. */
    private fun framesPerTick() = sampleRate / (ticksPerRev * vinylRpm / 60.0)

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

    fun setTrack(s: PcmSample?) { track = s; playhead = 0.0; scratchVel = 0.0; onEqChanged() }
    fun setLoop(v: Boolean) { loop = v }
    fun play() { playing = true }
    fun pause() { playing = false }
    fun togglePlay() { playing = !playing }
    fun seekToFrame(f: Double) { playhead = f.coerceIn(0.0, (frames - 1).coerceAtLeast(0).toDouble()) }

    /** Contact du jog (doigt posé). Pendant le contact = scratch ; au relâcher = reprise immédiate. */
    fun setScratching(on: Boolean) {
        touchHeld = on
        if (on) { scratchVel = 0.0; lastTickNanos = System.nanoTime() }
    }
    fun setTempo(v: Float) { baseTempo = v.coerceIn(0f, 1f) }

    /** Un tick de jog (±1). Vitesse = framesParTick / temps écoulé depuis le tick précédent. */
    fun jogTick(dir: Int, holdMs: Long = 140) {
        val nowN = System.nanoTime()
        val dt = (nowN - lastTickNanos) / 1e9
        lastTickNanos = nowN
        val d = if (dir >= 0) 1 else -1
        val fpt = framesPerTick()
        // frames source par frame de sortie ; dt borné pour éviter les sauts.
        val inst = d * fpt / (dt.coerceIn(0.002, 0.3) * outRate)
        scratchVel = scratchVel * 0.4 + inst * 0.6
        scratchUntilMs = System.currentTimeMillis() + holdMs
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
        val scratching = touchHeld || now < scratchUntilMs
        val data = s?.data
        val len = data?.size ?: 0
        val maxV = ratio * 24.0
        // Si plus de tick récent, on freine la "platine" (comme un vinyle qui ralentit).
        if (scratching && (System.nanoTime() - lastTickNanos) > 28_000_000L) scratchVel *= 0.55
        var vel = scratchVel.coerceIn(-maxV, maxV)
        for (i in 0 until n) {
            var v = 0f
            if (data != null && len > 1) {
                val speed = if (scratching) vel else if (playing) (0.5 + baseTempo).coerceIn(0.5, 1.5) * ratio else 0.0
                // Toujours échantillonner à la position courante (pas de silence entre ticks → plus de grésillement).
                val x = sampleAt(data, playhead)
                val y = sH.process(sM.process(sL.process(x, low), mid), high)
                v = y * channelVol * crossGain * masterGain
                playhead += speed
                if (playhead >= len - 1) { if (loop) playhead -= (len - 1) else { playhead = (len - 1).toDouble(); if (!scratching) playing = false } }
                if (playhead < 0) { if (loop) playhead += (len - 1) else playhead = 0.0 }
            }
            v = v.coerceIn(-1f, 1f)
            out[i * 2] = v; out[i * 2 + 1] = v
        }
        scratchVel = vel
    }

    /** Interpolation cubique (Catmull-Rom, 4 points) : bien moins d'aliasing que le linéaire au scratch. */
    private fun sampleAt(data: FloatArray, pos: Double): Float {
        val n = data.size
        if (n == 0) return 0f
        val i = pos.toInt()
        if (i < 1 || i >= n - 2) return data[i.coerceIn(0, n - 1)]
        val frac = (pos - i).toFloat()
        val p0 = data[i - 1]; val p1 = data[i]; val p2 = data[i + 1]; val p3 = data[i + 2]
        val a = p1
        val b = 0.5f * (p2 - p0)
        val c = p0 - 2.5f * p1 + 2f * p2 - 0.5f * p3
        val d = 0.5f * (p3 - p0) + 1.5f * (p1 - p2)
        return ((d * frac + c) * frac + b) * frac + a
    }

    fun stop() {
        running = false
        renderThread?.join(200); renderThread = null
        at?.run { runCatching { pause(); flush(); stop(); release() } }
        at = null
    }
}
