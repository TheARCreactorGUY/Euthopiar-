import re, glob

for f in ['tools/AnimeDetails-xGGA17NG.js', 'tools/TvDetails-CKqgoTJP.js']:
    t = open(f, encoding='utf-8').read()
    print(f, "length:", len(t))
    links = set(re.findall(r'/watch/[^\s"\'`]+', t))
    for l in sorted(links):
        print("  route:", l)
    apis = set(re.findall(r'/api/[^\s"\'`]+', t))
    for a in sorted(apis):
        print("  api:", a)
