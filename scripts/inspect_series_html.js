const https = require('https');

function fetch(url) {
    return new Promise((resolve, reject) => {
        https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, res => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => resolve(data));
        }).on('error', reject);
    });
}

async function testSeries() {
    const urls = [
        'https://4khdhub.one/mobland-series-98/',
        'https://4khdhub.one/the-final-problem-series-8203/',
        'https://4khdhub.one/a-different-world-series-8184/',
        'https://4khdhub.one/the-scandal-series-8138/'
    ];

    for (const u of urls) {
        console.log('\n========================================');
        console.log('Fetching:', u);
        const html = await fetch(u);
        
        // Find all links in the content / download sections
        const linkRegex = /<a[^>]*href=["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>/gi;
        let m;
        const foundLinks = [];
        while ((m = linkRegex.exec(html)) !== null) {
            const href = m[1];
            const text = m[2].replace(/<[^>]+>/g, '').trim();
            if (href.includes('greenmotors') || href.includes('hubcloud') || href.includes('/episode') || href.includes('-series-') || href.includes('/category/')) {
                foundLinks.push({ href, text });
            }
        }
        console.log('Found relevant links count:', foundLinks.length);
        foundLinks.slice(0, 10).forEach(l => console.log('  ->', l.text.substring(0, 40), '|', l.href));

        // Check if there are other anchor tags or buttons
        const allA = [];
        const aRegex = /<a[^>]*class=["']([^"']*)["'][^>]*href=["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>/gi;
        while ((m = aRegex.exec(html)) !== null) {
            if (m[2].startsWith('http') && !m[2].includes('tmdb') && !m[2].includes('fonts') && !m[2].includes('schema')) {
                allA.push({ cls: m[1], href: m[2], text: m[3].replace(/<[^>]+>/g, '').trim() });
            }
        }
        console.log('External/Direct A tags:', allA.length);
        allA.slice(0, 10).forEach(l => console.log('   [A]', l.cls, '|', l.text.substring(0, 30), '|', l.href));
    }
}

testSeries();
