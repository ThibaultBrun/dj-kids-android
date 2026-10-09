# DJ Kids — État d'avancement (autonomie jusqu'à dim 2026-10-11 11:00)

## Contexte machine
- Repo : `~/repos/dj-kids-android` (remote GitHub `ThibaultBrun/dj-kids-android`, branche `main`).
- SDK Android : `export ANDROID_HOME=/home/debian/android-sdk` (JDK 21 OK).
- ⚠️ `dl.google.com` renvoie 404 depuis le VPS → Maven passe par le mirror gvt1 avec `isAllowInsecureProtocol=true` (voir `settings.gradle.kts`).
- Build détaché : `cd ~/repos/dj-kids-android && export ANDROID_HOME=/home/debian/android-sdk && nohup ./gradlew :app:assembleDebug --no-daemon > build.log 2>&1 &`
- Lire le résultat : `tail -n 60 build.log` (chercher `BUILD SUCCESSFUL` / `BUILD FAILED` / `e: ...`).

## Boucle d'autonomie
- Cron `dj-kids-autobuild` (sessionTarget main) réveille cette session toutes les 20 min.
- À chaque réveil : lire ce fichier → vérifier build.log → corriger erreurs → relancer build → avancer roadmap → commit (SANS trailer Co-Authored-By, dates 22h-01h) + push → MAJ ce fichier → point Discord #dj-kids.
- STOP + supprimer le cron si on dépasse dim 2026-10-11 11:00.

## 🎯 OBJECTIF : publication Play Store (tous les possesseurs de MK3), pas juste Charly
Donc : qualité production, mapping MK3 réel qui marche, branding, fiche store, conformité.

## Roadmap
1. [x] Phase 1 — MIDI monitor
2. [x] Phase 2/3 — 2 platines + mixer + crossfader + EQ 3 bandes + pitch/tempo + MIDI learn/mapping
3. [x] APK debug compile ✅ (app-debug.apk 19 Mo)
4. [x] Phase 3b — TrainerScreen : 7 techniques x5 niveaux, autoroute Canvas, platine tactile, scoring, unlock progressif (commit 18f6828, BUILD SUCCESSFUL 2026-10-10 23:45)
5. [ ] Phase 4 — scratch basse latence Oboe (NDK/JNI) — AudioTrack LOW_LATENCY actuel OK pour le trainer
6. [ ] Preset mapping MK3 **validé sur hardware réel** (bloquant publication — attendre les codes MIDI de Thibault via l'écran Monitor)
### 🎚️ Banque de sons + Mode Apprentissage du scratch (demande Thibault 2026-10-10)
- **Banque de sons intégrée** : samples de scratch classiques (vocal "Ahh"/"Fresh"), breakbeats, FX. Bundlés dans `app/src/main/assets/` (⚠️ licences CC0/royalty-free obligatoires pour le Play Store). Modèle = `audio/SoundBank.kt`.
- **Mode "jeu" = apprentissage du scratch** (PAS DJ Hero). Trainer rythmique : breakbeat en boucle + métronome + pattern visuel des temps où scratcher, scoring de la précision (timing du jog).
- **Progression** (modèle = `game/Curriculum.kt`) :
  - Chapitre 1 — **Baby scratch** sur un breakbeat :
    - Niv 1 : que des **noires** (1/temps)
    - Niv 2 : **croches** (2/temps)
    - Niv 3 : **noires + croches** mélangées
    - Niv 4 : **doubles croches** (4/temps)
    - Niv 5 : **tout** (mix libre)
  - Chapitres suivants : forward, chirp, tear, scribble, transformer, flare, orbit… (1 technique/chapitre, même échelle de 5 niveaux).
- Dépend du moteur scratch basse latence (Oboe, phase 4) pour le ressenti ; le trainer rythmique peut démarrer en phase 3 (jog = scrub position/pitch).

### Chantier publication Play Store
6. [ ] Nom store sans violer la marque Hercules/Inpulse (app = "DJ Kids" ; usage nominatif "compatible Hercules DJControl Inpulse 200 MK3" OK en description, pas dans le titre)
7. [ ] Icône + feature graphic + captures (DA soignée, pas générique IA)
8. [ ] Politique de confidentialité hébergée (app offline → policy simple) : `djkids.tbrun.dev/privacy`
9. [ ] Keystore release + signing ; AAB release (local ou Codemagic)
10. [ ] Fiche + upload via API Android Publisher (SA `pista-play.json` — vérifier accès au compte Play Console ; sinon créer l'app dans la console)
11. [ ] Déclaration permissions (USB host, lecture audio/média), content rating, data safety (aucune donnée collectée)

## Dernier statut
- 2026-10-10 23:42 : assets audio ajoutés — 8 WAV synthétiques CC0 générés par Python (3 breakbeats 70/85/90 BPM, 3 scratch vocaux Ahh/Fresh/Aah yeah, 2 FX airhorn/rewind). BUILD SUCCESSFUL 12s. Commit 8e74b22 pushé sur main.
- Prochaine priorité : intégrer ScratchEngine dans TrainerScreen (charger les WAV via PcmSample.fromAsset, démarrer engine, piloter scratchSpeed depuis le drag jog) + tester la boucle breakbeat en arrière-plan pendant le scoring.
