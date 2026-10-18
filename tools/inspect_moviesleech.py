import urllib.request
import urllib.parse
import re

def inspect_chain():
    user_agent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
    
    # 1. Start from Drishyam Leech link
    leech_url = 'https://leechpro.blog/archives/36626'
    req = urllib.request.Request(leech_url, headers={'User-Agent': user_agent, 'Referer': 'https://moviesleech.club/'})
    html = urllib.request.urlopen(req).read().decode('utf-8', errors='ignore')
    
    match = re.search(r'href=[\"\'](https://cloud\.unblockedgames\.world/\?sid=[^\"\']+)[\"\']', html)
    if not match:
        print("No unblockedgames URL found")
        return
    
    cloud_url = match.group(1)
    print("Cloud URL:", cloud_url)
    
    # Step 1: GET cloud_url
    req1 = urllib.request.Request(cloud_url, headers={'User-Agent': user_agent, 'Referer': leech_url})
    html1 = urllib.request.urlopen(req1).read().decode('utf-8', errors='ignore')
    
    form1_action = re.search(r'<form[^>]*action=[\"\']([^\"\']+)[\"\']', html1).group(1)
    if not form1_action.startswith('http'):
        form1_action = 'https://cloud.unblockedgames.world' + ('/' if not form1_action.startswith('/') else '') + form1_action
        
    inputs1 = dict(re.findall(r'<input[^>]*name=[\"\']([^\"\']+)[\"\'][^>]*value=[\"\']([^\"\']*)[\"\']', html1))
    print("Form 1 action:", form1_action, "inputs:", list(inputs1.keys()))
    
    # Step 2: POST form 1
    data1 = urllib.parse.urlencode(inputs1).encode('utf-8')
    req2 = urllib.request.Request(form1_action, data=data1, headers={'User-Agent': user_agent, 'Referer': cloud_url, 'Content-Type': 'application/x-www-form-urlencoded'})
    resp2 = urllib.request.urlopen(req2)
    step2_url = resp2.geturl()
    html2 = resp2.read().decode('utf-8', errors='ignore')
    
    form2_action = re.search(r'<form[^>]*action=[\"\']([^\"\']+)[\"\']', html2).group(1)
    if not form2_action.startswith('http'):
        form2_action = 'https://cloud.unblockedgames.world' + ('/' if not form2_action.startswith('/') else '') + form2_action
        
    inputs2 = dict(re.findall(r'<input[^>]*name=[\"\']([^\"\']+)[\"\'][^>]*value=[\"\']([^\"\']*)[\"\']', html2))
    print("Form 2 action:", form2_action, "inputs:", list(inputs2.keys()))
    
    # Step 3: POST form 2
    data2 = urllib.parse.urlencode(inputs2).encode('utf-8')
    req3 = urllib.request.Request(form2_action, data=data2, headers={'User-Agent': user_agent, 'Referer': step2_url, 'Content-Type': 'application/x-www-form-urlencoded'})
    html3 = urllib.request.urlopen(req3).read().decode('utf-8', errors='ignore')
    
    sc_match = re.search(r's_343\(\s*[\"\']([^\"\']+)[\"\']\s*,\s*[\"\']([^\"\']+)[\"\']', html3)
    go_match = re.search(r'setAttribute\(\s*[\"\']href[\"\']\s*,\s*[\"\']([^\"\']+)[\"\']', html3)
    
    go_url = go_match.group(1).replace(r'\/', '/')
    print("Go URL:", go_url)
    
    headers4 = {'User-Agent': user_agent, 'Referer': form2_action}
    if sc_match:
        cookie = f"{sc_match.group(1)}={sc_match.group(2)}"
        headers4['Cookie'] = cookie
        print("Cookie:", cookie)
        
    # Step 4: GET go_url
    req4 = urllib.request.Request(go_url, headers=headers4)
    resp4 = urllib.request.urlopen(req4)
    final_url4 = resp4.geturl()
    html4 = resp4.read().decode('utf-8', errors='ignore')
    
    print("Final URL 4:", final_url4)
    
    driveseed_url = None
    if 'driveseed.org' in final_url4:
        driveseed_url = final_url4
    else:
        m = re.search(r'content=[\"\']\d+;\s*url=([^\"\']+)[\"\']', html4, re.I)
        if m:
            driveseed_url = m.group(1).replace('&amp;', '&')
            
    print("DriveSeed URL:", driveseed_url)
    
    if driveseed_url:
        req_a = urllib.request.Request(driveseed_url, headers={'User-Agent': user_agent, 'Referer': 'https://cloud.unblockedgames.world/'})
        html_a = urllib.request.urlopen(req_a).read().decode('utf-8', errors='ignore')
        
        file_path_match = re.search(r'window\.location\.replace\(\s*[\"\']([^\"\']+)[\"\']\s*\)', html_a) or re.search(r'href=[\"\'](/file/[^\"\']+)[\"\']', html_a)
        if file_path_match:
            file_path = file_path_match.group(1)
            file_url = file_path if file_path.startswith('http') else 'https://driveseed.org' + file_path
            print("File URL:", file_url)
            
            req_b = urllib.request.Request(file_url, headers={'User-Agent': user_agent, 'Referer': driveseed_url})
            html_b = urllib.request.urlopen(req_b).read().decode('utf-8', errors='ignore')
            
            # Print all links on DriveSeed file page!
            all_links = re.findall(r'<a\s+[^>]*href=[\"\']([^\"\']+)[\"\'][^>]*>(.*?)</a>', html_b, re.I | re.S)
            print(f"\n--- FOUND {len(all_links)} LINKS ON DRIVESEED PAGE ---")
            for h, t in all_links:
                t_clean = re.sub(r'<[^>]+>', '', t).strip()
                t_ascii = t_clean.encode('ascii', 'ignore').decode('ascii')
                print(f"HREF: {h} | TEXT: {t_ascii}")

if __name__ == '__main__':
    inspect_chain()
