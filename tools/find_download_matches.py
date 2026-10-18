import re, glob

for f in ['tools/AnimeDetails-xGGA17NG.js', 'tools/TvDetails-CKqgoTJP.js', 'tools/MovieDetails-FmCUyFJl.js', 'tools/Watch.js']:
    t = open(f, encoding='utf-8').read()
    print(f, "download matches:")
    for m in re.finditer(r'download', t, re.IGNORECASE):
        print("  ", t[max(0, m.start() - 40):min(len(t), m.end() + 60)])
