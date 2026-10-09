package fr.tbrun.djkids.midi

import android.content.Context
import fr.tbrun.djkids.model.DeckId
import org.json.JSONArray
import org.json.JSONObject

/** Liaison d'un contrôle physique vers une fonction DJ. */
data class Binding(
    val fn: ControlFunction,
    val deck: DeckId?,
    val pad: Int = -1,
    /** Inverse le sens d'un fader/knob continu (utile si le contrôleur envoie 127→0). */
    val inverted: Boolean = false,
)

/**
 * Mapping MIDI persistant (SharedPreferences, JSON).
 *
 * La MK3 étant trop récente, ses codes exacts ne sont pas encore figés : plutôt que de deviner,
 * on apprend chaque contrôle via le **MIDI Learn** (écran Mapping). Un preset "devinette
 * Inpulse 200" est fourni pour démarrer vite, à corriger avec le monitor si besoin.
 */
class MidiMapping(context: Context) {

    private val prefs = context.getSharedPreferences("midi_mapping", Context.MODE_PRIVATE)
    private val bindings = HashMap<String, Binding>()

    init { load() }

    fun all(): Map<String, Binding> = bindings.toMap()

    fun bindingFor(fn: ControlFunction, deck: DeckId?, pad: Int = -1): String? =
        bindings.entries.firstOrNull {
            it.value.fn == fn && it.value.deck == deck && it.value.pad == pad
        }?.key

    /** Associe (ou réassocie) une clé de contrôle à une fonction. */
    fun learn(controlKey: String, binding: Binding) {
        // Un contrôle = une seule fonction : on retire l'ancienne liaison de cette clé.
        bindings[controlKey] = binding
        save()
    }

    fun clear(controlKey: String) {
        bindings.remove(controlKey)
        save()
    }

    fun clearAll() {
        bindings.clear()
        save()
    }

    /** Résout un message entrant en action DJ applicable. */
    fun resolve(msg: MidiMessage): MidiAction {
        val b = bindings[msg.controlKey()] ?: return MidiAction.Unmapped
        return when {
            b.fn == ControlFunction.JOG -> {
                val delta = jogDelta(msg)
                MidiAction.Jog(b.deck, delta, touching = true)
            }
            b.fn.continuous -> {
                var v = normalized(msg)
                if (b.inverted) v = 1f - v
                MidiAction.Continuous(b.fn, b.deck, v)
            }
            else -> {
                val pressed = isPressed(msg)
                MidiAction.Button(b.fn, b.deck, pressed, b.pad)
            }
        }
    }

    private fun normalized(msg: MidiMessage): Float = when (msg.type) {
        MidiType.PITCH_BEND -> msg.value14 / 16383f
        else -> msg.data2 / 127f
    }

    private fun isPressed(msg: MidiMessage): Boolean = when (msg.type) {
        MidiType.NOTE_ON -> msg.data2 > 0
        MidiType.NOTE_OFF -> false
        MidiType.CC -> msg.data2 >= 64
        else -> msg.data2 > 0
    }

    /** Décodage relatif classique (two's complement autour de 64). */
    private fun jogDelta(msg: MidiMessage): Float {
        val v = msg.data2
        val signed = if (v < 64) v else v - 128
        return signed / 20f // pas de tour ; ajustable
    }

    // --- Persistance JSON ---

    private fun save() {
        val arr = JSONArray()
        bindings.forEach { (key, b) ->
            arr.put(JSONObject().apply {
                put("key", key)
                put("fn", b.fn.name)
                put("deck", b.deck?.name ?: JSONObject.NULL)
                put("pad", b.pad)
                put("inv", b.inverted)
            })
        }
        prefs.edit().putString("bindings", arr.toString()).apply()
    }

    private fun load() {
        bindings.clear()
        val s = prefs.getString("bindings", null) ?: return
        runCatching {
            val arr = JSONArray(s)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val fn = ControlFunction.valueOf(o.getString("fn"))
                val deck = o.optString("deck", "").takeIf { it.isNotEmpty() && it != "null" }
                    ?.let { DeckId.valueOf(it) }
                bindings[o.getString("key")] = Binding(
                    fn = fn,
                    deck = deck,
                    pad = o.optInt("pad", -1),
                    inverted = o.optBoolean("inv", false),
                )
            }
        }
    }

    /**
     * Preset "devinette Inpulse 200" : hypothèses de départ (canal 1 = platine A, canal 2 = B).
     * ⚠️ À vérifier avec le MIDI monitor : si un contrôle ne répond pas, utilise le MIDI Learn.
     */
    fun applyInpulse200Preset() {
        bindings.clear()
        fun deckCh(d: DeckId) = if (d == DeckId.A) 1 else 2
        for (d in DeckId.entries) {
            val ch = deckCh(d)
            // Boutons transport (notes présumées)
            learnSilent("N:$ch:0x07".hx(), Binding(ControlFunction.PLAY, d))
            learnSilent("N:$ch:0x06".hx(), Binding(ControlFunction.CUE, d))
            learnSilent("N:$ch:0x05".hx(), Binding(ControlFunction.SYNC, d))
            learnSilent("N:$ch:0x0D".hx(), Binding(ControlFunction.LOAD, d))
            // Faders / knobs (CC présumés)
            learnSilent("CC:$ch:0x00".hx(), Binding(ControlFunction.VOLUME, d))
            learnSilent("CC:$ch:0x08".hx(), Binding(ControlFunction.TEMPO, d))
            learnSilent("CC:$ch:0x02".hx(), Binding(ControlFunction.EQ_HIGH, d))
            learnSilent("CC:$ch:0x03".hx(), Binding(ControlFunction.EQ_MID, d))
            learnSilent("CC:$ch:0x04".hx(), Binding(ControlFunction.EQ_LOW, d))
            learnSilent("CC:$ch:0x09".hx(), Binding(ControlFunction.FILTER, d))
            learnSilent("CC:$ch:0x0A".hx(), Binding(ControlFunction.JOG, d))
            // 8 pads (notes 0x10.. présumées)
            for (p in 0 until 8) {
                learnSilent("N:$ch:${0x10 + p}", Binding(ControlFunction.PAD, d, pad = p))
            }
        }
        // Mixer global sur canal 1 (présumé)
        learnSilent("CC:1:0x01".hx(), Binding(ControlFunction.CROSSFADER, null))
        learnSilent("CC:1:0x0C".hx(), Binding(ControlFunction.MASTER, null))
        save()
    }

    private fun learnSilent(key: String, b: Binding) { bindings[key] = b }

    /** Convertit "N:1:0x07" -> "N:1:7" (clés stockées en décimal). */
    private fun String.hx(): String {
        val parts = split(":")
        if (parts.size < 3) return this
        val num = parts[2].let { if (it.startsWith("0x")) it.drop(2).toInt(16) else it.toInt() }
        return "${parts[0]}:${parts[1]}:$num"
    }
}
