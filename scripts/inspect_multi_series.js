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

async function run() {
    const listHtml = await fetch('https://4khdhub.one/category/series/');
    const cardRegex = /<a\s+[^>]*class=["'][^"']*movie-card[^"']*["'][^>]*>([\s\S]*?)<\/a>|<a\s+[^>]*href=["']([^"']+)["'][^>]*class=["'][^"']*movie-card[^"']*["'][^>]*>([\s\S]*?)<\/a>/gi;
    
    // Better: extract all a tags with movie-card
    const aRegex = /<a\b([^>]*)>([\s\S]*?)<\/a>/gi;
    let m;
    const seriesList = [];
    while ((m = aRegex.exec(listHtml)) !== null) {
        const attrs = m[1];
        const inner = m[2];
        if (attrs.includes('movie-card')) {
            const hrefM = attrs.match(/href=["']([^"']+)["']/i);
            const href = hrefM ? hrefM[1] : '';
            const titleM = inner.match(/class=["']movie-card-title["']>([^<]+)<\/div>/i);
            const title = titleM ? titleM[1].trim() : 'Unknown';
            let fullHref = href.startsWith('http') ? href : `https://4khdhub.one${href.startsWith('/') ? '' : '/'}${href}`;
            seriesList.push({ title, href: fullHref });
        }
    }
    console.log(`Found ${seriesList.length} series on page 1:`);
    for (let i = 0; i < Math.min(6, seriesList.length); i++) {
        const s = seriesList[i];
        console.log(`\n======================================================`);
        console.log(`[${i+1}] ${s.title} (${s.href})`);
        const sHtml = await fetch(s.href);
        const dlItems = (sHtml.match(/class=["'][^"']*download-item[^"']*["']/g) || []).length;
        const epItems = (sHtml.match(/class=["'][^"']*episode-item[^"']*["']/g) || []).length;
        const seasonItems = (sHtml.match(/class=["'][^"']*season-item[^"']*["']/g) || []).length;
        const epDlItems = (sHtml.match(/class=["'][^"']*episode-download-item[^"']*["']/g) || []).length;
        const gmLinks = (sHtml.match(/greenmotors\.club/g) || []).length;
        console.log(`Counts: download-item=${dlItems}, episode-item=${epItems}, season-item=${seasonItems}, episode-download-item=${epDlItems}, greenmotors=${gmLinks}`);
        
        // Let's see what sections or download buttons exist:
        const fileTitles = [...sHtml.matchAll(/class=["'](?:file-title|episode-file-title)["']>([^<]+)<\/div>/gi)].map(x => x[1].trim());
        console.log('File titles:', fileTitles.slice(0, 5));

        const badges = [...sHtml.matchAll(/class=["']badge-psa["']>([^<]+)<\/span>/gi)].map(x => x[1].trim());
        console.log('Badges (badge-psa):', badges.slice(0, 8));
        
        // Are there episode numbers or links to other episode pages?
        const epNumbers = [...sHtml.matchAll(/class=["']episode-number[^"']*["']>([^<]+)<\/div>/gi)].map(x => x[1].trim());
        console.log('Episode numbers:', epNumbers.slice(0, 8));
    }
}

run().catch(console.error);
