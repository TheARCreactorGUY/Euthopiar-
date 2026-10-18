t = open("tools/Watch.js", encoding="utf-8").read()

import re
matches = [m.start() for m in re.finditer(r'function\s+([a-zA-Z0-9_$]+)\s*\(\s*\{', t[:61432])]
for m in matches[-5:]:
    print("Component at:", m)
    print(t[m:m+200])
