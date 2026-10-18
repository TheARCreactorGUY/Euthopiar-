import urllib.request, ssl, re
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {'Referer': 'https://player.vidzee.wtf/', 'User-Agent': 'Mozilla/5.0'}

req = urllib.request.Request('https://player.vidzee.wtf/assets/Ads-IamQT1xR.js', headers=headers)
js = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')

idx = js.find('No\\x20stream\\x20URL')
if idx != -1:
    print('Found snippet around No stream URL:')
    print(js[max(0, idx - 1000): min(len(js), idx + 1000)])
else:
    print('Not found')
