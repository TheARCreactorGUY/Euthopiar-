t = open("tools/Watch.js", encoding="utf-8").read()

import re
m = re.search(r'(?:function\s+et|var\s+et\s*=|let\s+et\s*=|const\s+et\s*=)', t)
if m:
    print("Found et declaration at:", m.start())
    idx = m.start()
    print(t[idx:idx+1500])
