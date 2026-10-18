import urllib.request, ssl, re
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {'Referer': 'https://player.vidzee.wtf/', 'User-Agent': 'Mozilla/5.0'}

req = urllib.request.Request('https://player.vidzee.wtf/assets/Ads-IamQT1xR.js', headers=headers)
js = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')

idx = js.find("''+I+")
if idx != -1:
    print('Context around \'\' + I +:')
    print(js[max(0, idx - 500): min(len(js), idx + 200)])
else:
    print("''+I+ not found")

# Look at top of file for imports
print('Top of Ads-IamQT1xR.js:')
print(js[:600])
