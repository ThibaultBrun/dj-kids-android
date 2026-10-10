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

    // Vrais sons CC0 (Freesound). Détail licences/auteurs : assets/sounds/CREDITS.md.
    // Breakbeats = instrus hip-hop de plusieurs minutes (pour scratcher/mixer par-dessus).
    val breakbeats: List<SoundSample> = listOf(
        SoundSample("bb_liberian", "Liberian Girl", SoundCategory.BREAKBEAT, "sounds/breakbeat/break1.mp3", credit = "CC0 Soulja Unit"),
        SoundSample("bb_reminder", "The Reminder", SoundCategory.BREAKBEAT, "sounds/breakbeat/break2.mp3", credit = "CC0 Soulja Unit"),
        SoundSample("bb_struggling", "Struggling", SoundCategory.BREAKBEAT, "sounds/breakbeat/break3.mp3", credit = "CC0 Soulja Unit"),
        SoundSample("bb_lovemotion", "Love Motion", SoundCategory.BREAKBEAT, "sounds/breakbeat/break4.mp3", credit = "CC0 Soulja Unit"),
        SoundSample("bb_urtime", "Is It Ur Time", SoundCategory.BREAKBEAT, "sounds/breakbeat/break5.mp3", credit = "CC0 Soulja Unit"),
    )

    // Scratch = VOIX BRUTES (one-shots) à scratcher soi-même avec le jog. CC0 Freesound (mieux notées).
    val scratches: List<SoundSample> = listOf(
        SoundSample("scr_sample", "Scratch sample", SoundCategory.SCRATCH, "sounds/scratch/scratch4.ogg", credit = "CC0 Freesound"),
        SoundSample("scr_yeah", "Yeah !", SoundCategory.SCRATCH, "sounds/scratch/scratch1.ogg", credit = "CC0 Freesound"),
        SoundSample("scr_ahyeah", "Ah Yeah", SoundCategory.SCRATCH, "sounds/scratch/scratch6.ogg", credit = "CC0 Freesound"),
        SoundSample("scr_awyeah", "Aw Yeah", SoundCategory.SCRATCH, "sounds/scratch/scratch7.ogg", credit = "CC0 Freesound"),
        SoundSample("scr_hey_grp", "Hey (groupe)", SoundCategory.SCRATCH, "sounds/scratch/scratch2.ogg", credit = "CC0 Freesound"),
        SoundSample("scr_hey_low", "Hey grave", SoundCategory.SCRATCH, "sounds/scratch/scratch3.ogg", credit = "CC0 Freesound"),
        SoundSample("scr_chop", "Vocal chop", SoundCategory.SCRATCH, "sounds/scratch/scratch5.ogg", credit = "CC0 Freesound"),
    )

    val fx: List<SoundSample> = listOf(
        SoundSample("fx_airhorn", "Air horn", SoundCategory.FX, "sounds/fx/fx1.mp3", credit = "CC0 mcpable"),
        SoundSample("fx_horn_bike", "Klaxon", SoundCategory.FX, "sounds/fx/fx2.mp3", credit = "CC0 loganzsound"),
        SoundSample("fx_airhorn2", "Air horn 2", SoundCategory.FX, "sounds/fx/fx3.mp3", credit = "CC0 jacksonacademyashmore"),
    )

    val all: List<SoundSample> get() = breakbeats + scratches + fx

    fun byId(id: String): SoundSample? = all.firstOrNull { it.id == id }

    /** Breakbeat par défaut pour le trainer de scratch. */
    val defaultBreak: SoundSample get() = breakbeats.first()

    /** Sample de scratch par défaut pour le trainer. */
    val defaultScratch: SoundSample get() = scratches.first()
}
