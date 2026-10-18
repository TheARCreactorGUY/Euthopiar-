import re

with open("tools/AnimeDetails-xGGA17NG.js", "r", encoding="utf-8") as f:
    txt = f.read()

print("AnimeDetails length:", len(txt))
# Search for watch URL formation
matches = re.findall(r'[`"\'/]/watch/[^`"\']+', txt)
print("Watch routes in AnimeDetails:", matches)

# Search for episode click or season click
matches2 = re.findall(r'episode[^;,\}]+', txt)
for m in matches2[:10]:
    print("  EPISODE:", m)
