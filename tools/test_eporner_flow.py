import urllib.request
import json
import ssl
import http.client
import re

UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'

def doh(h):
    r = urllib.request.Request(f'https://1.1.1.1/dns-query?name={h}&type=A', headers={'Accept': 'application/dns-json'})
    with urllib.request.urlopen(r) as res:
        d = json.loads(res.read())
        return [x['data'] for x in d.get('Answer', []) if x.get('type') == 1]

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# 1. Fetch API videos as getHomeCatalog() does
ips = doh('www.eporner.com')
unblocked = [ip for ip in ips if ip in ('94.75.220.4', '94.75.220.7')]
target_ip = unblocked[0] if unblocked else ips[0]

print("1. Testing fetchApiVideos...")
conn = http.client.HTTPSConnection(target_ip, 443, context=ctx, timeout=10)
conn.request('GET', '/api/v2/video/search/?query=&per_page=16&page=1&thumbsize=big&order=top-weekly&format=json', headers={
    'Host': 'www.eporner.com',
    'User-Agent': UA
})
res = conn.getresponse()
print("   API status:", res.status)
data = json.loads(res.read().decode('utf-8'))
videos = data.get('videos', [])
print(f"   Got {len(videos)} videos from API.")

if not videos:
    print("NO VIDEOS FOUND")
    exit(1)

sample_video = videos[0]
print(f"2. Testing sample video: id={sample_video.get('id')}, title={sample_video.get('title')}, url={sample_video.get('url')}")

# Simulate MediaItem created by parseApiJson
media_id = sample_video.get('id')
media_url = sample_video.get('url') or f"https://www.eporner.com/video-{media_id}/"

# 3. Simulate getDetails(mediaItem)
print("3. Simulating getDetails(mediaItem)...")
video_id = media_id.removeprefix("video-").split("/")[0].split("-")[0]
video_page_url = media_url if media_url.startswith("http") else f"https://www.eporner.com/{media_url.lstrip('/')}"
print(f"   videoPageUrl = {video_page_url}")

# Fetch HTML
parts = video_page_url.split('/')
v_path = '/' + '/'.join(parts[3:])
conn_page = http.client.HTTPSConnection(target_ip, 443, context=ctx, timeout=10)
conn_page.request('GET', v_path, headers={
    'Host': 'www.eporner.com',
    'User-Agent': UA,
    'Referer': 'https://www.eporner.com/'
})
res_page = conn_page.getresponse()
print(f"   Video page HTTP status: {res_page.status}")
if res_page.status in (301, 302):
    redir = res_page.getheader('Location')
    print(f"   Page Redirect to: {redir}")
    if redir:
        v_path = redir if redir.startswith('/') else '/' + '/'.join(redir.split('/')[3:])
        conn_page2 = http.client.HTTPSConnection(target_ip, 443, context=ctx, timeout=10)
        conn_page2.request('GET', v_path, headers={'Host': 'www.eporner.com', 'User-Agent': UA, 'Referer': 'https://www.eporner.com/'})
        res_page = conn_page2.getresponse()
        print(f"   Redirected page HTTP status: {res_page.status}")

html = res_page.read().decode('utf-8', errors='ignore')
print(f"   HTML size: {len(html)} bytes")

# Extract dloadPaths
dloads = re.findall(r'href="(/dload/[^"]+)"', html)
print(f"   Found {len(dloads)} dload paths: {dloads}")

# Extract contentUrl
content_urls = re.findall(r'"contentUrl":\s*"([^"]+)"', html)
print(f"   Found contentUrl: {content_urls}")

# 4. Simulate resolveDloadRedirect for each dload path
resolved_episodes = []
for p in dloads:
    print(f"4. Testing resolveDloadRedirect for path: {p}")
    conn_dl = http.client.HTTPSConnection(target_ip, 443, context=ctx, timeout=10)
    conn_dl.request('GET', p, headers={
        'Host': 'www.eporner.com',
        'User-Agent': UA,
        'Referer': 'https://www.eporner.com/'
    })
    res_dl = conn_dl.getresponse()
    loc = res_dl.getheader('Location')
    print(f"   status: {res_dl.status}, Location: {loc}")
    if loc:
        clean = loc.split('?dload=')[0]
        resolved_episodes.append(clean)
        print(f"   Clean stream URL: {clean}")

# 5. Test probe on first resolved episode
if resolved_episodes:
    first_stream = resolved_episodes[0]
    print(f"\n5. Probing playback stream: {first_stream}")
    s_parts = first_stream.split('/')
    cdn_host = s_parts[2]
    cdn_path = '/' + '/'.join(s_parts[3:])
    cdn_ips = doh(cdn_host)
    print(f"   CDN host {cdn_host} resolved to IPs: {cdn_ips}")
    if cdn_ips:
        conn_stream = http.client.HTTPSConnection(cdn_ips[0], 443, context=ctx, timeout=10)
        conn_stream.request('GET', cdn_path, headers={
            'Host': cdn_host,
            'User-Agent': UA,
            'Referer': 'https://www.eporner.com/',
            'Origin': 'https://www.eporner.com',
            'Range': 'bytes=0-1024'
        })
        res_stream = conn_stream.getresponse()
        print(f"   Stream status: {res_stream.status}")
        print(f"   Content-Type: {res_stream.getheader('Content-Type')}")
        print(f"   Content-Length: {res_stream.getheader('Content-Length')}")
        print(f"   Content-Range: {res_stream.getheader('Content-Range')}")
else:
    print("NO RESOLVED EPISODES TO PROBE!")
