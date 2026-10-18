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

async function run() {
    const html = await fetch('https://4khdhub.one/the-final-problem-series-8203/');
    // find body
    const bodyIdx = html.indexOf('<body');
    const seasonItemIdx = html.indexOf('class="season-item"', bodyIdx);
    console.log('seasonItemIdx:', seasonItemIdx);
    if (seasonItemIdx !== -1) {
        console.log(html.substring(seasonItemIdx - 50, seasonItemIdx + 2000));
    } else {
        console.log('No season-item found in body');
        // Let's find episode-item
        const epIdx = html.indexOf('class="episode-item"', bodyIdx);
        console.log('episode-item in body:', epIdx);
        if (epIdx !== -1) {
            console.log(html.substring(epIdx - 50, epIdx + 2000));
        }
    }
}

run().catch(console.error);
