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

# (catégorie, dossier, [requêtes], filtre durée, nb à garder, préfixe fichier, tri)
# scratch = VOIX BRUTES à scratcher soi-même (pas déjà scratchées) ; breakbeat = longs (minutes).
PLAN = [
    ("scratch", "scratch",
     ["woo vocal", "yeah vocal shout", "hey vocal", "ahh vocal", "vocal stab", "ho vocal shout", "come on vocal", "uh vocal"],
     "0.2 TO 2.5", 8, "scratch", "score"),
    ("breakbeat", "breakbeat",
     ["hip hop instrumental beat", "boom bap instrumental", "funky drummer", "old school hip hop beat", "funk breakbeat"],
     "40 TO 600", 5, "break", "duration_desc"),
    ("fx", "fx", ["air horn", "rewind vinyl", "siren"], "0.3 TO 5.0", 3, "fx", "score"),
]

def search(query, dur, sort="score"):
    params = urllib.parse.urlencode({
        "query": query,
        "filter": f'license:"Creative Commons 0" duration:[{dur}]',
        "fields": "id,name,license,username,duration,previews,url",
        "page_size": 15,
        "sort": sort,
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
    for cat, folder, queries, dur, keep, prefix, sort in PLAN:
        (ASSETS / folder).mkdir(parents=True, exist_ok=True)
        # nettoie l'ancien lot de la catégorie
        for old in (ASSETS / folder).glob("*.wav"):
            old.unlink()
        picked = []
        for q in queries:
            if len(picked) >= keep:
                break
            for res in search(q, dur, sort):
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
            fname = f"{prefix}{i}.mp3"
            dst = ASSETS / folder / fname
            try:
                dl(src, dst)  # on garde le mp3 (léger) ; le décodeur de l'app lit le mp3
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
