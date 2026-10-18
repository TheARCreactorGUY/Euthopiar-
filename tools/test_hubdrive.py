import urllib.request, re

url = 'https://hubdrive.pics/file/12218597393'
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36',
    'Referer': 'https://greenmotors.club/'
}
req = urllib.request.Request(url, headers=headers)
try:
    with urllib.request.urlopen(req, timeout=12) as resp:
        print('HubDrive status:', resp.status)
        print('Final URL:', resp.geturl())
        html = resp.read().decode('utf-8', errors='ignore')
        print('Length:', len(html))
        links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
        for h, t in links[:15]:
            clean_t = re.sub(r'<[^>]+>', '', t).strip()
            print(f'  [{clean_t}] -> {h}')
except Exception as e:
    print('HubDrive error:', e)
