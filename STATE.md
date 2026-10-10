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
9. [x] Keystore release + signing ; AAB release ✅ — `~/djkids-release.jks` (alias djkids-release, 10000j) ; creds dans `~/.djkids-keystore.env` (chmod 600) ; AAB 13Mo dans `app/build/outputs/bundle/release/app-release.aab`
10. [ ] Fiche + upload via API Android Publisher (SA `pista-play.json` — vérifier accès au compte Play Console ; sinon créer l'app dans la console)
11. [ ] Déclaration permissions (USB host, lecture audio/média), content rating, data safety (aucune donnée collectée)

## Dernier statut
- 2026-10-10 07:20 : Store listings **es-ES** + **de-DE** créés (titre/court/long/feature_graphic) + publish.py RELEASE_NOTES 4 langues. Commit c0ba418 pushé. Build précédent : BUILD SUCCESSFUL 18s.
- 2026-10-10 06:42 : Curriculum : ajout technique **Orbit** (🪐) — chapitre 9, BPM 82, usesCut, "2 clics de crossfader aller-retour". BUILD SUCCESSFUL 18s. Commit 5114d9c pushé.
- 2026-10-10 06:22 : UX no-audio — `soundsAvailable: StateFlow<Boolean>` exposé dans DjViewModel ; bannière "Mode démo · pas d'audio" dans ChapterPicker quand samples manquants. BUILD SUCCESSFUL 24s. Commit pushé.
- 2026-10-10 06:15 : Fix bug Compose : `onTrainerResult` déplacé dans `LaunchedEffect(result)` (ne s'exécute plus à chaque recomposition). ChapterPicker enrichi : badge de progression par technique (N2/5…, ✓ si complété, border Violet→Cyan→Lime). BUILD SUCCESSFUL 23s. Commit 318353b pushé.
- 2026-10-10 05:42 : README mis à jour (Mode Apprentissage documenté, architecture `game/` ajoutée, prochaine étape Play Store clarifiée). BUILD SUCCESSFUL (précédent build). Commit pushé.
- 2026-10-10 05:10 : Feature graphic 1024×500 créé (SVG → PNG via Chromium headless) + icône tonearm améliorée (bras de lecture + pivot + cartridge rose, sillons multiples). BUILD SUCCESSFUL 15s. Commit dc8bcf8 pushé. Prochaine étape bloquée : création manuelle de l'app en Play Console par Thibault.
- 2026-10-10 04:42 : Play Store API testée → `fr.tbrun.djkids` n'existe pas encore en Play Console (**création manuelle requise**). Script `scripts/publish.py` créé + métadonnées store (`store/en-US/` et `store/fr-FR/` : title, short_description, full_description). Pas de build lancé (gradle propre, disque 96%).
- 2026-10-10 03:55 : `TechniqueAnimation` ajoutée dans LevelPicker — Canvas animé 68dp : jog → vinyle + flèche directionnelle Cyan/Pink pulsante ; cut → crossfader + ondes Violet. BUILD SUCCESSFUL 21s. Commit 7d53382 pushé.
- 2026-10-11 03:25 : Keystore release généré (`~/djkids-release.jks`) + AAB signé 13Mo (`bundleRelease` BUILD SUCCESSFUL 1m50s). Privacy policy déployée sur https://djkids.tbrun.dev/privacy (Caddy + DNS OK).
- 2026-10-11 03:15 : LevelPicker — composable `RhythmPreview` (dots Canvas : gros point = temps fort, petit = subdivision, actif/grisé selon locked) + `docs/privacy.html` (politique de confidentialité prête pour djkids.tbrun.dev/privacy). BUILD SUCCESSFUL 29s. Commit 4233231 pushé.
- 2026-10-11 02:42 : TrainerScreen UX — BPM affiché à côté du combo pendant la session + arc de vitesse lumineux sur le vinyle (Lime→Pink selon intensité, sens = direction du jog). BUILD SUCCESSFUL 25s. Commit cb3d4c9 pushé sur main.
- 2026-10-10 22:30 : ScratchPad → disque vinyle Canvas animé (sillons, étiquette Violet, marqueur rotatif LaunchedEffect). Commit 5df3ef1.

## Prochaine priorité
- **Chantier Play Store** : ⚠️ **action manuelle Thibault requise** — créer l'app `fr.tbrun.djkids` dans https://play.google.com/console, puis accorder le rôle "Release Manager" au SA `publisher@pista-498813.iam.gserviceaccount.com`. Ensuite : `python3 scripts/publish.py --track internal`.
- Métadonnées store prêtes dans `store/en-US/` et `store/fr-FR/`.
- **Icône + feature graphic** : DA soignée, pas générique IA.
- **Phase 4 Oboe** : NDK/JNI basse latence scratch. Nécessite NDK (⚠️ disque à 95%, NDK ~1.5Go — libérer de l'espace d'abord). NDK 26.3 recommandé.
- [x] Trainer UX : description animée de chaque technique scratch — `TechniqueAnimation` done (commit 7d53382).
- Codemagic : configurer groupe `djkids_signing` dans UI Codemagic (DJKIDS_KEYSTORE_B64/PASSWORD/KEY_ALIAS/KEY_PASSWORD) — manuel, pas automatisable.
