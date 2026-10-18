import urllib.request, re, base64, json

url = "https://greenmotors.club/?id=Q1dxTzFnbjhrQm5kMG5lL01tQzcvdEhwZ0JtZVdjNUEvbXdPRHcyWGVHVXdFQWFKTWNpb1JTVDFFblVHam0vVy9PUmVvYU1jNG9GV1ZHNko1bkVEamc9PQ=="

def rot13(s):
    res = []
    for c in s:
        if 'a' <= c <= 'z':
            res.append(chr((ord(c) - ord('a') + 13) % 26 + ord('a')))
        elif 'A' <= c <= 'Z':
            res.append(chr((ord(c) - ord('A') + 13) % 26 + ord('A')))
        else:
            res.append(c)
    return "".join(res)

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36'}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=12) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

m_direct = re.search(r'https?://[^\'"\s]+hubcloud[^\'"\s]+', html)
if m_direct and not any(x in m_direct.group(0) for x in [".css", ".js"]):
    print("Direct HubCloud link:", m_direct.group(0))

m_payload = re.search(r"s\(\s*['\"]o['\"]\s*,\s*['\"]([^'\"]+)['\"]\s*,\s*\d+", html)
if m_payload:
    raw = m_payload.group(1)
    s1 = base64.b64decode(raw).decode('utf-8')
    s2 = base64.b64decode(s1).decode('utf-8')
    s3 = rot13(s2)
    s4 = base64.b64decode(s3).decode('utf-8')
    root = json.loads(s4)
    dest_b64 = root.get("o")
    dest = base64.b64decode(dest_b64).decode('utf-8')
    print("Decoded destination:", dest)
