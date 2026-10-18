import re

def analyze_file(fname):
    print(f"\n=================== {fname} ===================")
    html = open(fname, 'r', encoding='utf-8').read()
    scripts = re.findall(r'<script\b[^>]*>(.*?)</script>', html, re.DOTALL)
    for i, s in enumerate(scripts):
        if len(s.strip()) > 0:
            print(f"--- Script {i} ({len(s)} chars) ---")
            print(s.encode('ascii', 'backslashreplace').decode('ascii')[:400])

analyze_file('tools/cinezo_probe.html')
analyze_file('tools/vidbolt_probe.html')
