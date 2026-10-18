import re

for name in ['filmu', 'cinezo', 'vidbolt']:
    print(f"=== {name} ===")
    content = open(f"tools/{name}_page.html", encoding="utf-8").read()
    # find all script src
    srcs = re.findall(r'<script[^>]+src=["\']([^"\']+)["\']', content)
    print("Script src:", srcs)
    # find api endpoints or urls
    urls = re.findall(r'https?://[^\s"\'`<>]+', content)
    print("Unique URLs:", set(urls))
    # find fetch or XMLHttpRequest or axios
    calls = re.findall(r'(?:fetch|get|post)\(["\']([^"\']+)["\']', content)
    print("Calls:", calls)
