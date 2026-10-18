import re

with open("tools/Watch.js", "r", encoding="utf-8") as f:
    watch = f.read()

# Let's find any object or variable defining servers or tabs
# Search for names like 'Vidrift', 'Vidy', etc.
matches = re.finditer(r'(Vidrift|Vidy|Filmu|Vidbolt|Cinezo)', watch)
for m in matches:
    start = max(0, m.start() - 200)
    end = min(len(watch), m.end() + 200)
    print("--- SERVER MATCH ---")
    print(watch[start:end])
    print()
