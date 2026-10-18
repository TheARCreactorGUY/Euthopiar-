import urllib.request

url = 'https://4khdhub.one/marvels-the-punisher-series-2288/'
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=12) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

idx = html.find('class="episode-file-info"')
print(html[idx-400:idx+600])
