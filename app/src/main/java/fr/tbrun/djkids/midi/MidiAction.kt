package fr.tbrun.djkids.midi

import fr.tbrun.djkids.model.DeckId

/**
 * Fonction DJ qu'un contrôle du Hercules peut déclencher. Le mapping associe une "clé de
 * contrôle" (ex. "N:1:7") à une de ces fonctions. Les contrôles continus (faders, knobs, jogs)
 * transmettent la valeur normalisée ; les boutons transmettent l'état pressé/relâché.
 */
enum class ControlFunction(val label: String, val continuous: Boolean, val perDeck: Boolean) {
    NONE("—", false, false),

    // Boutons par platine
    PLAY("Play / Pause", false, true),
    CUE("Cue", false, true),
    SYNC("Sync", false, true),
    LOAD("Charger un morceau", false, true),

    // Faders / knobs par platine
    VOLUME("Volume", true, true),
    TEMPO("Pitch / Tempo", true, true),
    EQ_LOW("EQ Grave", true, true),
    EQ_MID("EQ Médium", true, true),
    EQ_HIGH("EQ Aigu", true, true),
    FILTER("Filtre", true, true),
    JOG("Jog (nudge / scratch)", true, true),
    JOG_TOUCH("Contact jog", false, true),

    // Pads par platine (num = index 0..7 via param)
    PAD("Pad (hot cue / sample)", false, true),

    // Mixer global
    CROSSFADER("Crossfader", true, false),
    MASTER("Volume master", true, false),
}

/** Action résolue, prête à appliquer par le moteur audio. */
sealed interface MidiAction {
    data class Button(val fn: ControlFunction, val deck: DeckId?, val pressed: Boolean, val pad: Int = -1) : MidiAction
    data class Continuous(val fn: ControlFunction, val deck: DeckId?, val value: Float) : MidiAction
    /** Jog relatif : delta signé (tours), + = avant. */
    data class Jog(val deck: DeckId?, val delta: Float, val touching: Boolean) : MidiAction
    data object Unmapped : MidiAction
}
