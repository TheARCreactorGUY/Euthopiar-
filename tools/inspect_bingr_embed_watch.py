import re

with open("tools/Watch.js", "r", encoding="utf-8") as f:
    text = f.read()

matches = set(re.findall(r'https?://[^\s"\'`]+', text))
print("Watch.js URLs:")
for m in sorted(matches):
    print("  ", m)

# Also search for template strings like `embed/` or `embed`
embed_templates = set(re.findall(r'[`"\'][^`"\']*embed[^`"\']*[`"\']', text, re.IGNORECASE))
print("\nEmbed templates:")
for t in list(embed_templates)[:20]:
    print("  ", t)
