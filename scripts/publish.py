#!/usr/bin/env python3
"""
Upload AAB to Google Play Store (internal track) via Android Publisher API.
Prerequisites:
  1. App fr.tbrun.djkids must exist in Play Console (create manually first).
  2. SA publisher@pista-498813.iam.gserviceaccount.com granted Release Manager role.
  pip3 install --break-system-packages google-auth google-api-python-client

Usage:
  python3 scripts/publish.py [--track internal|alpha|beta|production]
"""
import argparse
import os
import sys

SA_FILE = os.path.expanduser("~/.secrets/pista-play.json")
AAB_PATH = "app/build/outputs/bundle/release/app-release.aab"
PACKAGE = "fr.tbrun.djkids"
RELEASE_NOTES = {
    "en-US": "Initial release — learn to scratch on your Hercules DJControl Inpulse 200 MK3.",
    "fr-FR": "Première version — apprenez le scratch sur votre Hercules DJControl Inpulse 200 MK3.",
    "es-ES": "Primera versión — aprende scratch con tu Hercules DJControl Inpulse 200 MK3.",
    "de-DE": "Erste Version — lerne Scratch mit deinem Hercules DJControl Inpulse 200 MK3.",
    "it-IT": "Prima versione — impara lo scratch con il tuo Hercules DJControl Inpulse 200 MK3.",
    "pt-BR": "Primeira versão — aprenda scratch com seu Hercules DJControl Inpulse 200 MK3.",
    "nl-NL": "Eerste versie — leer scratch op je Hercules DJControl Inpulse 200 MK3.",
    "ja-JP": "初回リリース — Hercules DJControl Inpulse 200 MK3でスクラッチを学ぼう。",
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--track", default="internal", choices=["internal", "alpha", "beta", "production"])
    args = parser.parse_args()

    try:
        from google.oauth2 import service_account
        from googleapiclient.discovery import build
        from googleapiclient.http import MediaFileUpload
        from googleapiclient.errors import HttpError
    except ImportError:
        print("Missing deps: pip3 install --break-system-packages google-auth google-api-python-client")
        sys.exit(1)

    if not os.path.exists(SA_FILE):
        print(f"Service account not found: {SA_FILE}")
        sys.exit(1)
    if not os.path.exists(AAB_PATH):
        print(f"AAB not found: {AAB_PATH} — run: ./gradlew bundleRelease")
        sys.exit(1)

    creds = service_account.Credentials.from_service_account_file(
        SA_FILE, scopes=["https://www.googleapis.com/auth/androidpublisher"]
    )
    service = build("androidpublisher", "v3", credentials=creds)

    try:
        edit = service.edits().insert(packageName=PACKAGE, body={}).execute()
        edit_id = edit["id"]
        print(f"Edit opened: {edit_id}")

        media = MediaFileUpload(AAB_PATH, mimetype="application/octet-stream", resumable=True)
        bundle = service.edits().bundles().upload(
            packageName=PACKAGE, editId=edit_id, media_body=media
        ).execute()
        version_code = bundle["versionCode"]
        print(f"AAB uploaded — versionCode: {version_code}")

        release_notes_list = [{"language": lang, "text": text} for lang, text in RELEASE_NOTES.items()]
        service.edits().tracks().update(
            packageName=PACKAGE,
            editId=edit_id,
            track=args.track,
            body={
                "releases": [{
                    "versionCodes": [version_code],
                    "status": "completed",
                    "releaseNotes": release_notes_list,
                }]
            },
        ).execute()
        print(f"Track '{args.track}' updated.")

        service.edits().commit(packageName=PACKAGE, editId=edit_id).execute()
        print("Edit committed. Done!")
        print(f"Check: https://play.google.com/console/u/0/developers/app/{PACKAGE}")

    except HttpError as e:
        print(f"API error {e.status_code}: {e.reason}")
        if "not found" in str(e).lower():
            print("→ Create the app manually at https://play.google.com/console then re-run.")
        sys.exit(1)


if __name__ == "__main__":
    main()
