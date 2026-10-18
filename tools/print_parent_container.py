with open('tools/movie_details_sample.html', 'r', encoding='utf-8') as f:
    html = f.read()
import re
m = re.search(r'https://greenmotors\.club/\?id=[^\'\"\s]+', html)
if m:
    start = max(0, m.start() - 1500)
    end = min(len(html), m.start() - 200)
    print(html[start:end])
