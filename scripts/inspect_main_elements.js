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
    const mainIdx = html.indexOf('<main');
    const mainHtml = html.substring(mainIdx);
    
    // Find all class names in mainHtml
    const classes = new Set();
    const classRegex = /class=["']([^"']+)["']/g;
    let m;
    while ((m = classRegex.exec(mainHtml)) !== null) {
        m[1].split(/\s+/).forEach(c => classes.add(c));
    }
    console.log('Classes inside <main>:', Array.from(classes));

    // Print all div IDs or structures with "content-" or "download-" or "episode-"
    const ids = [];
    const idRegex = /id=["']([^"']+)["']/g;
    while ((m = idRegex.exec(mainHtml)) !== null) {
        ids.push(m[1]);
    }
    console.log('IDs inside <main>:', ids);

    // Let's print the entire download section of the HTML!
    // Search for "download-section" or "accordion" or "download"
    const dlIdx = mainHtml.indexOf('download-section');
    console.log('download-section idx:', dlIdx);
    if (dlIdx !== -1) {
        console.log(mainHtml.substring(dlIdx - 50, dlIdx + 3000));
    } else {
        // Find where greenmotors first appears
        const gmIdx = mainHtml.indexOf('greenmotors');
        console.log('first greenmotors at:', gmIdx);
        console.log(mainHtml.substring(Math.max(0, gmIdx - 800), gmIdx + 1200));
    }
}

run().catch(console.error);
