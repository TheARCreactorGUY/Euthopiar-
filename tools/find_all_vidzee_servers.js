const fs = require('fs');
const code = fs.readFileSync('tools/Ads_deobfuscated.js', 'utf8');

// Find where _0x30698d is set with useState
const sIdx = code.indexOf('_0x30698d,');
if (sIdx !== -1) {
  console.log('Snippet around _0x30698d:');
  console.log(code.slice(sIdx - 100, sIdx + 500));
}

// Find setServerList or similar
for (const m of code.matchAll(/\[\s*\{[^}]*id:[^}]*\}\s*\]/g)) {
  console.log('Static server list:', m[0]);
}

// Search for any strings ending with cloud or stream or server
for (const m of code.matchAll(/["']([a-zA-Z0-9_\-]+(?:cloud|play|stream|server|cdn|box|drive|flix|fast|load))["']/gi)) {
  console.log('Server-like string:', m[1]);
}
