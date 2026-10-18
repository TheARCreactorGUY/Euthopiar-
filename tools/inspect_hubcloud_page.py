import urllib.request
import re

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36',
    'Referer': 'https://greenmotors.club/'
}
url = 'https://hubcloud.ist/drive/wm1bbhpaop10ao7'
req = urllib.request.Request(url, headers=headers)
try:
    with urllib.request.urlopen(req, timeout=12) as resp:
        print('Status:', resp.status)
        print('Final URL:', resp.geturl())
        html = resp.read().decode('utf-8', errors='ignore')
        print('HTML length:', len(html))
        with open('tools/hubcloud_sample.html', 'w', encoding='utf-8') as f:
            f.write(html)
        links = re.findall(r'<a\s+[^>]*href=[\'"]([^\'"]+)[\'"][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
        print(f'Total links: {len(links)}')
        for h, t in links[:20]:
            clean_t = re.sub(r'<[^>]+>', '', t).strip()
            print(f'  [{clean_t}] -> {h}')
        forms = re.findall(r'<form\s+[^>]*action=[\'"]([^\'"]*)[\'"][^>]*>(.*?)</form>', html, re.DOTALL | re.IGNORECASE)
        print(f'Total forms: {len(forms)}')
        for a, b in forms:
            print(f'  Form action: {a}')
            inputs = re.findall(r'<input\s+[^>]*name=[\'"]([^\'"]+)[\'"][^>]*value=[\'"]([^\'"]*)[\'"]', b)
            print(f'    inputs: {inputs}')
except Exception as e:
    print('Error:', e)
