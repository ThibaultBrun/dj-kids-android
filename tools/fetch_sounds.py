#!/usr/bin/env python3
"""Récupère de VRAIS samples CC0 depuis Freesound et les convertit en WAV mono 44.1k
dans app/src/main/assets/sounds/. Écrit les crédits dans assets/sounds/CREDITS.md.

⚠️ Uniquement licence CC0 (publicdomain/zero). Token lu depuis ~/.freesound.env (hors repo).
Usage: python3 tools/fetch_sounds.py
"""
import json, os, re, subprocess, sys, urllib.parse, urllib.request, pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app/src/main/assets/sounds"
TOKEN = None
for line in (pathlib.Path.home() / ".freesound.env").read_text().splitlines():
    if line.startswith("FREESOUND_TOKEN="):
        TOKEN = line.split("=", 1)[1].strip()
assert TOKEN, "FREESOUND_TOKEN manquant dans ~/.freesound.env"

API = "https://freesound.org/apiv2"

# (catégorie, dossier, [requêtes], filtre durée, nb à garder, préfixe fichier)
PLAN = [
    ("scratch", "scratch", ["scratch", "turntable scratch", "vinyl scratch"], "0.3 TO 3.0", 6, "scratch"),
    ("breakbeat", "breakbeat", ["drum break", "breakbeat", "funky drummer break", "boom bap drum loop"], "1.0 TO 12.0", 4, "break"),
    ("fx", "fx", ["air horn", "rewind vinyl", "dj horn"], "0.3 TO 4.0", 3, "fx"),
]

def search(query, dur):
    params = urllib.parse.urlencode({
        "query": query,
        "filter": f'license:"Creative Commons 0" duration:[{dur}]',
        "fields": "id,name,license,username,duration,previews,url",
        "page_size": 15,
        "sort": "score",
        "token": TOKEN,
    })
    with urllib.request.urlopen(f"{API}/search/text/?{params}", timeout=30) as r:
        return json.load(r).get("results", [])

def bpm_from(name):
    m = re.search(r"(\d{2,3})\s*BPM", name, re.I)
    return int(m.group(1)) if m else None

def dl(url, dest):
    req = urllib.request.Request(url, headers={"Authorization": f"Token {TOKEN}"})
    with urllib.request.urlopen(req, timeout=60) as r, open(dest, "wb") as f:
        f.write(r.read())

def main():
    credits = ["# Crédits & licences des sons", "",
               "Tous les fichiers ci-dessous sont **CC0** (domaine public, Freesound). Attribution non requise mais listée par respect.", "",
               "| Fichier | Son | Auteur | Licence | Source |", "|---|---|---|---|---|"]
    manifest = []
    seen = set()
    for cat, folder, queries, dur, keep, prefix in PLAN:
        (ASSETS / folder).mkdir(parents=True, exist_ok=True)
        picked = []
        for q in queries:
            if len(picked) >= keep:
                break
            for res in search(q, dur):
                if len(picked) >= keep:
                    break
                if res["id"] in seen:
                    continue
                if "publicdomain/zero" not in res["license"]:
                    continue
                prev = res.get("previews", {})
                src = prev.get("preview-hq-mp3") or prev.get("preview-lq-mp3")
                if not src:
                    continue
                seen.add(res["id"])
                picked.append((res, src))
        for i, (res, src) in enumerate(picked, 1):
            fname = f"{prefix}{i}.wav"
            mp3 = ASSETS / folder / f".{prefix}{i}.mp3"
            wav = ASSETS / folder / fname
            try:
                dl(src, mp3)
                subprocess.run(["ffmpeg", "-y", "-i", str(mp3), "-ac", "1", "-ar", "44100",
                                str(wav)], check=True, capture_output=True)
                mp3.unlink(missing_ok=True)
            except Exception as e:
                print(f"skip {res['id']}: {e}")
                continue
            bpm = bpm_from(res["name"])
            manifest.append({"cat": cat, "file": f"sounds/{folder}/{fname}", "id": res["id"],
                             "name": res["name"], "bpm": bpm})
            credits.append(f"| {fname} | {res['name'][:40]} | {res['username']} | CC0 | {res['url']} |")
            print(f"OK {cat}/{fname}  ({res['duration']:.1f}s bpm={bpm})  {res['name'][:40]}")
    (ASSETS / "CREDITS.md").write_text("\n".join(credits) + "\n")
    (ASSETS / "manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False))
    print(f"\n{len(manifest)} sons récupérés. Manifeste -> assets/sounds/manifest.json")

if __name__ == "__main__":
    main()
