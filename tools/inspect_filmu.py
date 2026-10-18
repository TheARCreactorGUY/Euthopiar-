import re

html = open('tools/filmu_probe.html', 'r', encoding='utf-8').read()
print("Filmu length:", len(html))
print("Scripts:", re.findall(r'src=["\']([^"\']+)["\']', html))
# Find script content
script_tags = re.findall(r'<script\b[^>]*>(.*?)</script>', html, re.DOTALL)
print("Inline script tags:", len(script_tags))
for i, s in enumerate(script_tags):
    print(f"--- Script {i} ({len(s)} chars) ---")
    print(s[:500])
