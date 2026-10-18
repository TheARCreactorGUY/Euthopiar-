import urllib.request, ssl, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {
    'Referer': 'https://vidsrc-embed.ru/embed/movie/634649',
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
}

req = urllib.request.Request('https://vidsrc-embed.ru/vs_src.php?type=movie&id=634649', headers=headers)
try:
    resp = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')
    print('vs_src response:', resp)
    data = json.loads(resp)
    src = data.get('src')
    print('Source URL:', src)
    if src:
        req2 = urllib.request.Request(src, headers={'Referer': 'https://vidsrc-embed.ru/', 'User-Agent': 'Mozilla/5.0'})
        resp2 = urllib.request.urlopen(req2, context=ctx).read().decode('utf-8')
        print('Target iframe len:', len(resp2))
        print('Target snippet:', resp2[:300])
except Exception as e:
    print('vs_src error:', e)
