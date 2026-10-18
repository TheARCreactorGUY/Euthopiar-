import urllib.request, json, base64, hashlib
from Crypto.Cipher import AES

def evp_kdf(password, salt, key_len=32, iv_len=16):
    d = b''
    d_i = b''
    while len(d) < key_len + iv_len:
        d_i = hashlib.md5(d_i + password + salt).digest()
        d += d_i
    return d[:key_len], d[key_len:key_len+iv_len]

def decrypt_cryptojs(cipher_b64, passphrase):
    raw = base64.b64decode(cipher_b64)
    salt = raw[8:16]
    ciphertext = raw[16:]
    key, iv = evp_kdf(passphrase.encode(), salt)
    cipher = AES.new(key, AES.MODE_CBC, iv)
    dec = cipher.decrypt(ciphertext)
    pad = dec[-1]
    return dec[:-pad].decode('utf-8')

passphrase = '7f4c9e2a81d63b05c4f7a9e8126d3b50e1a8c7f23d9465ab0c6e9f1d4a7b832c'
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://vidstuck.xyz/embed/movie/1022789',
    'Origin': 'https://vidstuck.xyz',
    'Content-Type': 'application/json'
}

# 1. Get token
token_body = json.dumps({
    'a7f39c821d604e5b9c71f36e1547b': '1022789',
    'c285f91ab306d28147a35632e816b': 'movie',
    '6b491e7253ad84d392e7561a9384c': 'orion'
}).encode()

req = urllib.request.Request('https://vidstuck.xyz/backend/fuckoffniggawtaf', data=token_body, headers=headers)
with urllib.request.urlopen(req) as resp:
    tok_data = json.loads(resp.read().decode())
    print('Token response:', tok_data)

ts = tok_data['ts']
token = tok_data['token']

for srv in ['atlas', 'meow', 'orion', 'centaurus', 'andromeda']:
    url = f'https://vidstuck.xyz/backend/{srv}?a7f39c821d604e5b9c71f36e1547b=1022789&c285f91ab306d28147a35632e816b=movie&6b491e7253ad84d392e7561a9384c={srv}&61d9a5274c8e3b29afd6384c291e6={ts}&c492f7a183d6502b1e7436c538a716d={token}&5e28c9147a306d1e829f3674b392a1=Inside+Out+2&b731e6c94f08269d725f8341c306e=2024&e164932c50216a39e5814b3027=2024-06-11'
    req_srv = urllib.request.Request(url, headers={'User-Agent': headers['User-Agent'], 'Referer': 'https://vidstuck.xyz/'})
    try:
        with urllib.request.urlopen(req_srv) as r:
            data = json.loads(r.read().decode())
            print(f'=== Server {srv} ===')
            links = data.get('links', [])
            for l in links:
                enc = l.get('link')
                if enc:
                    dec = decrypt_cryptojs(enc, passphrase)
                    t = l.get('type')
                    res = l.get('resolution')
                    print(f'  Decrypted: {dec} | Type: {t} | Res: {res}')
            dubs = data.get('dubs', [])
            if dubs:
                print(f'  Dubs available: {len(dubs)} (e.g. {[d.get("lanName") for d in dubs[:5]]})')
    except Exception as e:
        print(f'=== Server {srv} ERROR: {e} ===')
