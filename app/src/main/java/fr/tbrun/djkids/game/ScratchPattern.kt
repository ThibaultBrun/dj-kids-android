package fr.tbrun.djkids.game

import kotlin.random.Random

/** Un "hit" = un moment où scratcher, exprimé en temps (beats) depuis le début. */
data class Hit(val beat: Double, val value: NoteValue)

/** Pattern complet d'un niveau : la liste des hits à jouer, + les métadonnées de tempo. */
data class ScratchPattern(
    val bpm: Int,
    val beatsPerBar: Int,
    val bars: Int,
    val hits: List<Hit>,
) {
    val totalBeats: Int get() = beatsPerBar * bars
    val beatMs: Double get() = 60_000.0 / bpm
    fun durationMs(): Double = totalBeats * beatMs
}

/**
 * Génère un pattern à partir d'un [Level]. On remplit chaque temps avec la grille la plus fine
 * autorisée, puis on garde les emplacements selon la densité et les valeurs permises.
 */
object PatternGenerator {

    fun generate(level: Level, beatsPerBar: Int = 4, seed: Long = System.currentTimeMillis()): ScratchPattern {
        val rng = Random(seed)
        val r = level.rhythm
        val finest = r.allowed.maxByOrNull { it.perBeat } ?: NoteValue.QUARTER
        val hits = mutableListOf<Hit>()

        for (bar in 0 until level.bars) {
            for (beat in 0 until beatsPerBar) {
                val slots = finest.perBeat
                for (s in 0 until slots) {
                    val pos = bar * beatsPerBar + beat + s.toDouble() / slots
                    val value = valueForSlot(s, slots, r, rng) ?: continue
                    // Densité : on peut sauter un emplacement (sauf le tout premier, pour l'amorce).
                    if (pos > 0.0 && rng.nextFloat() > r.density) continue
                    hits.add(Hit(pos, value))
                }
            }
        }
        // Garantit au moins un hit sur le premier temps.
        if (hits.none { it.beat == 0.0 }) {
            hits.add(0, Hit(0.0, r.allowed.minByOrNull { it.perBeat } ?: NoteValue.QUARTER))
        }
        return ScratchPattern(level.bpm, beatsPerBar, level.bars, hits.sortedBy { it.beat })
    }

    /**
     * Décide quelle valeur occupe l'emplacement `s` sur `slots` subdivisions d'un temps.
     * - Non mélangé : une seule valeur autorisée → on ne pose que sur les emplacements qui tombent dessus.
     * - Mélangé : on choisit aléatoirement une valeur compatible avec cet emplacement.
     */
    private fun valueForSlot(s: Int, slots: Int, r: RhythmProfile, rng: Random): NoteValue? {
        val candidates = r.allowed.filter { v ->
            // L'emplacement s/slots doit coïncider avec une subdivision de la valeur v.
            val step = slots / v.perBeat
            step > 0 && s % step == 0
        }
        if (candidates.isEmpty()) return null
        if (!r.mixed) {
            // On privilégie la valeur la plus "simple" qui tombe ici.
            return candidates.minByOrNull { it.perBeat }
        }
        return candidates[rng.nextInt(candidates.size)]
    }
}
