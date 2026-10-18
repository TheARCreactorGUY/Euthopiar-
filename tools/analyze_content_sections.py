import re

with open("tools/ContentSections-DVGU_g2Q.js", "r", encoding="utf-8") as f:
    cs = f.read()

print("ContentSections length:", len(cs))
endpoints = set(re.findall(r'[`"\'/](/[a-zA-Z0-9_\-\./\?\=\&\{\}\$]+)[`"\']', cs))
print("Endpoints in ContentSections:")
for ep in sorted(endpoints):
    print(" ", ep)

# Also check for genres or categories
genres = set(re.findall(r'genre[s]?=[0-9a-zA-Z,]+', cs))
print("Genres in ContentSections:")
for g in sorted(genres):
    print(" ", g)
