import re

doc = open('docs/EUP_V2_DEVELOPER_GUIDE.md', 'r', encoding='utf-8').read()

terms = ['MatchScorer', 'TmdbBridge', 'AniSkip', 'OpenSubtitles', 'WebResolver']
for t in terms:
    print(f"\n==================== {t} ====================")
    matches = list(re.finditer(t, doc, re.IGNORECASE))
    print(f"Occurrences: {len(matches)}")
    for m in matches[:3]:
        start = max(0, m.start() - 200)
        end = min(len(doc), m.end() + 300)
        print("--- SNIPPET ---")
        print(doc[start:end])
