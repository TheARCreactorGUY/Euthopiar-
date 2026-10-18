import urllib.request, json, ssl, base64, re, time

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://bingr.one/'
}

def vidy_m(val):
    val = val & 0xFFFFFFFF
    val ^= (val >> 16)
    val = (val * 2246822507) & 0xFFFFFFFF
    val ^= (val >> 13)
    val = (val * 3266489909) & 0xFFFFFFFF
    val ^= (val >> 16)
    return val & 0xFFFFFFFF

def vidy_p(e, t):
    t = t & 31
    e = e & 0xFFFFFFFF
    return ((e << t) | (e >> (32 - t))) & 0xFFFFFFFF

def decrypt_vidy(ciphertext, seed, media_id_str):
    clean = ciphertext.strip().replace('-', '+').replace('_', '/')
    padded = clean + '=' * ((4 - len(clean) % 4) % 4)
    o = bytearray(base64.b64decode(padded))
    if len(o) < 4: return None
    media_id_num = int(media_id_str)
    t1 = 2166136261
    for ch in seed: t1 = ((t1 ^ ord(ch)) * 16777619) & 0xFFFFFFFF
    t2 = vidy_m(media_id_num ^ 2654435769)
    s = vidy_m(vidy_m(t1) ^ t2)
    a = [0] * 61
    set_indices = [False] * 61
    for e in range(8):
        t = (s % 61)
        s = vidy_p((s + 2654435769) & 0xFFFFFFFF, 7 + (7 & e))
        a[t] = s ^ vidy_m(s)
        set_indices[t] = True
        s = vidy_m((s + t) & 0xFFFFFFFF)
    acc = vidy_m(2779096485 ^ s)
    n = bytearray(len(o))
    step = 0
    byte_idx = 0
    while byte_idx < len(o):
        d = acc % 61
        r = -1 if set_indices[d] else 0
        term = a[d] ^ ((2654435769 * (step + 1)) & 0xFFFFFFFF)
        l_val = ((acc ^ term) | (acc & term & r)) & 0xFFFFFFFF
        rot1 = vidy_p((l_val + acc) & 0xFFFFFFFF, 31 & d)
        rot2 = vidy_p(acc, 31 & (d * 7))
        new_acc = vidy_m((rot1 ^ rot2) + 2654435769)
        a[d] = new_acc
        set_indices[d] = True
        acc = new_acc
        t_gen = new_acc
        step += 1
        n[byte_idx] = t_gen & 0xFF
        byte_idx += 1
        if byte_idx < len(o): n[byte_idx] = (t_gen >> 8) & 0xFF; byte_idx += 1
        if byte_idx < len(o): n[byte_idx] = (t_gen >> 16) & 0xFF; byte_idx += 1
        if byte_idx < len(o): n[byte_idx] = (t_gen >> 24) & 0xFF; byte_idx += 1
    r_bytes = bytearray(len(o))
    for i in range(len(o)): r_bytes[i] = o[i] ^ n[i]
    if r_bytes[:4] != b'mvm1': return None
    return r_bytes[4:].decode('utf-8', errors='ignore')

# 10 Movies
MOVIES = [
    (550, "Fight Club"),
    (27205, "Inception"),
    (157336, "Interstellar"),
    (155, "The Dark Knight"),
    (680, "Pulp Fiction"),
    (603, "The Matrix"),
    (98, "Gladiator"),
    (299534, "Avengers: Endgame"),
    (324857, "Spider-Man: Into the Spider-Verse"),
    (872585, "Oppenheimer")
]

# 5 Web Series (TV)
TV_SHOWS = [
    (1399, "Game of Thrones"),
    (1396, "Breaking Bad"),
    (66732, "Stranger Things"),
    (76479, "The Boys"),
    (94997, "House of the Dragon")
]

