import re

doc = open('test_eups_repo/sources/OneShowsPlugin.kt', 'r', encoding='utf-8').read()
matches = re.findall(r'serverName\s*=\s*"([^"]+)"', doc)
for m in matches[:25]:
    print("serverName:", m)

labels = re.findall(r'serverLabel\s*=\s*"([^"]+)"', doc)
for l in labels[:25]:
    print("serverLabel:", l)
