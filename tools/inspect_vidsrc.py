import urllib.request, ssl, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {'Referer': 'https://www.1shows.org/', 'User-Agent': 'Mozilla/5.0'}

req = urllib.request.Request('https://vidsrc-embed.ru/embed/movie/634649', headers=headers)
html = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')

m = re.search(r'<iframe[^>]*id=[\'"]player_iframe[\'"][^>]*>', html)
if m:
    print('player_iframe tag:', m.group(0))
else:
    print('player_iframe not found')

m2 = re.search(r'data-api=[\'"]([^\'"]+)[\'"]', html)
if m2:
    print('data-api:', m2.group(1))

# Search for any player URLs or stream URLs in the script that uses data-api
idx = html.find("getAttribute('data-api')")
if idx != -1:
    print('\nContext around data-api in script:')
    print(html[idx-100:idx+600])
