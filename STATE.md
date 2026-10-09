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
2. [~] Phase 2/3 — 2 platines + mixer + crossfader + EQ 3 bandes + pitch/tempo + MIDI learn/mapping (code écrit, commit 950f3de)
3. [x] APK debug compile ✅ (app-debug.apk 19 Mo, BUILD SUCCESSFUL 2026-10-10 01:07)
4. [ ] Phase 4 — scratch basse latence (Oboe) + 8 pads + sync affinés
5. [ ] Preset mapping MK3 **validé sur hardware réel** (bloquant publication — attendre les codes MIDI de Thibault via l'écran Monitor)
### Chantier publication Play Store
6. [ ] Nom store sans violer la marque Hercules/Inpulse (app = "DJ Kids" ; usage nominatif "compatible Hercules DJControl Inpulse 200 MK3" OK en description, pas dans le titre)
7. [ ] Icône + feature graphic + captures (DA soignée, pas générique IA)
8. [ ] Politique de confidentialité hébergée (app offline → policy simple) : `djkids.tbrun.dev/privacy`
9. [ ] Keystore release + signing ; AAB release (local ou Codemagic)
10. [ ] Fiche + upload via API Android Publisher (SA `pista-play.json` — vérifier accès au compte Play Console ; sinon créer l'app dans la console)
11. [ ] Déclaration permissions (USB host, lecture audio/média), content rating, data safety (aucune donnée collectée)

## Dernier statut
- 2026-10-10 ~01:05 : Maven OK. Build compilait → 2 erreurs Kotlin (OpenDocument sans `()`, import `lazy.item` invalide) corrigées. Build debug relancé en détaché.
- Nouvel objectif confirmé par Thibault : **publier sur le Play Store** pour tous les possesseurs de MK3.
- Décision backend : **app 100% locale/offline** (lecture audio du téléphone + MIDI USB). Pas d'auth ni d'API. Domaine `djkids.tbrun.dev` servira juste la page privacy (exigée par Play) et le site vitrine.
