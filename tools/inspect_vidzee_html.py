import urllib.request, ssl, re, json
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {'Referer': 'https://www.1shows.org/', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
req = urllib.request.Request('https://player.vidzee.wtf/embed/movie/634649', headers=headers)
html = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')
print('Vidzee HTML len:', len(html))
print('Scripts in Vidzee:')
for s in re.findall(r'<script[^>]*src=[\'"]([^\'"]+)[\'"]', html):
    print(' ', s)
print('\nInline scripts:')
for s in re.findall(r'<script[^>]*>(.*?)</script>', html, re.DOTALL):
    if len(s.strip()) > 0:
        print('--- INLINE SCRIPT ---')
        print(s[:500])
