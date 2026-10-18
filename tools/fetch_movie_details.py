import urllib.request

url = "https://4khdhub.one/avengers-infinity-war-marvel-phase-3-movie-325/"
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36'
}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=12) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

with open('tools/movie_details_sample.html', 'w', encoding='utf-8') as f:
    f.write(html)

print("Saved tools/movie_details_sample.html. Length:", len(html))
