with open("tools/filmu_servers.js", encoding="utf-8") as f:
    t = f.read()

import re
for domain in ['box.filmu.in', 'showbox.filmu.in', 'ott.filmu.in', 'prime.filmu.in', 'api.filmu.in']:
    idx = t.find(domain)
    if idx != -1:
        print(f"=== {domain} ===")
        print(t[max(0, idx - 150):min(len(t), idx + 250)])
