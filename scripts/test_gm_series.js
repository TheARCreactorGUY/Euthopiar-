const https = require('https');

function fetch(url, headers = {}) {
    return new Promise((resolve, reject) => {
        const u = new URL(url);
        const options = {
            hostname: u.hostname,
            path: u.pathname + u.search,
            method: 'GET',
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
                'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
                ...headers
            }
        };
        https.get(options, res => {
            if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                let loc = res.headers.location;
                if (!loc.startsWith('http')) {
                    loc = u.origin + loc;
                }
                return fetch(loc, headers).then(resolve, reject);
            }
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => resolve(data));
        }).on('error', reject);
    });
}

function rot13(str) {
    return str.replace(/[a-zA-Z]/g, function(c) {
        return String.fromCharCode((c <= "Z" ? 90 : 122) >= (c = c.charCodeAt(0) + 13) ? c : c - 26);
    });
}

function base64Decode(str) {
    return Buffer.from(str, 'base64').toString('utf8');
}

async function bypassGreenmotors(gmUrl) {
    const html = await fetch(gmUrl);
    const m = html.match(/s\(\s*['"]o['"]\s*,\s*['"]([^'"]+)['"]\s*,\s*\d+/);
    if (!m) {
        console.log('No payload in GM response! HTML snippet:');
        console.log(html.substring(0, 500));
        return null;
    }
    const rawPayload = m[1];
    const s1 = base64Decode(rawPayload);
    const s2 = base64Decode(s1);
    const s3 = rot13(s2);
    const s4 = base64Decode(s3);
    const root = JSON.parse(s4);
    const dest = base64Decode(root.o);
    return dest;
}

async function test() {
    console.log('Testing Mobland greenmotors link...');
    const moblandGm = "https://greenmotors.club/?id=Q1dxTzFnbjhrQm5kMG5lL01tQzcvdEhwZ0JtZVdjNUEvbXdPRHcyWGVHVXRuUG9YNW80cXAzMzJJV2NqWUJMZUYrVFQzUU5BelFJMENRYXdNbUhjQ1E9PQ==";
    const dest1 = await bypassGreenmotors(moblandGm);
    console.log('Mobland resolved target:', dest1);

    if (dest1) {
        console.log('Fetching destination...');
        const destHtml = await fetch(dest1, { 'Referer': 'https://greenmotors.club/' });
        console.log('Destination length:', destHtml.length);
        console.log('Destination title:', (destHtml.match(/<title>([^<]+)<\/title>/i) || [])[1]);
        
        // Let's inspect all links on destination!
        const aRegex = /<a[^>]*href=["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>/gi;
        let m;
        const links = [];
        while ((m = aRegex.exec(destHtml)) !== null) {
            const href = m[1];
            const text = m[2].replace(/<[^>]+>/g, '').trim().replace(/\s+/g, ' ');
            links.push({ text, href });
        }
        console.log(`Destination links (${links.length}):`);
        links.forEach(l => console.log('  ->', l.text, '|', l.href));

        // Check if there are episode links or gamerxyt
        const gamerMatch = destHtml.match(/var\s+url\s*=\s*['"]([^'"]+)['"]/i) || destHtml.match(/href=['"](https:\/\/gamerxyt\.com\/hubcloud\.php[^'"]+)['"]/i);
        console.log('Gamerxyt match:', gamerMatch ? gamerMatch[1] : 'none');
    }
}

test().catch(console.error);
