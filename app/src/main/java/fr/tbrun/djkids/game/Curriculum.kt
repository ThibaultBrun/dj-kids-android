package fr.tbrun.djkids.game

/**
 * Programme d'apprentissage du scratch. Chaque [Technique] = un chapitre ; chaque chapitre
 * a 5 niveaux de difficulté rythmique croissante (noires → tout). Le trainer génère un
 * pattern de "hits" (moments où scratcher, calés sur les subdivisions du beat) à partir du
 * [RhythmProfile] du niveau, le joue sur un breakbeat en boucle, et score la précision du geste.
 *
 * Spéc d'origine (Thibault, 2026-10-10) : Ch.1 Baby scratch → N1 noires, N2 croches,
 * N3 noires+croches, N4 doubles croches, N5 tout. Puis autres techniques.
 */

/** Valeur rythmique = subdivision du temps (4/4). */
enum class NoteValue(val perBeat: Int, val label: String) {
    QUARTER(1, "Noire"),      // 1 par temps
    EIGHTH(2, "Croche"),      // 2 par temps
    SIXTEENTH(4, "Double croche"), // 4 par temps
}

/** Ce qui est autorisé rythmiquement à un niveau donné. */
data class RhythmProfile(
    val allowed: Set<NoteValue>,
    /** Densité 0..1 : proportion d'emplacements effectivement joués (1 = tous). */
    val density: Float,
    /** Autorise le mélange de plusieurs valeurs dans une même mesure. */
    val mixed: Boolean,
)

data class Level(
    val index: Int,          // 1..5
    val title: String,
    val rhythm: RhythmProfile,
    val bpm: Int,
    /** Nombre de mesures (4 temps) à réussir pour valider le niveau. */
    val bars: Int = 4,
    /** Score de précision minimal (0..1) pour débloquer le suivant. */
    val passAccuracy: Float = 0.7f,
)

/** Une technique de scratch = un chapitre. */
data class Technique(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    /** Explication du geste, affichée avant de lancer le niveau. */
    val howTo: String,
    val levels: List<Level>,
    /**
     * true = technique qui se score sur le **cut** (crossfader) et non sur l'aller-retour du jog.
     * Le jog ne fait alors que produire le son ; c'est le coup de crossfader calé sur le rythme
     * qui est jugé (forward, chirp, transformer, flare…).
     */
    val usesCut: Boolean = false,
)

object Curriculum {

    /** Les 5 niveaux standards d'une technique, construits depuis un BPM de base. */
    private fun standardLevels(baseBpm: Int): List<Level> = listOf(
        Level(1, "Que des noires", RhythmProfile(setOf(NoteValue.QUARTER), 1f, mixed = false), baseBpm),
        Level(2, "Les croches", RhythmProfile(setOf(NoteValue.EIGHTH), 0.85f, mixed = false), baseBpm),
        Level(3, "Noires + croches", RhythmProfile(setOf(NoteValue.QUARTER, NoteValue.EIGHTH), 0.8f, mixed = true), baseBpm + 4),
        Level(4, "Doubles croches", RhythmProfile(setOf(NoteValue.SIXTEENTH), 0.7f, mixed = false), baseBpm + 4),
        Level(5, "Tout !", RhythmProfile(NoteValue.entries.toSet(), 0.75f, mixed = true), baseBpm + 8),
    )

    val techniques: List<Technique> = listOf(
        Technique(
            id = "baby",
            name = "Baby scratch",
            emoji = "👶",
            description = "La base : on pousse et on tire le disque, sans toucher le crossfader.",
            howTo = "Pose la main sur le jog. À chaque voyant, fais un aller-retour (avant puis arrière). " +
                "Reste bien calé sur le rythme du break.",
            levels = standardLevels(70),
        ),
        Technique(
            id = "cut",
            name = "Le cut",
            emoji = "✂️",
            description = "Hacher le son avec le crossfader, bien sur le rythme. La base de toutes les coupes.",
            howTo = "Laisse le son tourner et coupe-le net avec le crossfader à chaque voyant. " +
                "Ici on ne juge que le CUT (le crossfader), pas le jog.",
            levels = standardLevels(70),
            usesCut = true,
        ),
        Technique(
            id = "forward",
            name = "Forward scratch",
            emoji = "▶️",
            description = "On ne garde que le son quand on pousse vers l'avant (crossfader coupé au retour).",
            howTo = "Pousse le jog vers l'avant sur le temps, coupe au retour. Un son net par hit.",
            levels = standardLevels(72),
            usesCut = true,
        ),
        Technique(
            id = "chirp",
            name = "Chirp",
            emoji = "🐦",
            description = "Un \"tchirp\" : on coupe le crossfader au tout début et à la fin du mouvement.",
            howTo = "Lance le son puis coupe vite : ça fait un petit cri d'oiseau. Avant ET arrière.",
            levels = standardLevels(74),
            usesCut = true,
        ),
        Technique(
            id = "tear",
            name = "Tear scratch",
            emoji = "💧",
            description = "Le retour se fait en deux temps : ça \"déchire\" le son.",
            howTo = "Aller d'un coup, retour en deux petits mouvements. Garde le groove.",
            levels = standardLevels(74),
        ),
        Technique(
            id = "scribble",
            name = "Scribble",
            emoji = "✏️",
            description = "Des petits allers-retours très rapides, poignet tendu.",
            howTo = "Petits mouvements secs et rapides, comme si tu gribouillais. Suis la cadence.",
            levels = standardLevels(68),
        ),
        Technique(
            id = "transformer",
            name = "Transformer",
            emoji = "🤖",
            description = "Le son continu est haché par des coups de crossfader.",
            howTo = "Bouge le jog lentement et hache avec le crossfader sur chaque hit.",
            levels = standardLevels(76),
            usesCut = true,
        ),
        Technique(
            id = "flare",
            name = "Flare",
            emoji = "🔥",
            description = "Scratch avancé : un (ou deux) \"clics\" de crossfader pendant le mouvement.",
            howTo = "Mouvement continu + 1 clic de crossfader au milieu. Niveau expert.",
            levels = standardLevels(80),
            usesCut = true,
        ),
        Technique(
            id = "orbit",
            name = "Orbit",
            emoji = "🪐",
            description = "Le sommet du scratch : deux clics de crossfader dans un seul mouvement, dans les deux sens.",
            howTo = "Mouvement aller-retour continu + 2 clics de crossfader (début et milieu de chaque sens). " +
                "Maîtrise le Flare avant d'attaquer ça.",
            levels = standardLevels(82),
            usesCut = true,
        ),
    )

    fun technique(id: String): Technique? = techniques.firstOrNull { it.id == id }
}
