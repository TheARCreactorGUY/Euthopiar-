with open('tools/movie_details_sample.html', 'r', encoding='utf-8') as f:
    html = f.read()
import re
m = re.search(r'https://greenmotors\.club/\?id=[^\'\"\s]+', html)
if m:
    start = max(0, m.start() - 500)
    end = min(len(html), m.end() + 300)
    print(html[start:end])
