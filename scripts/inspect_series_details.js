const https = require('https');

function fetch(url) {
    return new Promise((resolve, reject) => {
        https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' } }, res => {
            if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                return fetch(res.headers.location).then(resolve, reject);
            }
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => resolve(data));
        }).on('error', reject);
    });
}

async function run() {
    console.log('Fetching mobland...');
    const html = await fetch('https://4khdhub.one/mobland-series-98/');
    
    // Let's inspect headings, download sections, buttons, etc.
    const re = /<a[^>]*href=["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>/gi;
    let m;
    const links = [];
    while ((m = re.exec(html)) !== null) {
        const href = m[1];
        const text = m[2].replace(/<[^>]+>/g, '').trim().replace(/\s+/g, ' ');
        if (!href.includes('/category/') && !href.includes('/tag/') && !href.includes('/author/') && !href.includes('wp-admin') && href !== 'https://4khdhub.one/' && href !== '#' && !href.includes('twitter.com') && !href.includes('facebook.com') && !href.includes('t.me')) {
            links.push({ text, href });
        }
    }
    
    console.log('Total filtered links:', links.length);
    links.forEach(l => console.log(`[${l.text}] -> ${l.href}`));

    // Also look at h2/h3/h4/p near downloads
    console.log('\n--- Content Structure Snippets ---');
    const contentMatch = html.match(/<div class="entry-content[\s\S]*?<\/div>\s*<!-- \.entry-content -->/i) || html.match(/<article[\s\S]*?<\/article>/i);
    if (contentMatch) {
        const c = contentMatch[0];
        // print headers and paragraphs
        const headers = c.match(/<h[1-6][^>]*>[\s\S]*?<\/h[1-6]>/gi) || [];
        headers.forEach(h => console.log('HEADER:', h.replace(/<[^>]+>/g, '').trim()));
    }
}

run().catch(console.error);
