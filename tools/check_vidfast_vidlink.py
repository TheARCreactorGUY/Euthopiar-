import urllib.request, ssl, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {
    'Referer': 'https://www.1shows.org/',
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
}

def check(url, name):
    req = urllib.request.Request(url, headers=headers)
    try:
        html = urllib.request.urlopen(req, context=ctx).read().decode('utf-8', errors='ignore')
        print(f"=== {name} ===")
        # Look for API calls or /api/
        apis = re.findall(r'["\'](/api/[^"\']+)["\']', html)
        print("APIs in page:", apis)
        # Look for script chunks
        chunks = re.findall(r'/_next/static/chunks/[^"\']+\.js', html)
        print("Chunks:", chunks[:5])
        return html, chunks
    except Exception as e:
        print(f"Error {name}:", e)
        return "", []

check('https://vidfast.pro/movie/634649', 'VidFast')
check('https://vidlink.pro/movie/634649', 'VidLink')
