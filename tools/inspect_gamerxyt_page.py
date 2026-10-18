import urllib.request
import re

gamer_url = "https://gamerxyt.com/hubcloud.php?host=hubcloud&id=wm1bbhpaop10ao7&token=cGdzODk0RkJEeU5TT21qUDRxeGRicUYzZldueW4wRXJjM0crQjlsTVVuMD0="
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36',
    'Referer': 'https://hubcloud.ist/'
}
req = urllib.request.Request(gamer_url, headers=headers)
try:
    with urllib.request.urlopen(req, timeout=12) as resp:
        print('Status:', resp.status)
        print('Final URL:', resp.geturl())
        html = resp.read().decode('utf-8', errors='ignore')
        print('HTML length:', len(html))
        with open('tools/gamerxyt_sample.html', 'w', encoding='utf-8') as f:
            f.write(html)
        links = re.findall(r'<a\s+[^>]*href=[\'"]([^\'"]+)[\'"][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
        print(f'Total links on gamerxyt: {len(links)}')
        for h, t in links:
            clean_t = re.sub(r'<[^>]+>', '', t).strip()
            print(f'  [{clean_t}] -> {h}')
except Exception as e:
    print('Error:', e)
