# 🎧 DJ Kids (Android) — Hercules DJControl Inpulse 200 MK3

Appli DJ **native Android** pour enfant, pilotée par la **Hercules DJControl Inpulse 200 MK3**
branchée en USB (OTG). Kotlin + Jetpack Compose, audio via **media3/ExoPlayer**, MIDI via l'API
native `android.media.midi` (pas de driver Hercules nécessaire : la MK3 est un contrôleur
USB-MIDI class-compliant).

L'appli est **jouable au doigt** (deux platines tactiles complètes) **et** au contrôleur. Comme la
MK3 est trop récente pour avoir un mapping public, les codes MIDI se **capturent / s'apprennent**
directement dans l'appli (écran *Monitor* + *MIDI Learn*).

## Ce qu'elle fait

### Mode Mix (2 platines)
- **2 platines** : charger un morceau du téléphone (n'importe quel audio), play/pause, **cue**.
- **8 pads** par platine = **hot-cues** (1er appui = pose le point, appuis suivants = saut + lecture).
- **Mixer** : crossfader équal-power, volume par voie, **volume master**.
- **EQ 3 bandes** par platine (grave/médium/aigu) avec **kill** — vrai DSP biquad (RBJ cookbook)
  inséré dans la chaîne audio d'ExoPlayer.
- **Pitch / tempo** façon vinyle (la vitesse change aussi la hauteur, ±50 %).
- **Sync** simple (aligne le tempo d'une platine sur l'autre).
- **Jog** : nudge (recalage temporel). Le scratch basse latence (Oboe/NDK) reste la phase 4.
- **LED feedback** : le moteur MIDI sait renvoyer des notes au contrôleur (pads/VU) — à câbler
  une fois le mapping confirmé.

### Mode Apprentissage (Trainer)
Curriculum progressif de scratch articulé en **techniques × 5 niveaux** :
- **Chapitre 1 — Baby scratch** : noires → croches → doubles croches → rythmes mixtes.
- Chapitres suivants : forward, chirp, tear, scribble, transformer, flare, orbit…

À chaque session : un breakbeat tourne en boucle, une **grille rythmique Canvas** affiche les temps
exactement quand scratcher, et le jog tactile (ou la vraie platine MK3) est scoré en précision de
timing (Perfect / Good / Miss). Les niveaux se débloquent progressivement.

Un **disque vinyle Canvas animé** (sillons, étiquette, marqueur) + un **arc de vitesse** lumineux
(Lime → Pink) donnent un retour visuel immédiat de la direction et de l'intensité du scratch.

## Écrans
1. **Mix** — les deux platines + le mixer central (jouable sans contrôleur).
2. **Learn** — curriculum de scratch : sélection technique/niveau → session d'entraînement rythmique.
3. **Mapping** — assigne chaque fonction à un contrôle physique (MIDI Learn), ou applique le
   preset "devinette Inpulse 200" puis corrige. Les assignations sont **persistées**.
4. **Monitor** — affiche chaque message MIDI reçu (type/canal/numéro/valeur) pour vérifier les codes.

## Architecture
```
app/src/main/java/fr/tbrun/djkids/
├── MainActivity.kt            # navigation 4 onglets (Mix / Learn / Mapping / Monitor)
├── midi/
│   ├── MidiEngine.kt          # connexion USB-MIDI (in + out LED), décodage
│   ├── MidiMessage.kt         # message décodé + clé de contrôle stable
│   ├── MidiMapping.kt         # mapping persistant (SharedPreferences/JSON) + learn + preset
│   └── MidiAction.kt          # fonctions DJ + action résolue
├── audio/
│   ├── DjEngine.kt            # 2 platines + crossfader + master + routage des actions
│   ├── Deck.kt                # ExoPlayer : play/cue/tempo/volume/hot-cues + EQ injecté
│   ├── Biquad.kt              # filtres biquad (shelf/peaking)
│   ├── SoundBank.kt           # manifeste samples CC0 bundlés (breakbeats / scratch / FX)
│   └── ThreeBandEqProcessor.kt# AudioProcessor media3 (EQ 3 bandes 16-bit)
├── game/
│   ├── Curriculum.kt          # techniques × 5 niveaux, unlock progressif
│   ├── ScratchPattern.kt      # patterns rythmiques (NoteValue, RhythmProfile)
│   └── TrainerEngine.kt       # moteur scoring timing (Perfect/Good/Miss)
├── ui/
│   ├── DjViewModel.kt         # câble MIDI → mapping → audio ; learn ; monitor
│   ├── theme/                 # couleurs néon + thème sombre
│   ├── components/            # Knob, faders, pads, platine animée, boutons
│   └── screens/               # DjScreen, TrainerScreen, MappingScreen, MonitorScreen
```

## Build
Pas de dépendance exotique : projet Gradle standard.

- **Android Studio** : ouvrir le dossier, laisser Gradle se synchroniser, Run sur l'appareil.
- **Codemagic** (voir `codemagic.yaml`) :
  - `android-debug` : APK debug à la demande (sideload sur le tél de Charly).
  - `android-release` : AAB signé au push sur `main` (variables de signature dans le groupe
    `djkids_signing` : `DJKIDS_KEYSTORE_B64`, `DJKIDS_KEYSTORE_PASSWORD`, `DJKIDS_KEY_ALIAS`,
    `DJKIDS_KEY_PASSWORD`).

Config : `applicationId = fr.tbrun.djkids`, `minSdk 26`, `compileSdk/targetSdk 35`, Kotlin 2.0,
Compose BOM 2024.12, media3 1.5.1.

## Prochaine étape
1. Builder l'APK debug, l'installer, brancher la MK3 en USB-OTG.
2. Onglet **Monitor** → presser chaque contrôle pour relever les codes (ou **Mapping** → Learn).
3. Vérifier/compléter le mapping ; envoyer les codes MIDI pour figer le preset exact.
4. **Play Store** : créer `fr.tbrun.djkids` dans [Play Console](https://play.google.com/console),
   accorder le rôle "Release Manager" au SA `publisher@pista-498813.iam.gserviceaccount.com`,
   puis lancer `python3 scripts/publish.py --track internal`.
5. Phase 4 : scratch basse latence (Oboe/NDK), filtre balayage DSP, feedback LED, BPM/sync réel.

## Permissions
`android.media.midi` ne demande aucune permission runtime. La lecture des morceaux passe par le
sélecteur système (SAF) → pas de permission stockage non plus.
