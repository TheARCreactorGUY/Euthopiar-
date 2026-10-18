const https = require('https');


function fetch(url) {
    return new Promise((resolve, reject) => {
        https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' } }, res => {
            if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                let loc = res.headers.location;
                if (!loc.startsWith('http')) {
                    loc = new URL(url).origin + loc;
                }
                return fetch(loc).then(resolve, reject);
            }
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => resolve(data));
        }).on('error', reject);
    });
}

function parseQuality(str) {
    const s = str.toLowerCase();
    if (s.includes('2160p') || s.includes('4k')) return '4K';
    if (s.includes('1080p') || s.includes('fhd')) return '1080p';
    if (s.includes('720p') || s.includes('hd')) return '720p';
    if (s.includes('480p') || s.includes('sd')) return '480p';
    return 'HD';
}

async function debugSeries(url) {
    console.log('\n======================================================');
    console.log('Testing series:', url);
    const html = await fetch(url);
    
    // Look at tabs
    const hasCompletePack = html.includes('id="complete-pack"');
    const hasEpisodesTab = html.includes('id="episodes"');
    console.log(`Tabs found: complete-pack=${hasCompletePack}, episodes=${hasEpisodesTab}`);

    // If there is an episodes tab, let's inspect each episode group
    // In #episodes:
    // <div class="season-item episode-item"> or .episode-item
    const episodeItems = [];
    const epItemRegex = /<div[^>]*class=["'][^"']*(?:season-item|episode-item)[^"']*["'][\s\S]*?(?=(?:<div[^>]*class=["'][^"']*(?:season-item|episode-item)[^"']*["']|$))/gi;
    let m;
    while ((m = epItemRegex.exec(html)) !== null) {
        episodeItems.push(m[0]);
    }
    console.log(`Matched ${episodeItems.length} season/episode accordion blocks`);

    // Let's also extract all .episode-download-item directly
    const dlItems = [];
    const dlRegex = /<div[^>]*class=["'][^"']*episode-download-item[^"']*["'][\s\S]*?<\/div>\s*<\/div>\s*<\/div>/gi;
    while ((m = dlRegex.exec(html)) !== null) {
        dlItems.push(m[0]);
    }
    console.log(`Found ${dlItems.length} total episode-download-item in whole page`);

    const episodeMap = new Map(); // key: "S{s}E{e}", val: []
    for (const itemHtml of dlItems) {
        const titleMatch = itemHtml.match(/class=["']episode-file-title["']>([\s\S]*?)<\/div>/i);
        const fileTitle = titleMatch ? titleMatch[1].replace(/<[^>]+>/g, '').trim() : '';

        const badgeMatch = itemHtml.match(/class=["']badge-psa["']>([\s\S]*?)<\/span>/i);
        const badge = badgeMatch ? badgeMatch[1].replace(/<[^>]+>/g, '').trim() : '';

        // Extract Season and Episode
        let seasonNum = 1;
        let epNum = 1;
        const sMatch = fileTitle.match(/\bS(\d{1,2})E(\d{1,3})\b/i);
        if (sMatch) {
            seasonNum = parseInt(sMatch[1], 10);
            epNum = parseInt(sMatch[2], 10);
        } else {
            const epBadgeMatch = badge.match(/\b(?:EP|Episode|E)[-_\s]*(\d+)\b/i);
            if (epBadgeMatch) {
                epNum = parseInt(epBadgeMatch[1], 10);
            }
        }

        // Greenmotors links
        const gmRegex = /href=["'](https:\/\/greenmotors\.club[^"']+)["'][^>]*>([\s\S]*?)<\/a>/gi;
        let gmM;
        const gms = [];
        while ((gmM = gmRegex.exec(itemHtml)) !== null) {
            gms.push({ url: gmM[1], server: gmM[2].replace(/<[^>]+>/g, '').replace(/Download/i, '').trim() });
        }

        const key = `S${seasonNum}E${epNum}`;
        if (!episodeMap.has(key)) {
            episodeMap.set(key, []);
        }
        episodeMap.get(key).push({ fileTitle, badge, links: gms.length });
    }

    console.log(`Distinct Episodes parsed: ${episodeMap.size}`);
    for (const [k, v] of episodeMap.entries()) {
        console.log(`  -> Episode ${k}: ${v.length} quality variants (e.g. "${v[0].fileTitle.substring(0, 50)}...")`);
    }
}

async function run() {
    await debugSeries('https://4khdhub.one/the-final-problem-series-8203/');
    await debugSeries('https://4khdhub.one/marvels-the-punisher-series-2288/');
    await debugSeries('https://4khdhub.one/a-different-world-series-8184/');
    await debugSeries('https://4khdhub.one/mobland-series-98/');
}

run().catch(console.error);
