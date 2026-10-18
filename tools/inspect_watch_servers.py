import re

with open("tools/Watch.js", "r", encoding="utf-8") as f:
    watch = f.read()

print("Watch.js size:", len(watch))

# Search for server names, providers, embeds
server_matches = re.findall(r'\{[^{}]*(?:name|server|url|provider|embed)[^{}]*\}', watch)
print("Small objects matching keywords:", len(server_matches))

# Search for array of servers
arrays = re.findall(r'\[(?:\{[^{}]+\},?)+\]', watch)
print("Arrays of objects:", len(arrays))
for a in arrays:
    if any(k in a for k in ['vidrift', 'vidy', 'filmu', 'vidbolt', 'cinezo', 'stream', 'embed', 'server']):
        print("--> Array:", a[:200])

# Let's search for urls or domains in Watch.js
domains = set(re.findall(r'https?://[a-zA-Z0-9\.\-_]+', watch))
print("Domains found in Watch.js:")
for d in sorted(domains):
    print(" ", d)

# Also search without https:// (e.g. //domain or domain.com)
embed_like = set(re.findall(r'[a-zA-Z0-9\-]+\.(?:in|st|xyz|live|net|com|org|me|to|so|pm|pro|stream|one|online|site)[a-zA-Z0-9\-_/]*', watch))
print("Domain-like strings:")
for el in sorted(embed_like):
    print(" ", el)
