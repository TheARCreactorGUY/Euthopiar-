import urllib.request, re

url = "https://4khdhub.one/?s=Loki"
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36'
}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=12) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

matches = re.findall(r'<a href="([^"]+)" class="movie-card"[^>]*>.*?<h3 class="movie-card-title">([^<]+)</h3>', html, re.DOTALL)
print("Search results for Loki:")
for h, t in matches[:5]:
    print(f"  {t.strip()} -> {h}")
