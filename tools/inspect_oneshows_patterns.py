import re

doc = open('test_eups_repo/sources/OneShowsPlugin.kt', 'r', encoding='utf-8').read()

def show_section(pattern, length=1500):
    m = re.search(pattern, doc)
    if m:
        print(f"\n==================== PATTERN: {pattern} ====================")
        print(doc[m.start():m.start() + length])

show_section(r'suspend fun getDownloadLinks')
show_section(r'AniSkipClient')
show_section(r'OpenSubtitles v3 Bridge')
show_section(r'MatchScorer\.score')