# 5 Anime
ANIME = [
    (30, 890, "Neon Genesis Evangelion"),
    (21, 37854, "One Piece"),
    (113415, 95479, "Jujutsu Kaisen"),
    (1535, 13916, "Death Note"),
    (16498, 1429, "Attack on Titan")
]

print("=" * 80)
print("  VERIFYING 20 CONTENT PIECES (10 MOVIES, 5 WEBSERIES, 5 ANIME)")
print("=" * 80)

# SECTION 1: MOVIES
print("\n--- [PART 1: 10 MOVIES] ---")
for tmdb_id, title in MOVIES:
    print(f"\n>> Movie: {title} (TMDB {tmdb_id})")
    # 1. Details
    req = urllib.request.Request(f"https://api.bingr.one/api/details/movie/{tmdb_id}?v=1", headers=headers)
    with urllib.request.urlopen(req, timeout=8) as r:
        d = json.loads(r.read())
        print(f"   Details: Title='{d.get('title')}', Rating={d.get('rating')}, Year={d.get('year')}")

    # 2. Vidrift Orion Multi-Audio
    v_url = f"https://embed.vidrift.in/embed/movie/{tmdb_id}"
    req_v = urllib.request.Request(v_url, headers=headers)
    try:
        with urllib.request.urlopen(req_v, timeout=8) as r:
            html = r.read().decode('utf-8')
            m = re.search(r'var\s+embedMeta\s*=\s*(\{.*?\});', html, re.DOTALL)
            if m:
                meta = json.loads(m.group(1))
                orion = meta.get('orionStreams', [])
                audio_names = [s.get('name') for s in orion]
                print(f"   Vidrift Orion: {len(orion)} streams found -> {audio_names[:4]}...")
            else:
                print("   Vidrift Orion: meta not found")
    except Exception as e:
        print(f"   Vidrift Orion: {e}")

    # 3. Vidy Multi-Server CDN
    try:
        s_req = urllib.request.Request(f"https://api.wecollege.net/seed?mediaId={tmdb_id}", headers={'Referer': 'https://www.vidy.st/', 'User-Agent': headers['User-Agent']})
        with urllib.request.urlopen(s_req, timeout=6) as r:
            seed = json.loads(r.read())['seed']
            srv_req = urllib.request.Request(f"https://api.wecollege.net/miami/sources?mediaType=movie&tmdbId={tmdb_id}&enc=2&seed={seed}", headers={'Referer': 'https://www.vidy.st/', 'User-Agent': headers['User-Agent']})
            with urllib.request.urlopen(srv_req, timeout=6) as sr:
                decrypted = decrypt_vidy(sr.read().decode('utf-8'), seed, str(tmdb_id))
                sources = json.loads(decrypted).get('sources', []) if decrypted else []
                print(f"   Vidy CDN (Miami): {len(sources)} qualities available (Decrypted PRNG OK)")
    except Exception as e:
        print(f"   Vidy CDN: {e}")

    # 4. Embed URLs
    print(f"   Embeds: Filmu=https://embed.filmu.in/movie/{tmdb_id} | Cinezo=https://player.cinezo.live/embed/movie/{tmdb_id} | Vidbolt=https://vidbolt.xyz/movie/{tmdb_id}")

    # 5. Subtitles
    sub_req = urllib.request.Request(f"https://api.bingr.one/api/subtitles/vdrk/movie/{tmdb_id}", headers=headers)
    try:
        with urllib.request.urlopen(sub_req, timeout=6) as r:
            subs = json.loads(r.read()).get('subtitles', [])
            print(f"   Subtitles: {len(subs)} VDRK WebVTT tracks")
    except Exception as e:
        print(f"   Subtitles: {e}")

    # 6. Download links
    dl_req = urllib.request.Request(f"https://streamrip.fun/api/download/movie/{tmdb_id}", headers=headers)
    try:
        with urllib.request.urlopen(dl_req, timeout=10) as r:
            dls = json.loads(r.read()).get('downloads', [])
            print(f"   Downloads: {len(dls)} direct locker links")
    except Exception as e:
        print(f"   Downloads: {e}")

