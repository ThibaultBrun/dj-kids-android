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

## Roadmap
1. [x] Phase 1 — MIDI monitor
2. [~] Phase 2/3 — 2 platines + mixer + crossfader + EQ 3 bandes + pitch/tempo + MIDI learn/mapping (code écrit, commit 950f3de)
3. [ ] Faire compiler l'APK debug (blocage résolu = accès Maven ; build en cours)
4. [ ] Phase 4 — scratch basse latence (Oboe) + 8 pads + sync affinés
5. [ ] Icône/branding + README d'install + APK release signé (Codemagic)

## Dernier statut
- 2026-10-10 ~01:00 : accès Maven débloqué (`isAllowInsecureProtocol`). Build debug relancé en détaché. En attente du résultat du compilateur.
- Décision backend : **app 100% locale/offline** (lecture audio du téléphone + MIDI USB). Pas d'auth ni d'API pour l'usage de Charly. Si un jour on veut une bibliothèque cloud ou sync réglages → `api.djkids.tbrun.dev` (à faire seulement si une feature l'exige).
