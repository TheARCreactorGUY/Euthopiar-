import urllib.request, json, os
key = os.environ.get("TMDB_API_KEY", "")
for tmdb_id, ep in [("95350", "tv"), ("550", "movie")]:
    u1 = f"https://api.themoviedb.org/3/{ep}/{tmdb_id}/images?api_key={key}"
    u2 = f"https://api.themoviedb.org/3/{ep}/{tmdb_id}/images?api_key={key}&include_image_language=en,null"
    r1 = json.loads(urllib.request.urlopen(u1).read().decode())
    r2 = json.loads(urllib.request.urlopen(u2).read().decode())
    print(ep, tmdb_id, "without:", len(r1.get("logos", [])), "with:", len(r2.get("logos", [])))
    if r2.get("logos"):
        print("  Sample logo:", r2["logos"][0]["file_path"])
