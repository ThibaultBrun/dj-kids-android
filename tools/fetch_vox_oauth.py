#!/usr/bin/env python3
"""Échange le code OAuth2 Freesound -> access_token, puis télécharge des voix de scratch
en ORIGINAL (lossless), les convertit en WAV mono 44.1k dans assets/sounds/scratch/.
Usage: python3 tools/fetch_vox_oauth.py <AUTH_CODE>
"""
import json,sys,subprocess,urllib.parse,urllib.request,pathlib
env=dict(l.strip().split("=",1) for l in open(pathlib.Path.home()/".freesound.env") if "=" in l)
CID=env["FREESOUND_CLIENT_ID"]; SECRET=env["FREESOUND_TOKEN"]
API="https://freesound.org/apiv2"
DST=pathlib.Path.home()/"repos/dj-kids-android/app/src/main/assets/sounds/scratch"

def token_from_code(code):
    data=urllib.parse.urlencode({"client_id":CID,"client_secret":SECRET,
        "grant_type":"authorization_code","code":code}).encode()
    r=urllib.request.urlopen(f"{API}/oauth2/access_token/",data=data,timeout=30)
    return json.load(r)

def search(q):
    p=urllib.parse.urlencode({"query":q,"filter":'license:"Creative Commons 0" duration:[0.2 TO 3.0] samplerate:[44100 TO 192000]',
      "fields":"id,name,username,url,num_downloads","page_size":15,"sort":"downloads_desc","token":SECRET})
    return json.load(urllib.request.urlopen(f"{API}/search/text/?{p}",timeout=30)).get("results",[])

def download_original(sid,access,dest):
    req=urllib.request.Request(f"{API}/sounds/{sid}/download/",headers={"Authorization":f"Bearer {access}"})
    open(dest,"wb").write(urllib.request.urlopen(req,timeout=120).read())

def main():
    code=sys.argv[1]
    tok=token_from_code(code); access=tok["access_token"]
    json.dump(tok, open(pathlib.Path.home()/".freesound-oauth.json","w"))
    print("access_token OK")
    queries=["dj scratch vocal","acapella hey vocal","rap vocal yeah","vocal shout","ah yeah vocal","hip hop vocal sample","fresh vocal","funk vocal stab"]
    seen=set();picked=[]
    for q in queries:
        for r in search(q):
            if len(picked)>=8: break
            if r["id"] in seen: continue
            seen.add(r["id"]); picked.append(r)
        if len(picked)>=8: break
    for f in DST.glob("*"):
        if f.suffix in (".mp3",".ogg",".wav"): f.unlink()
    rows=[]
    for i,r in enumerate(picked,1):
        raw=DST/f".orig{i}"; wav=DST/f"scratch{i}.wav"
        try:
            download_original(r["id"],access,raw)
            subprocess.run(["ffmpeg","-y","-i",str(raw),"-ac","1","-ar","44100",str(wav)],check=True,capture_output=True)
            raw.unlink(missing_ok=True)
        except Exception as e:
            print("skip",r["id"],e); continue
        rows.append((f"scratch{i}.wav",r["name"][:34],r["username"],r["url"]))
        print("OK",wav.name,"orig lossless |",r["name"][:34])
    print("DONE",len(rows))
    for row in rows: print(row)

if __name__=="__main__": main()
