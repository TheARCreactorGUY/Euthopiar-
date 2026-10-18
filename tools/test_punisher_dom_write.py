with open('tools/punisher_dom.py', 'w', encoding='utf-8') as out:
    out.write('''
import urllib.request, re
url = "https://4khdhub.one/marvels-the-punisher-series-2288/"
headers = {'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36'}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=12) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

idx = html.find("Episode-10")
if idx != -1:
    print(html[idx-100:idx+400])
''')
