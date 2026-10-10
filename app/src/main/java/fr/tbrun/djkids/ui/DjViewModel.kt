package fr.tbrun.djkids.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.tbrun.djkids.audio.DjEngine
import fr.tbrun.djkids.audio.PcmSample
import fr.tbrun.djkids.audio.ScratchEngine
import fr.tbrun.djkids.audio.SoundBank
import fr.tbrun.djkids.game.Curriculum
import fr.tbrun.djkids.game.Level
import fr.tbrun.djkids.game.Technique
import fr.tbrun.djkids.game.TrainerEngine
import fr.tbrun.djkids.midi.Binding
import fr.tbrun.djkids.midi.ControlFunction
import fr.tbrun.djkids.midi.MidiAction
import fr.tbrun.djkids.midi.MidiEngine
import fr.tbrun.djkids.midi.MidiMapping
import fr.tbrun.djkids.midi.MidiMessage
import fr.tbrun.djkids.model.DeckId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Cible de MIDI learn : la fonction qu'on veut associer au prochain contrôle pressé. */
data class LearnTarget(val fn: ControlFunction, val deck: DeckId?, val pad: Int = -1)

/** Gain appliqué au delta du jog MK3 pour obtenir une vitesse de scratch au ressenti naturel. */
private const val SCRATCH_JOG_GAIN = 7.0

class DjViewModel(app: Application) : AndroidViewModel(app) {

    val midi = MidiEngine(app)
    val mapping = MidiMapping(app)
    val engine = DjEngine(app, viewModelScope)

    // --- Mode apprentissage du scratch ---
    val scratch = ScratchEngine()
    val trainer = TrainerEngine(viewModelScope)
    private val prefs = app.getSharedPreferences("dj_progress", Context.MODE_PRIVATE)
    private var scratchSampleLoaded = false
    private var trainerActive = false

    private val _soundsAvailable = MutableStateFlow(false)
    /** true si au moins un sample audio a été trouvé dans les assets. */
    val soundsAvailable: StateFlow<Boolean> = _soundsAvailable

    // Pilotage du scratch par le vrai jog de la MK3 (en plus du tactile).
    private var jogSign = 0
    @Volatile private var lastJogMs = 0L
    private var jogWatchdog: Job? = null

    // Exercices avec le cut (crossfader).
    private var currentUsesCut = false
    private var faderClosed = false

    private val _monitor = MutableStateFlow<List<String>>(emptyList())
    val monitor: StateFlow<List<String>> = _monitor

    private val _learn = MutableStateFlow<LearnTarget?>(null)
    val learn: StateFlow<LearnTarget?> = _learn

    /** Incrémenté à chaque modif du mapping pour rafraîchir l'écran Mapping. */
    private val _mappingVersion = MutableStateFlow(0)
    val mappingVersion: StateFlow<Int> = _mappingVersion

    init {
        viewModelScope.launch {
            midi.messages.collect { msg -> onMidi(msg) }
        }
    }

    private fun onMidi(msg: MidiMessage) {
        _monitor.value = (_monitor.value + msg.pretty()).takeLast(300)

        val target = _learn.value
        if (target != null) {
            // On ignore les relâchements (note off / vel 0) pour ne pas capturer le mauvais event.
            val isRelease = msg.pretty().startsWith("NOTE OFF")
            if (!isRelease) {
                mapping.learn(msg.controlKey(), Binding(target.fn, target.deck, target.pad))
                _learn.value = null
                _mappingVersion.value++
            }
            return
        }
        val action = mapping.resolve(msg)
        // En mode apprentissage, le jog et le crossfader de la platine pilotent le scratch (pas les platines DJ).
        if (trainerActive) {
            if (action is MidiAction.Jog) { onTrainerJog(action.delta); return }
            if (action is MidiAction.Continuous && action.fn == ControlFunction.CROSSFADER) {
                onTrainerCut(action.value); return
            }
        }
        engine.apply(action)
    }

    /** Un mouvement de jog MK3 pendant le trainer : pilote la vitesse de scratch + score les allers-retours. */
    private fun onTrainerJog(delta: Float) {
        lastJogMs = System.currentTimeMillis()
        scratch.setScratchSpeed(delta.toDouble() * SCRATCH_JOG_GAIN)
        val s = if (delta > 0.01f) 1 else if (delta < -0.01f) -1 else 0
        // Pour une technique de cut, le jog ne fait que produire le son : on ne score QUE le crossfader.
        if (!currentUsesCut && s != 0 && jogSign != 0 && s != jogSign) {
            scratch.pluck()
            trainer.registerScratch()
        }
        if (s != 0) jogSign = s
    }