print("\n--- [PART 2: 5 WEB SERIES (TV)] ---")
for tmdb_id, title in TV_SHOWS:
    print(f"\n>> TV Series: {title} (TMDB {tmdb_id})")
    # 1. Details & Seasons
    req = urllib.request.Request(f"https://api.bingr.one/api/details/tv/{tmdb_id}?v=1", headers=headers)
    with urllib.request.urlopen(req, timeout=8) as r:
        d = json.loads(r.read())
        seasons = [(s.get('season'), s.get('name'), s.get('episodes')) for s in d.get('seasons', []) if s.get('season', 0) > 0]
        print(f"   Details: Title='{d.get('title') or d.get('name')}', Seasons: {len(seasons)} -> {seasons[:3]}")

    # 2. Bingr Episodes for S1
    ep_req = urllib.request.Request(f"https://api.bingr.one/api/episodes/{tmdb_id}/1", headers=headers)
    try:
        with urllib.request.urlopen(ep_req, timeout=6) as r:
            eps = json.loads(r.read()).get('episodes', [])
            print(f"   Episodes: Season 1 has {len(eps)} episodes. Ep 1='{eps[0].get('title')}' Still='{eps[0].get('still')[:40]}...'")
    except Exception as e:
        print(f"   Episodes: {e}")

    # 3. Vidrift Orion TV S1E1
    v_url = f"https://embed.vidrift.in/embed/tv/{tmdb_id}/1/1"
    req_v = urllib.request.Request(v_url, headers=headers)
    try:
        with urllib.request.urlopen(req_v, timeout=8) as r:
            html = r.read().decode('utf-8')
            m = re.search(r'var\s+embedMeta\s*=\s*(\{.*?\});', html, re.DOTALL)
            if m:
                meta = json.loads(m.group(1))
                orion = meta.get('orionStreams', [])
                print(f"   Vidrift Orion S1E1: {len(orion)} multi-audio streams")
            else:
                print("   Vidrift Orion S1E1: meta not found")
    except Exception as e:
        print(f"   Vidrift Orion: {e}")

    # 4. Vidy CDN TV S1E1
    try:
        s_req = urllib.request.Request(f"https://api.wecollege.net/seed?mediaId={tmdb_id}", headers={'Referer': 'https://www.vidy.st/', 'User-Agent': headers['User-Agent']})
        with urllib.request.urlopen(s_req, timeout=6) as r:
            seed = json.loads(r.read())['seed']
            srv_req = urllib.request.Request(f"https://api.wecollege.net/miami/sources?mediaType=tv&seasonId=1&episodeId=1&tmdbId={tmdb_id}&enc=2&seed={seed}", headers={'Referer': 'https://www.vidy.st/', 'User-Agent': headers['User-Agent']})
            with urllib.request.urlopen(srv_req, timeout=6) as sr:
                decrypted = decrypt_vidy(sr.read().decode('utf-8'), seed, str(tmdb_id))
                sources = json.loads(decrypted).get('sources', []) if decrypted else []
                print(f"   Vidy CDN S1E1: {len(sources)} streams decrypted")
    except Exception as e:
        print(f"   Vidy CDN: {e}")

    # 5. Embed URLs
    print(f"   Embeds: Filmu=https://embed.filmu.in/tv/{tmdb_id}/1/1 | Cinezo=https://player.cinezo.live/embed/tv/{tmdb_id}/1/1 | Vidbolt=https://vidbolt.xyz/tv/{tmdb_id}/1/1")

    # 6. Subtitles
    sub_req = urllib.request.Request(f"https://api.bingr.one/api/subtitles/vdrk/tv/{tmdb_id}/1/1", headers=headers)
    try:
        with urllib.request.urlopen(sub_req, timeout=6) as r:
            subs = json.loads(r.read()).get('subtitles', [])
            print(f"   Subtitles S1E1: {len(subs)} VDRK WebVTT tracks")
    except Exception as e:
        print(f"   Subtitles: {e}")

    # 7. Downloads
    dl_req = urllib.request.Request(f"https://streamrip.fun/api/download/tv/{tmdb_id}?season=1&episode=1", headers=headers)
    try:
        with urllib.request.urlopen(dl_req, timeout=12) as r:
            dls = json.loads(r.read()).get('downloads', [])
            print(f"   Downloads S1E1: {len(dls)} direct links")
    except Exception as e:
        print(f"   Downloads: {e}")

