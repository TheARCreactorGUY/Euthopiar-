import urllib.request, json

headers = {'User-Agent': 'Mozilla/5.0', 'Referer': 'https://bingr.one/'}

test_endpoints = [
    # Movies (10)
    ("Trending Movies", "/trending/movie"),
    ("Popular Movies", "/discover/movie?sort_by=popularity.desc"),
    ("Top Rated Movies", "/discover/movie?sort_by=vote_average.desc&min_votes=1000"),
    ("Upcoming Movies", "/discover/movie?upcoming=true&sort_by=popularity.desc"),
    ("Action Movies", "/discover/movie?genre=28&sort_by=popularity.desc"),
    ("Sci-Fi Movies", "/discover/movie?genre=878&sort_by=popularity.desc"),
    ("Horror Movies", "/discover/movie?genre=27&sort_by=popularity.desc"),
    ("Romance Movies", "/discover/movie?genre=10749&sort_by=popularity.desc"),
    ("Comedy Movies", "/discover/movie?genre=35&sort_by=popularity.desc"),
    ("Animation Feature Films", "/discover/movie?genre=16&sort_by=popularity.desc"),
    
    # TV (10)
    ("Trending TV Shows", "/trending/tv"),
    ("Popular TV Shows", "/discover/tv?sort_by=popularity.desc"),
    ("Top Rated TV Shows", "/discover/tv?sort_by=vote_average.desc&min_votes=500"),
    ("Upcoming TV", "/discover/tv?upcoming=true&sort_by=popularity.desc"),
    ("Crime & Thriller Series", "/discover/tv?with_genres=80,9648&sort_by=popularity.desc"),
    ("Comedy Series", "/discover/tv?with_genres=35&sort_by=popularity.desc"),
    ("Action & Adventure Series", "/discover/tv?with_genres=10759&sort_by=popularity.desc"),
    ("Sci-Fi & Fantasy Series", "/discover/tv?with_genres=10765&sort_by=popularity.desc"),
    ("Animation TV", "/discover/tv?genre=16"),
    ("Drama Series", "/discover/tv?with_genres=18&sort_by=popularity.desc"),

    # Anime (10)
    ("Trending Anime", "/anime/trending"),
    ("Discover Anime", "/anime/discover"),
    ("Popular Anime", "/anime/discover?sort=POPULARITY_DESC&isAdult=false"),
    ("Top Rated Anime", "/anime/discover?sort=SCORE_DESC&isAdult=false"),
    ("Action Anime", "/anime/discover?genre=Action&sort=POPULARITY_DESC&isAdult=false"),
    ("Adventure Anime", "/anime/discover?genre=Adventure&sort=POPULARITY_DESC&isAdult=false"),
    ("Fantasy Anime", "/anime/discover?genre=Fantasy&sort=SCORE_DESC&isAdult=false"),
    ("Comedy Anime", "/anime/discover?genre=Comedy&sort=POPULARITY_DESC&isAdult=false"),
    ("Sci-Fi Anime", "/anime/discover?genre=Sci-Fi&sort=POPULARITY_DESC&isAdult=false"),
    ("Anime Movies", "/anime/discover?sort=SCORE_DESC&format=MOVIE")
]

base = "https://api.bingr.one/api"
passed = 0
for name, path in test_endpoints:
    url = f"{base}{path}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read())
            res = data.get('results', [])
            print(f"[OK] {name:<26} -> {len(res)} items")
            passed += 1
    except Exception as e:
        print(f"[FAIL] {name:<26} -> {e}")

print(f"\nTotal: {passed}/{len(test_endpoints)} endpoints operational.")
