def parse_episode_data(data):
    s = data.replace("eup://bingr/", "").replace("eup://", "")
    parts = s.split("/")
    
    # Check if format contains media type
    # e.g. "1399/tv/1/2" or "30/anime/1/5" or "550/movie"
    if len(parts) >= 4 and parts[1] in ["tv", "anime", "movie"]:
        m_id = parts[0]
        m_type = parts[1]
        season = int(parts[2])
        episode = int(parts[3])
        return {"id": m_id, "type": m_type, "season": season, "episode": episode}
    
    if len(parts) >= 2 and parts[1] == "movie":
        return {"id": parts[0], "type": "movie", "season": None, "episode": None}

    # Format like "1399:1:2"
    if ":" in s:
        c_parts = s.split(":")
        if len(c_parts) >= 3:
            return {"id": c_parts[0], "type": "tv", "season": int(c_parts[1]), "episode": int(c_parts[2])}
        elif len(c_parts) == 1:
            return {"id": c_parts[0], "type": "movie", "season": None, "episode": None}

    # Format like "1399/1/2"
    if len(parts) >= 3:
        return {"id": parts[0], "type": "tv", "season": int(parts[1]), "episode": int(parts[2])}

    # Format like "550"
    return {"id": parts[0], "type": "movie", "season": None, "episode": None}

samples = [
    "eup://bingr/1399/tv/1/1",
    "eup://bingr/30/anime/1/5",
    "eup://bingr/550/movie",
    "eup://bingr/1399/1/1",
    "eup://bingr/550",
    "1399:2:5",
    "550"
]

for sample in samples:
    print(f"Sample: {sample:<28} -> {parse_episode_data(sample)}")
