# Play Store — Checklist de publication

## Actions manuelles requises (Thibault)

### 1. Créer l'app dans Play Console
- URL : https://play.google.com/console
- Package : `fr.tbrun.djkids`
- Accorder le rôle **Release Manager** au SA : `publisher@pista-498813.iam.gserviceaccount.com`
- Ensuite : `python3 scripts/publish.py --track internal`

### 2. Content Rating (IARC)
Remplir le questionnaire en console. Réponses pour DJ Kids :

| Question | Réponse |
|----------|---------|
| Violence | Non |
| Sexualité | Non |
| Langage offensant | Non |
| Substances contrôlées | Non |
| Jeux d'argent | Non |
| Contenu généré par l'utilisateur | Non |
| Partage de localisation | Non |
| Achats intégrés | Non |

→ Résultat attendu : **EVERYONE** (US) / **3+** (EU PEGI)

### 3. Data Safety
L'app ne collecte **aucune donnée** :
- Pas d'internet (offline)
- Pas de compte utilisateur
- Pas d'analytics
- Pas de localisation
- USB host uniquement (hardware control)

Remplir dans Play Console → Data safety :
- "Does your app collect or share any of the required user data types?" → **No**
- "Is all of the user data collected by your app encrypted in transit?" → N/A
- "Do you provide a way for users to request that their data is deleted?" → N/A

Lien politique de confidentialité : `https://djkids.tbrun.dev/privacy`

### 4. Screenshots (à créer)
Requis : 2–8 screenshots phone (16:9 ou 9:16, min 320px)
- `store/screenshots/phone/01-trainer.png` — TrainerScreen en action
- `store/screenshots/phone/02-chapter-picker.png` — ChapterPicker avec badges
- `store/screenshots/phone/03-level-picker.png` — LevelPicker 5 niveaux
- `store/screenshots/phone/04-deck-view.png` — Vue platines dual deck

→ Capturer sur device ou émulateur (adb screencap)

### 5. Feature Graphic
Déjà généré : `store/feature_graphic.png` (1024×500)

### 6. Icône
Déjà dans APK : `app/src/main/res/mipmap-*/ic_launcher.png`
Play Console requiert un PNG 512×512 — extraire depuis le APK ou rebuilder.

## Permissions à déclarer
- `android.permission.USB_PERMISSION` — Connexion hardware MIDI (Hercules MK3)
- Pas d'autres permissions sensibles

## Déclaration catégorie
- Catégorie : **Music & Audio**
- Tags : DJ, Scratch, Learn, Kids

## Langues store disponibles
- en-US ✅, fr-FR ✅, es-ES ✅, de-DE ✅, it-IT ✅, pt-BR ✅, nl-NL ✅, ja-JP ✅
