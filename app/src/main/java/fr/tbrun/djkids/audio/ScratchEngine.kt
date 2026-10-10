package fr.tbrun.djkids.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * Moteur de scratch temps réel, basse latence (AudioTrack en float + LOW_LATENCY).
 *
 * Deux couches mixées :
 *  - un **breakbeat** en boucle (le fond sur lequel on scratche),
 *  - un **sample de scratch** (vocal "Ahh"…) dont la tête de lecture est pilotée par le jog :
 *    la vitesse peut être positive (avant), nulle (figé) ou négative (arrière) → c'est ça, le scratch.
 *
 * Phase 4 "Oboe" : on pourra remplacer AudioTrack par Oboe/AAudio pour gratter la latence, l'API
 * publique ci-dessous restant identique. Pour l'instant AudioTrack LOW_LATENCY suffit au trainer.
 */
class ScratchEngine(private val outputSampleRate: Int = 48000) {

    @Volatile private var scratch: PcmSample? = null
    @Volatile private var breakLoop: PcmSample? = null

    // Tête de lecture du scratch (en frames, fractionnaire) + vitesse (frames de sample / frame de sortie).
    @Volatile private var scratchPos = 0.0
    @Volatile private var scratchSpeed = 0.0
    @Volatile private var scratchGain = 1f

    // Breakbeat
    @Volatile private var breakPos = 0.0
    @Volatile private var breakRate = 1.0
    @Volatile private var breakPlaying = false
    @Volatile private var breakGain = 0.9f

    // Crossfader virtuel (0..1) pour couper le scratch (forward/transformer/chirp…).
    @Volatile private var faderOpen = 1f

    @Volatile private var running = false
    private var audioTrack: AudioTrack? = null
    private var renderThread: Thread? = null

    fun setScratchSample(s: PcmSample?) { scratch = s; scratchPos = 0.0 }
    fun setBreak(s: PcmSample?) { breakLoop = s; breakPos = 0.0 }

    /** Vitesse de scratch : 1.0 = vitesse normale avant, -1.0 = arrière, 0 = figé. */
    fun setScratchSpeed(v: Double) { scratchSpeed = v }

    /** Nouvelle attaque : on repart du début du sample (ou d'un offset). */
    fun pluck(fromFrame: Int = 0) { scratchPos = fromFrame.toDouble() }

    fun setFader(open: Float) { faderOpen = open.coerceIn(0f, 1f) }
    fun setScratchGain(g: Float) { scratchGain = g.coerceIn(0f, 1f) }

    fun setBreakPlaying(playing: Boolean) { breakPlaying = playing }
    fun setBreakGain(g: Float) { breakGain = g.coerceIn(0f, 1f) }

    /** Cale la boucle de break pour qu'elle tourne à [targetBpm] si son BPM d'origine est connu. */
    fun setBreakBpm(originalBpm: Int?, targetBpm: Int) {
        breakRate = if (originalBpm != null && originalBpm > 0) targetBpm.toDouble() / originalBpm else 1.0
    }

    fun start() {
        if (running) return
        val minBuf = AudioTrack.getMinBufferSize(
            outputSampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT,
        ).coerceAtLeast(2048)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(outputSampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        running = true
        track.play()

        val framesPerBlock = 256
        renderThread = thread(name = "scratch-render", priority = Thread.MAX_PRIORITY) {
            val buf = FloatArray(framesPerBlock * 2) // stéréo entrelacé
            while (running) {
                render(buf, framesPerBlock)
                val written = track.write(buf, 0, buf.size, AudioTrack.WRITE_BLOCKING)
                if (written < 0) break
            }
        }
    }

    private fun render(out: FloatArray, frames: Int) {
        val scr = scratch
        val brk = breakLoop
        val srcRateScr = (scr?.sampleRate ?: outputSampleRate).toDouble() / outputSampleRate
        val srcRateBrk = (brk?.sampleRate ?: outputSampleRate).toDouble() / outputSampleRate

        for (i in 0 until frames) {
            var s = 0f

            // Couche scratch : avance selon la vitesse de scratch (× ratio de resampling).
            if (scr != null && abs(scratchSpeed) > 1e-6) {
                val sample = sampleAt(scr.data, scratchPos)
                s += sample * scratchGain * faderOpen
                scratchPos += scratchSpeed * srcRateScr
                if (scratchPos < 0) scratchPos = 0.0
                if (scratchPos >= scr.frames) scratchPos = (scr.frames - 1).toDouble()
            }

            // Couche breakbeat : boucle continue.
            if (brk != null && breakPlaying) {
                s += sampleAt(brk.data, breakPos) * breakGain
                breakPos += breakRate * srcRateBrk
                if (breakPos >= brk.frames) breakPos -= brk.frames
            }

            val v = s.coerceIn(-1f, 1f)
            out[i * 2] = v
            out[i * 2 + 1] = v
        }
    }

    /** Lecture interpolée linéaire (évite les clics quand la tête est fractionnaire). */
    /** Interpolation cubique (Catmull-Rom) : scratch plus propre (moins d'aliasing) que le linéaire. */
    private fun sampleAt(data: FloatArray, pos: Double): Float {
        val n = data.size
        if (n == 0) return 0f
        val i = pos.toInt()
        if (i < 1 || i >= n - 2) return data[i.coerceIn(0, n - 1)]
        val frac = (pos - i).toFloat()
        val p0 = data[i - 1]; val p1 = data[i]; val p2 = data[i + 1]; val p3 = data[i + 2]
        val b = 0.5f * (p2 - p0)
        val c = p0 - 2.5f * p1 + 2f * p2 - 0.5f * p3
        val d = 0.5f * (p3 - p0) + 1.5f * (p1 - p2)
        return ((d * frac + c) * frac + b) * frac + p1
    }

    fun stop() {
        running = false
        renderThread?.join(200)
        renderThread = null
        audioTrack?.run { runCatching { pause(); flush(); stop(); release() } }
        audioTrack = null
    }
}
