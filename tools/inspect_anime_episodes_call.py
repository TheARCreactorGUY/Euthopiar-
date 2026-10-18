t = open("tools/Watch.js", encoding="utf-8").read()

import re
print("Matches with /anime/ in Watch.js:")
for m in re.finditer(r'/anime/', t):
    print(t[max(0, m.start() - 60):min(len(t), m.end() + 100)])
