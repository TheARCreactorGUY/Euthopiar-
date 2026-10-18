const fs = require('fs');
const code = fs.readFileSync('tools/Ads_deobfuscated.js', 'utf8');

for (const m of code.matchAll(/\/streams\/[^\s"']+/g)) {
  console.log('Stream endpoint:', m[0]);
}

// Find where server list is defined or fetched
const sIdx = code.indexOf('/streams/movie/');
console.log('\nAround /streams/movie/:');
console.log(code.slice(Math.max(0, sIdx - 600), Math.min(code.length, sIdx + 600)));
