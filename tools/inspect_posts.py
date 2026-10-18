import urllib.request
import re

def inspect_posts():
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
        'Referer': 'https://moviesleech.club/'
    }
    url = 'https://moviesleech.club/'
    req = urllib.request.Request(url, headers=headers)
    html = urllib.request.urlopen(req).read().decode('utf-8', errors='ignore')
    
    posts = re.findall(r'<a\s+[^>]*href=[\"\'](https://moviesleech\.club/download-[^\"\']+)[\"\']', html)
    print("Recent posts:", posts[:5])
    
    for p in posts[:3]:
        print("\n=== Post:", p)
        p_req = urllib.request.Request(p, headers=headers)
        p_html = urllib.request.urlopen(p_req).read().decode('utf-8', errors='ignore')
        
        # Find all download / external links on this post
        links = re.findall(r'<a\s+[^>]*href=[\"\'](https?://[^\"\']+)[\"\'][^>]*>(.*?)</a>', p_html, re.I | re.S)
        for h, t in links:
            t_clean = re.sub(r'<[^>]+>', '', t).strip()
            if any(k in h for k in ['leechpro', 'hubcloud', 'drive', 'gamerxyt', 'cloud', 'seed']):
                print(f"  Target: {h} | Text: {t_clean[:50]}")
                
                if 'leechpro.blog/archives/' in h:
                    l_req = urllib.request.Request(h, headers=headers)
                    l_html = urllib.request.urlopen(l_req).read().decode('utf-8', errors='ignore')
                    l_links = re.findall(r'<a\s+[^>]*href=[\"\'](https?://[^\"\']+)[\"\'][^>]*>(.*?)</a>', l_html, re.I | re.S)
                    for lh, lt in l_links:
                        if not any(k in lh for k in ['leechpro', 'templatelens', 'wordpress', 'gmpg', 'schema']):
                            lt_clean = re.sub(r'<[^>]+>', '', lt).strip()
                            lt_ascii = lt_clean.encode('ascii', 'ignore').decode('ascii')
                            print(f"    -> Leech Sub-Link: {lh[:80]}... | Text: {lt_ascii[:40]}")

if __name__ == '__main__':
    inspect_posts()
