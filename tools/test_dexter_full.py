import urllib.request, ssl, re, json, base64, hashlib
from urllib.parse import urlparse, parse_qs
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def resolve_dexter(tmdb_id, is_tv=False, season=1, episode=1):
    path = f'/e/tv/{tmdb_id}/{season}/{episode}?autostart=true' if is_tv else f'/e/movie/{tmdb_id}?autostart=true'
    embed_url = 'https://play.xpass.top' + path
    req = urllib.request.Request(embed_url, headers={'Referer': 'https://www.viduki.net/', 'User-Agent': 'Mozilla/5.0'})
    html = urllib.request.urlopen(req, context=ctx).read().decode('utf-8', errors='ignore')
    
    m = re.search(r'var\s+dataUrl\s*=\s*["\']([^"\']+)["\']', html)
    if not m:
        print('No dataUrl found in Dexter HTML')
        return []
    data_url = m.group(1)
    full_data_url = 'https://play.xpass.top' + data_url
    
    parsed = urlparse(data_url)
    pathname = parsed.path
    qs = parse_qs(parsed.query)
    token = qs.get('token', [''])[0]
    
    req2 = urllib.request.Request(full_data_url, headers={
        'Referer': embed_url,
        'User-Agent': 'Mozilla/5.0',
        'X-Requested-With': 'XMLHttpRequest'
    })
    enc_data = urllib.request.urlopen(req2, context=ctx).read().decode('utf-8').strip()
    
    norm = enc_data.replace('-', '+').replace('_', '/')
    norm += '=' * ((4 - len(norm) % 4) % 4)
    raw = base64.b64decode(norm)
    iv = raw[:12]
    tag = raw[-16:]
    ct = raw[12:-16]
    
    secret = 'spv3-data-response|spv3-build-1787821613-50e5fc97c9dce367|' + pathname + '|' + token
    key = hashlib.sha256(secret.encode('utf-8')).digest()
    
    aes = AESGCM(key)
    dec = aes.decrypt(iv, ct + tag, None)
    servers = json.loads(dec.decode('utf-8'))
    print(f'Dexter returned {len(servers)} servers!')
    
    results = []
    for s in servers[:3]:
        p_url = 'https://play.xpass.top' + s['url']
        req3 = urllib.request.Request(p_url, headers={'Referer': 'https://play.xpass.top/', 'User-Agent': 'Mozilla/5.0'})
        try:
            p_data = json.loads(urllib.request.urlopen(req3, context=ctx).read().decode('utf-8'))
            file_url = p_data['playlist'][0]['sources'][0]['file']
            print(f'  [{s["name"]}] -> {file_url[:80]}...')
            results.append((s['name'], file_url))
        except Exception as e:
            print(f'  [{s["name"]}] error:', e)
    return results

print('Testing Dexter for Movie 634649...')
resolve_dexter('634649')
print('\nTesting Dexter for TV 1399 (Game of Thrones)...')
resolve_dexter('1399', is_tv=True, season=1, episode=1)