    /** Position du crossfader MK3 pendant le trainer : coupe réellement le son + score les cuts. */
    private fun onTrainerCut(value: Float) {
        // Le crossfader ouvre/ferme la voie : loin du centre (0 ou 1) = coupé pour l'exercice.
        val open = value in 0.25f..0.75f
        scratch.setFader(if (open) 1f else 0f)
        if (currentUsesCut) {
            // Front descendant (ouvert → coupé) = un coup de cut, calé sur le rythme.
            if (!open && !faderClosed) { faderClosed = true; trainer.registerScratch() }
            if (open && faderClosed) faderClosed = false
        }
    }

    /** Cut au doigt (bouton de l'écran) : un coup de crossfader simulé. */
    fun trainerCutTap() {
        if (!currentUsesCut) return
        viewModelScope.launch {
            scratch.setFader(0f)
            trainer.registerScratch()
            delay(70)
            scratch.setFader(1f)
        }
    }

    // --- MIDI ---
    fun connect() = midi.connect()
    fun disconnect() = midi.disconnect()
    fun clearMonitor() { _monitor.value = emptyList() }

    // --- Learn ---
    fun startLearn(fn: ControlFunction, deck: DeckId?, pad: Int = -1) {
        _learn.value = LearnTarget(fn, deck, pad)
    }
    fun cancelLearn() { _learn.value = null }
    fun clearBinding(fn: ControlFunction, deck: DeckId?, pad: Int = -1) {
        mapping.bindingFor(fn, deck, pad)?.let { mapping.clear(it) }
        _mappingVersion.value++
    }
    fun applyPreset() { mapping.applyInpulse200Preset(); _mappingVersion.value++ }
    fun clearAllBindings() { mapping.clearAll(); _mappingVersion.value++ }

    // --- Chargement de morceaux ---
    fun loadTrack(deck: DeckId, uri: Uri, title: String) = engine.loadTrack(deck, uri, title)

    // --- Mode apprentissage ---
    private fun ensureSamples() {
        if (scratchSampleLoaded) return
        val ctx = getApplication<Application>()
        val scratchSample = PcmSample.fromAsset(ctx, SoundBank.defaultScratch.file)
        val breakSample = PcmSample.fromAsset(ctx, SoundBank.defaultBreak.file)
        scratch.setScratchSample(scratchSample)
        scratch.setBreak(breakSample)
        scratchSampleLoaded = true
        _soundsAvailable.value = scratchSample != null || breakSample != null
    }

    /** Niveau débloqué le plus haut pour une technique (1 par défaut). */
    fun unlockedLevel(techId: String): Int = prefs.getInt("unlock_$techId", 1)

    private fun unlock(techId: String, levelIndex: Int) {
        if (levelIndex > unlockedLevel(techId)) prefs.edit().putInt("unlock_$techId", levelIndex).apply()
    }

    /** Prépare un niveau : charge le pattern + cale le break au bon tempo. */
    fun trainerSelect(technique: Technique, level: Level) {
        ensureSamples()
        trainer.load(level)
        scratch.setBreakBpm(SoundBank.defaultBreak.bpm, level.bpm)
        currentUsesCut = technique.usesCut
        faderClosed = false
        scratch.setFader(1f)
    }

    fun trainerStart() {
        ensureSamples()
        scratch.start()
        scratch.setBreakPlaying(true)
        trainerActive = true
        jogSign = 0
        trainer.start()
        // Le jog MK3 n'émet que pendant le mouvement : si plus rien n'arrive, on fige le scratch.
        jogWatchdog?.cancel()
        jogWatchdog = viewModelScope.launch {
            while (trainerActive) {
                if (System.currentTimeMillis() - lastJogMs > 40) {
                    scratch.setScratchSpeed(0.0)
                    jogSign = 0
                }
                delay(20)
            }
        }
    }

    fun trainerStop() {
        trainer.stop()
        jogWatchdog?.cancel()
        jogWatchdog = null
        scratch.setBreakPlaying(false)
        scratch.setScratchSpeed(0.0)
        scratch.stop()
        trainerActive = false
    }

    /** Le jog/tactile pilote la vitesse de scratch (en frames sample / frame sortie). */
    fun scratchScrub(speed: Double) = scratch.setScratchSpeed(speed)

    /** Un aller-retour détecté = une attaque de scratch à scorer. */
    fun scratchStroke() {
        scratch.pluck()
        trainer.registerScratch()
    }

    /** À appeler quand un résultat tombe pour persister la progression. */
    fun onTrainerResult(techId: String, levelIndex: Int) {
        if (trainer.result.value?.passed == true) unlock(techId, levelIndex + 1)
    }

    override fun onCleared() {
        engine.release()
        if (trainerActive) scratch.stop()
        midi.disconnect()
        super.onCleared()
    }
}
