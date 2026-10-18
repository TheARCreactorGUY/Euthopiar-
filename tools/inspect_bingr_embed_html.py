import re

with open("tools/embed_bingr.html", "r", encoding="utf-8") as f:
    html = f.read()

scripts = re.findall(r'src=["\']([^"\']+\.js[^"\']*)["\']', html)
print("Scripts in embed_bingr.html:")
for s in scripts:
    print(" ", s)
