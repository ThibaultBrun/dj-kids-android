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

    // Vrais samples CC0 (Freesound). Détail licences/auteurs : assets/sounds/CREDITS.md.
    val breakbeats: List<SoundSample> = listOf(
        SoundSample("break_driver", "Driver break", SoundCategory.BREAKBEAT, "sounds/breakbeat/break4.wav", loop = true, credit = "CC0 vekoN"),
        SoundSample("break_silly1", "Silly break 1", SoundCategory.BREAKBEAT, "sounds/breakbeat/break1.wav", loop = true, credit = "CC0 RutgerMuller"),
        SoundSample("break_silly2", "Silly break 2", SoundCategory.BREAKBEAT, "sounds/breakbeat/break2.wav", loop = true, credit = "CC0 RutgerMuller"),
        SoundSample("break_crunchy", "Crunchy break", SoundCategory.BREAKBEAT, "sounds/breakbeat/break3.wav", bpm = 164, loop = true, credit = "CC0 deadrobotmusic"),
    )

    val scratches: List<SoundSample> = listOf(
        SoundSample("scr_transform1", "Transform 1", SoundCategory.SCRATCH, "sounds/scratch/scratch1.wav", credit = "CC0 G_M_D_THREE"),
        SoundSample("scr_transform2", "Transform 2", SoundCategory.SCRATCH, "sounds/scratch/scratch2.wav", credit = "CC0 G_M_D_THREE"),
        SoundSample("scr_backdrag1", "Back drag 1", SoundCategory.SCRATCH, "sounds/scratch/scratch3.wav", credit = "CC0 G_M_D_THREE"),
        SoundSample("scr_transform3", "Transform 3", SoundCategory.SCRATCH, "sounds/scratch/scratch4.wav", credit = "CC0 G_M_D_THREE"),
        SoundSample("scr_backdrag2", "Back drag 2", SoundCategory.SCRATCH, "sounds/scratch/scratch5.wav", credit = "CC0 G_M_D_THREE"),
        SoundSample("scr_rumble", "Rumble", SoundCategory.SCRATCH, "sounds/scratch/scratch6.wav", credit = "CC0 PuzzleAndy"),
    )

    val fx: List<SoundSample> = listOf(
        SoundSample("fx_airhorn", "Air horn", SoundCategory.FX, "sounds/fx/fx1.wav", credit = "CC0 mcpable"),
        SoundSample("fx_horn_bike", "Klaxon", SoundCategory.FX, "sounds/fx/fx2.wav", credit = "CC0 loganzsound"),
        SoundSample("fx_airhorn2", "Air horn 2", SoundCategory.FX, "sounds/fx/fx3.wav", credit = "CC0 jacksonacademyashmore"),
    )

    val all: List<SoundSample> get() = breakbeats + scratches + fx

    fun byId(id: String): SoundSample? = all.firstOrNull { it.id == id }

    /** Breakbeat par défaut pour le trainer de scratch. */
    val defaultBreak: SoundSample get() = breakbeats.first()

    /** Sample de scratch par défaut pour le trainer. */
    val defaultScratch: SoundSample get() = scratches.first()
}
