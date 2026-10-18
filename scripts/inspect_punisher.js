const https = require('https');

function fetch(url) {
    return new Promise((resolve, reject) => {
        https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' } }, res => {
            if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                let loc = res.headers.location;
                if (!loc.startsWith('http')) {
                    const parsed = new URL(url);
                    loc = parsed.origin + loc;
                }
                return fetch(loc).then(resolve, reject);
            }
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => resolve(data));
        }).on('error', reject);
    });
}

async function inspectPunisher() {
    const html = await fetch('https://4khdhub.one/marvels-the-punisher-series-2288/');
    
    // Find everything with class containing episode or season
    const regex = /<div[^>]*class=["'][^"']*(?:episode|season|download)[^"']*["'][^>]*>[\s\S]*?<\/div>/gi;
    console.log('HTML length:', html.length);

    // Let's find all elements that have class="season-item" or class="episode-item"
    const seasonMatches = [...html.matchAll(/<div[^>]*class=["'][^"']*season-item[^"']*["'][\s\S]*?<\/div>/gi)];
    console.log('season-item matches:', seasonMatches.length);

    const episodeMatches = [...html.matchAll(/<div[^>]*class=["'][^"']*episode-item[^"']*["']/gi)];
    console.log('episode-item matches:', episodeMatches.length);

    // Let's see what buttons or links exist in punisher
    const aMatches = [...html.matchAll(/<a[^>]*href=["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>/gi)];
    console.log('Total A tags:', aMatches.length);
    aMatches.forEach(m => {
        const href = m[1];
        const text = m[2].replace(/<[^>]+>/g, '').trim().replace(/\s+/g, ' ');
        if (!href.includes('/category/') && !href.includes('/tag/') && !href.includes('/author/') && !href.includes('wp-admin') && href !== 'https://4khdhub.one/' && href !== '#') {
            console.log(`[A] "${text}" -> ${href}`);
        }
    });

    // Also print a 2000 char section of the main content
    const entryContent = html.match(/<div class="entry-content[\s\S]*?<\/div>\s*<!-- \.entry-content -->/i) 
                      || html.match(/<main[\s\S]*?<\/main>/i)
                      || html.match(/<article[\s\S]*?<\/article>/i);
    if (entryContent) {
        console.log('\n--- Entry content excerpt ---');
        console.log(entryContent[0].substring(0, 3000));
    } else {
        console.log('No entry content matched, searching for download container');
        const dlIdx = html.indexOf('episodes-list');
        if (dlIdx !== -1) {
            console.log(html.substring(Math.max(0, dlIdx - 500), dlIdx + 1500));
        }
    }
}

inspectPunisher().catch(console.error);
