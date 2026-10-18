import urllib.request, json

headers = {'User-Agent': 'Mozilla/5.0', 'Referer': 'https://bingr.one/'}

def check(url, label):
    print(f"\n==================== {label} ====================")
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        seasons = data.get('seasons', [])
        print(f"Total seasons: {len(seasons)}")
        for s in seasons:
            s_num = s.get('season') or s.get('season_number')
            s_name = s.get('name')
            s_eps = s.get('episodes')
            ep_list = s.get('episode_list') or s.get('episodes_list') or []
            print(f"  Season {s_num} ('{s_name}'): episodes count = {s_eps}, ep_list len = {len(ep_list) if isinstance(ep_list, list) else ep_list}")
            if isinstance(s_eps, list) and s_eps:
                print(f"    Sample episode: {s_eps[0]}")
            elif isinstance(s_eps, int):
                print(f"    Episodes count is integer: {s_eps}")

check("https://api.bingr.one/api/details/tv/1399?v=1", "Game of Thrones (TV)")
check("https://api.bingr.one/api/anime/30?v=1", "Neon Genesis Evangelion (Anime)")
check("https://api.bingr.one/api/anime/21?v=1", "One Piece (Anime)")
