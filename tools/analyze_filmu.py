import re

js = open('tools/filmu_bundle.js', 'r', encoding='utf-8').read()

print("Filmu bundle length:", len(js))
endpoints = re.findall(r'[`"\'/](/[a-zA-Z0-9_\-\./\?\=\&\{\}\$]+)[`"\']', js)
filtered = [e for e in set(endpoints) if any(k in e for k in ['api', 'source', 'stream', 'embed', 'video', 'm3u8', 'vidsrc', 'provider'])]
print("Filmu endpoints:", len(filtered))
for e in sorted(filtered)[:20]:
    print(" ", e)

# Also search for domains
domains = set(re.findall(r'https?://[a-zA-Z0-9\.\-_]+', js))
print("Filmu domains:")
for d in sorted(domains):
    print(" ", d)
