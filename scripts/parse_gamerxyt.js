const fs = require('fs');
const html = fs.readFileSync('scripts/gamerxyt.html', 'utf8');

const linkRegex = /<a[^>]*href=["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>/gi;
let match;
while ((match = linkRegex.exec(html)) !== null) {
    const href = match[1];
    const text = match[2].replace(/<[^>]+>/g, '').trim();
    if (!href.startsWith('#') && !href.includes('font') && !href.includes('css')) {
        console.log('LINK:', text, '->', href);
    }
}

const formRegex = /<form[^>]*action=["']([^"']+)["'][^>]*>/gi;
while ((match = formRegex.exec(html)) !== null) {
    console.log('FORM ACTION:', match[1]);
}

const scriptRegex = /<script[^>]*>([\s\S]*?)<\/script>/gi;
let idx = 0;
while ((match = scriptRegex.exec(html)) !== null) {
    const s = match[1].trim();
    if (s.length > 0 && !s.includes('google') && !s.includes('cloudflare')) {
        console.log(`SCRIPT ${idx} (${s.length} chars):`, s.substring(0, 300));
    }
    idx++;
}
