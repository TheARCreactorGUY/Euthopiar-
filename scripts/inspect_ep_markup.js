const https = require('https');

function fetch(url) {
    return new Promise((resolve, reject) => {
        https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' } }, res => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => resolve(data));
        }).on('error', reject);
    });
}

async function run() {
    const html = await fetch('https://4khdhub.one/the-final-problem-series-8203/');
    // find all occurrences of class="episode-download-item" or class="download-item"
    const regex = /<div[^>]*class=["'][^"']*episode-download-item[^"']*["'][\s\S]*?<\/div>\s*<\/div>\s*<\/div>/gi;
    const firstMatch = regex.exec(html);
    if (firstMatch) {
        console.log('Match length:', firstMatch[0].length);
        console.log(firstMatch[0].substring(0, 1500));
    }

    // Also look at the container around season-item and episode-item
    const seasonIdx = html.indexOf('season-item');
    if (seasonIdx !== -1) {
        console.log('\n--- Season Item HTML ---');
        console.log(html.substring(seasonIdx - 100, seasonIdx + 1500));
    }
}

run().catch(console.error);
