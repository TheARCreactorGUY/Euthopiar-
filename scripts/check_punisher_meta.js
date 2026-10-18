const fs = require('fs');
const html = fs.readFileSync('C:/Users/sehga/.gemini/antigravity/brain/431c5418-275c-432a-8852-e5741ffcbc3a/.system_generated/steps/2356/content.md', 'utf8');

console.log('Title match:', html.match(/<title>(.*?)<\/title>/i)?.[1]);
console.log('Description:', html.match(/<meta name="description" content="(.*?)"/i)?.[1]);
console.log('JSON-LD:', html.match(/<script type="application\/ld\+json">(.*?)<\/script>/s)?.[1]);
console.log('Has defaultVideoId:', html.includes('defaultVideoId'));
console.log('Poster image:', html.match(/src="(https:\/\/image\.tmdb\.org\/[^"]+)"/)?.[1]);

// Check synopsis
const syn = html.match(/<p class="mt-4">([\s\S]*?)<\/p>/i);
console.log('Synopsis p.mt-4:', syn ? syn[1].trim() : 'NONE');

// Check metadata items
const metaRegex = /<span class="metadata-label">(.*?)<\/span>[\s\S]*?<span class="metadata-value"[^>]*>([\s\S]*?)<\/span>/gi;
let m;
while ((m = metaRegex.exec(html)) !== null) {
    console.log('Meta:', m[1].trim(), '->', m[2].replace(/<[^>]+>/g, '').trim());
}
