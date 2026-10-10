package fr.tbrun.djkids.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Jugement d'un scratch joué par l'élève, façon rythmique. */
enum class Judgement(val label: String, val score: Float) {
    PERFECT("Parfait", 1f),
    GOOD("Bien", 0.7f),
    OK("Ok", 0.4f),
    MISS("Raté", 0f),
}

data class ScoredHit(val hit: Hit, val judged: Judgement)

data class TrainerResult(
    val total: Int,
    val matched: Int,
    val accuracy: Float,
    val maxCombo: Int,
    val passed: Boolean,
)

/**
 * Moteur du mode apprentissage : joue l'horloge de la boucle, expose la position de lecture
 * (pour l'UI : curseur, voyants) et score les scratchs de l'élève par rapport aux hits attendus.
 *
 * Indépendant de l'audio : l'écran branche le son (ScratchEngine) et l'entrée (jog/tactile),
 * et appelle [registerScratch] à chaque aller-retour détecté.
 */
class TrainerEngine(private val scope: CoroutineScope) {

    private val _positionBeat = MutableStateFlow(0.0)
    val positionBeat: StateFlow<Double> = _positionBeat

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    private val _combo = MutableStateFlow(0)
    val combo: StateFlow<Int> = _combo

    private val _score = MutableStateFlow(0f)
    val score: StateFlow<Float> = _score

    /** Dernier jugement, pour le feedback visuel ("Parfait !"). */
    private val _lastJudgement = MutableStateFlow<Judgement?>(null)
    val lastJudgement: StateFlow<Judgement?> = _lastJudgement

    private val _result = MutableStateFlow<TrainerResult?>(null)
    val result: StateFlow<TrainerResult?> = _result

    private var pattern: ScratchPattern? = null
    private var level: Level? = null
    private var clockJob: Job? = null
    private var startNanos = 0L
    private var countInBeats = 4 // une mesure de décompte avant le premier hit
    private var maxCombo = 0

    fun load(level: Level, seed: Long = System.currentTimeMillis()) {
        this.level = level
        pattern = PatternGenerator.generate(level, seed = seed)
        _result.value = null
        _lastJudgement.value = null
        _positionBeat.value = -countInBeats.toDouble()
    }

    /** Ferme l'écran de résultat (évite la boucle du dialogue). */
    fun clearResult() { _result.value = null }

    fun currentPattern(): ScratchPattern? = pattern

    fun start() {
        val pat = pattern ?: return
        stopClock()
        _result.value = null
        _combo.value = 0
        _score.value = 0f
        maxCombo = 0
        matchedFlags = BooleanArray(pat.hits.size)
        startNanos = System.nanoTime()
        _running.value = true

        clockJob = scope.launch {
            val beatMs = pat.beatMs
            val totalMs = (pat.totalBeats + countInBeats) * beatMs
            while (_running.value) {
                val elapsed = (System.nanoTime() - startNanos) / 1_000_000.0
                _positionBeat.value = elapsed / beatMs - countInBeats
                if (elapsed >= totalMs) { finish(); break }
                delay(16)
            }
        }
    }

    private var matchedFlags = BooleanArray(0)

    /**
     * Appelé quand l'élève déclenche un scratch (aller-retour détecté). Compare l'instant à la
     * grille et attribue un jugement. [atBeat] = position musicale au moment du geste.
     */
    fun registerScratch(atBeat: Double = _positionBeat.value): Judgement {
        val pat = pattern ?: return Judgement.MISS
        if (atBeat < -0.5) return Judgement.MISS // pendant le décompte

        // Hit le plus proche encore non validé.
        var bestIdx = -1
        var bestDist = Double.MAX_VALUE
        pat.hits.forEachIndexed { i, h ->
            if (!matchedFlags.getOrElse(i) { true }) {
                val d = abs(h.beat - atBeat)
                if (d < bestDist) { bestDist = d; bestIdx = i }
            }
        }

        val j = when {
            bestIdx < 0 -> Judgement.MISS
            bestDist <= 0.12 -> Judgement.PERFECT
            bestDist <= 0.25 -> Judgement.GOOD
            bestDist <= 0.45 -> Judgement.OK
            else -> Judgement.MISS
        }

        if (j != Judgement.MISS && bestIdx >= 0) {
            matchedFlags[bestIdx] = true
            _combo.value += 1
            maxCombo = maxOf(maxCombo, _combo.value)
            _score.value += j.score
        } else {
            _combo.value = 0
        }
        _lastJudgement.value = j
        return j
    }

    private fun finish() {
        val pat = pattern ?: return
        val lvl = level
        val matchedCount = matchedFlags.count { it }
        val total = pat.hits.size.coerceAtLeast(1)
        val accuracy = _score.value / total
        val passed = lvl != null && accuracy >= lvl.passAccuracy
        _result.value = TrainerResult(total, matchedCount, accuracy, maxCombo, passed)
        _running.value = false
    }

    fun stop() { stopClock(); _running.value = false }

    private fun stopClock() {
        clockJob?.cancel()
        clockJob = null
    }
}
