import urllib.request, ssl, re
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {'Referer': 'https://player.vidzee.wtf/', 'User-Agent': 'Mozilla/5.0'}

req = urllib.request.Request('https://player.vidzee.wtf/assets/Ads-IamQT1xR.js', headers=headers)
js = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')

idx = js.find('async function ne')
print('Index of async function ne:', idx)
# Look at 2000 chars before ne
print(js[max(0, idx - 1500): idx])
