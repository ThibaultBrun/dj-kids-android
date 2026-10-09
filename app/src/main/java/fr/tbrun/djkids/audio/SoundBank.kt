package fr.tbrun.djkids.audio

/**
 * Banque de sons intégrée à l'app : breakbeats (boucles pour scratcher par-dessus),
 * samples de scratch classiques (vocal "Ahh", "Fresh"…) et FX.
 *
 * ⚠️ Publication Play Store : ne bundler QUE des fichiers CC0 / libres de droits.
 * Les fichiers vivent dans `app/src/main/assets/sounds/<category>/<file>`.
 * Le manifeste ci-dessous liste ce qu'on embarque ; [file] = chemin relatif à `assets/`.
 */

enum class SoundCategory { BREAKBEAT, SCRATCH, FX }

data class SoundSample(
    val id: String,
    val name: String,
    val category: SoundCategory,
    val file: String,
    /** Pour un breakbeat : BPM d'origine (sert au trainer pour caler la boucle). */
    val bpm: Int? = null,
    /** true = destiné à être joué en boucle (breakbeats). */
    val loop: Boolean = false,
    /** Licence / source, pour la page crédits (exigée par certaines licences CC). */
    val credit: String = "CC0",
)

object SoundBank {

    val breakbeats: List<SoundSample> = listOf(
        SoundSample("break_amen_slow", "Break lent", SoundCategory.BREAKBEAT, "sounds/breakbeat/amen_slow.wav", bpm = 70, loop = true),
        SoundSample("break_funky", "Funky break", SoundCategory.BREAKBEAT, "sounds/breakbeat/funky.wav", bpm = 90, loop = true),
        SoundSample("break_boombap", "Boom bap", SoundCategory.BREAKBEAT, "sounds/breakbeat/boombap.wav", bpm = 85, loop = true),
    )

    val scratches: List<SoundSample> = listOf(
        SoundSample("scr_ahh", "Ahh", SoundCategory.SCRATCH, "sounds/scratch/ahh.wav"),
        SoundSample("scr_fresh", "Fresh", SoundCategory.SCRATCH, "sounds/scratch/fresh.wav"),
        SoundSample("scr_aah_yeah", "Aah yeah", SoundCategory.SCRATCH, "sounds/scratch/aah_yeah.wav"),
    )

    val fx: List<SoundSample> = listOf(
        SoundSample("fx_airhorn", "Corne de brume", SoundCategory.FX, "sounds/fx/airhorn.wav"),
        SoundSample("fx_rewind", "Rewind", SoundCategory.FX, "sounds/fx/rewind.wav"),
    )

    val all: List<SoundSample> get() = breakbeats + scratches + fx

    fun byId(id: String): SoundSample? = all.firstOrNull { it.id == id }

    /** Breakbeat par défaut pour le trainer de scratch. */
    val defaultBreak: SoundSample get() = breakbeats.first()

    /** Sample de scratch par défaut (vocal "Ahh" = le son canonique du baby scratch). */
    val defaultScratch: SoundSample get() = scratches.first()
}
