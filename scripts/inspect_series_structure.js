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

async function inspect(url) {
    console.log('\n======================================================');
    console.log('Inspecting:', url);
    const html = await fetch(url);

    // Look for file titles
    const titleRegex = /class=["']file-title["']>([^<]+)<\/div>/g;
    let m;
    const fileTitles = [];
    while ((m = titleRegex.exec(html)) !== null) {
        fileTitles.push(m[1].trim());
    }
    console.log(`Found ${fileTitles.length} file-title elements:`);
    fileTitles.forEach((t, i) => console.log(`  [${i+1}] ${t}`));

    // Look for episode buttons or accordion triggers or sections
    const itemRegex = /<div[^>]*class=["'][^"']*season-toggle[^"']*["'][^>]*>([\s\S]*?)<\/div>/gi;
    let items = [];
    while ((m = itemRegex.exec(html)) !== null) {
        items.push(m[1].replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim());
    }
    console.log(`Found ${items.length} season-toggle elements:`);
    items.forEach((t, i) => console.log(`  [T${i+1}] ${t}`));

    // Check if there are other episode/download classes
    const classes = new Set();
    const classRegex = /class=["']([^"']+)["']/g;
    while ((m = classRegex.exec(html)) !== null) {
        m[1].split(/\s+/).forEach(c => {
            if (c.toLowerCase().includes('episode') || c.toLowerCase().includes('season') || c.toLowerCase().includes('download') || c.toLowerCase().includes('quality')) {
                classes.add(c);
            }
        });
    }
    console.log('Relevant CSS classes:', Array.from(classes));

    // Also check what other pages / URLs exist on the site for series
    // User mentioned: "second thier is no multi audio option , third so this website holds episode in a seperate way it has quality options first then redirects to a page for episode selection and then redirects again for streaming and downloading, I found this issue by seeing every series type content shows only 1 episode on details page, so make sure to fix it ."
    // Let's look closely at how the website links quality options or episode selection!
}

async function main() {
    await inspect('https://4khdhub.one/mobland-series-98/');
    await inspect('https://4khdhub.one/the-scandal-series-8138/');
    await inspect('https://4khdhub.one/the-final-problem-series-8203/');
    await inspect('https://4khdhub.one/marvels-the-punisher-series-2288/');
}

main().catch(console.error);
