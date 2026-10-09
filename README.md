# 🎧 DJ Kids (Android) — Hercules Inpulse 200 MK3

Appli DJ **native Android** pour enfant, pilotée par la **Hercules DJControl Inpulse 200 MK3**
branchée en USB (OTG). Kotlin + Jetpack Compose, audio via media3/ExoPlayer (Oboe en phase 2
pour le scratch), MIDI via l'API native `android.media.midi`.

## Pourquoi "from scratch"
La MK3 est un contrôleur **USB-MIDI standard** : Android la voit nativement, pas besoin de
driver Hercules. Aucune appli Android ne la mappe encore (trop récente) → on fait la nôtre.

## Feuille de route
1. **Phase 1 — MIDI monitor** (ce commit) : l'appli se connecte à la MK3 et **affiche chaque
   message MIDI** (pads, jogs, faders, boutons). → on capture le mapping réel du contrôleur.
2. **Phase 2 — 1 platine** : charger un morceau du téléphone, play/cue, piloté par la MK3.
3. **Phase 3 — 2 platines** : crossfader, volumes, EQ 3 bandes, pitch/tempo, jog (nudge).
4. **Phase 4 — jog scratch basse latence** (Oboe/NDK) + 8 pads (hot cues / samples) + sync.

## Démarrage (Phase 1)
1. Android Studio → **New Project → Empty Activity (Compose)**.
   - Package : `fr.tbrun.djkids` · minSdk **26** · Kotlin · Compose.
2. Dépose les fichiers de `app-src/` dans `app/src/main/java/fr/tbrun/djkids/`.
3. Branche la MK3 en USB-OTG, lance l'appli, tape **Connecter**.
4. Presse chaque pad / tourne chaque jog / bouge chaque fader → note les codes affichés
   et envoie-les moi : je génère le mapping complet pour les phases suivantes.

> Build auto possible via **Codemagic** (même principe que l'app Pista) — config à ajouter en phase 2.

## Permissions
`android.media.midi` ne nécessite pas de permission spéciale ; l'accès USB-MIDI est géré par
le système (une autorisation USB peut être demandée au branchement).
