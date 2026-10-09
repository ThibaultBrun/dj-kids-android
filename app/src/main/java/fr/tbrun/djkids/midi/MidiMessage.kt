package fr.tbrun.djkids.midi

/** Type MIDI canal (status high nibble). */
enum class MidiType(val code: Int) {
    NOTE_ON(0x90),
    NOTE_OFF(0x80),
    CC(0xB0),
    PITCH_BEND(0xE0),
    OTHER(0x00);

    companion object {
        fun from(status: Int): MidiType = when (status and 0xF0) {
            0x90 -> NOTE_ON
            0x80 -> NOTE_OFF
            0xB0 -> CC
            0xE0 -> PITCH_BEND
            else -> OTHER
        }
    }
}

/**
 * Message MIDI décodé.
 * @param channel 1..16
 * @param data1 note/CC number (ou LSB du pitch-bend)
 * @param data2 vélocité/valeur (ou MSB du pitch-bend ; value14 porte la valeur 0..16383)
 */
data class MidiMessage(
    val type: MidiType,
    val channel: Int,
    val data1: Int,
    val data2: Int,
    val raw: Int,
) {
    /** Valeur 14 bits pour le pitch-bend. */
    val value14: Int get() = (data2 shl 7) or data1

    /** Clé stable d'un contrôle, indépendante de la valeur (sert au mapping + MIDI learn). */
    fun controlKey(): String = when (type) {
        MidiType.NOTE_ON, MidiType.NOTE_OFF -> "N:$channel:$data1"
        MidiType.CC -> "CC:$channel:$data1"
        MidiType.PITCH_BEND -> "PB:$channel"
        MidiType.OTHER -> "?:$channel:$data1"
    }

    fun pretty(): String = when (type) {
        MidiType.NOTE_ON -> if (data2 > 0) "NOTE ON  ch$channel  note=$data1  vel=$data2"
                            else "NOTE OFF ch$channel  note=$data1"
        MidiType.NOTE_OFF -> "NOTE OFF ch$channel  note=$data1"
        MidiType.CC -> "CC       ch$channel  num=$data1  val=$data2"
        MidiType.PITCH_BEND -> "PITCH    ch$channel  val=$value14"
        MidiType.OTHER -> "0x%02X     ch$channel  d1=$data1  d2=$data2".format(raw and 0xF0)
    }
}