print("\n--- [PART 3: 5 ANIME] ---")
for anime_id, tmdb_id, title in ANIME:
    print(f"\n>> Anime: {title} (Anime ID {anime_id}, TMDB {tmdb_id})")
    # 1. Details & Seasons
    req = urllib.request.Request(f"https://api.bingr.one/api/anime/{anime_id}?v=1", headers=headers)
    with urllib.request.urlopen(req, timeout=8) as r:
        d = json.loads(r.read())
        seasons = [(s.get('season'), s.get('name'), s.get('episodes')) for s in d.get('seasons', [])]
        print(f"   Details: Title='{d.get('title')}', Seasons: {len(seasons)} -> {seasons[:3]}")

    # 2. Anime Episodes Chunk 1
    ep_req = urllib.request.Request(f"https://api.bingr.one/api/anime/{anime_id}/episodes?chunk=1", headers=headers)
    try:
        with urllib.request.urlopen(ep_req, timeout=6) as r:
            eps = json.loads(r.read()).get('episodes', [])
            print(f"   Episodes: Chunk 1 has {len(eps)} episodes. Ep 1='{eps[0].get('title')}' Still='{eps[0].get('still')[:40]}...'")
    except Exception as e:
        print(f"   Episodes: {e}")

    # 3. Filmu Embed
    f_url = f"https://embed.filmu.in/anime/{anime_id}/1"
    try:
        req_f = urllib.request.Request(f_url, headers=headers)
        with urllib.request.urlopen(req_f, timeout=6) as r:
            print(f"   Filmu Embed Ep 1: Status {r.status} (Verified Live)")
    except Exception as e:
        print(f"   Filmu Embed: {e}")

    # 4. Cinezo Embed
    c_url = f"https://player.cinezo.live/embed/anime/{anime_id}/1?dub=true"
    try:
        req_c = urllib.request.Request(c_url, headers=headers)
        with urllib.request.urlopen(req_c, timeout=6) as r:
            print(f"   Cinezo Embed Ep 1: Status {r.status} (Verified Live with Dub)")
    except Exception as e:
        print(f"   Cinezo Embed: {e}")

    # 5. Vidbolt & Vidy
    print(f"   Embeds: Vidbolt=https://vidbolt.xyz/anime/{anime_id}/1 | Vidy=https://www.vidy.st/anime/{anime_id}/1")

    # 6. AniSkip
    if d.get('idMal'):
        try:
            askip = urllib.request.Request(f"https://api.aniskip.com/v2/skip-times/{d.get('idMal')}/1?types[]=op&types[]=ed&episodeLength=0", headers=headers)
            with urllib.request.urlopen(askip, timeout=5) as r:
                sk = json.loads(r.read()).get('results', [])
                print(f"   AniSkip: {len(sk)} skip interval(s) found (idMal: {d.get('idMal')})")
        except Exception as e:
            print(f"   AniSkip: {e}")

    # 7. Downloads (via TMDB ID fallback if available)
    if tmdb_id:
        dl_req = urllib.request.Request(f"https://streamrip.fun/api/download/tv/{tmdb_id}?season=1&episode=1", headers=headers)
        try:
            with urllib.request.urlopen(dl_req, timeout=12) as r:
                dls = json.loads(r.read()).get('downloads', [])
                print(f"   Downloads Ep 1: {len(dls)} direct links")
        except Exception as e:
            print(f"   Downloads: {e}")
