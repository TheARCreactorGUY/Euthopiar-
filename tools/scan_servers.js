const https = require('https');

const candidates = [
    'atlas', 'meow', 'orion', 'centaurus', 'andromeda',
    'neptune', 'titan', 'saturn', 'jupiter', 'mars', 'pluto', 'venus', 'mercury',
    'aurora', 'nebula', 'nova', 'pulsar', 'galaxy', 'cosmos', 'hyperion', 'hydra',
    'pegasus', 'draco', 'cygnus', 'phoenix', 'sirius', 'vega', 'polaris', 'rigel', 'altair', 'betelgeuse',
    'vidsrc', 'vidplay', 'filemoon', 'streamtape', 'mixdrop', 'upcloud', 'megacloud', 'superstream',
    'hls', 'vip', 'fast', 'pro', 'backup', 'mirror', 'mirror1', 'mirror2', 'server1', 'server2', 'server3',
    'alpha', 'beta', 'gamma', 'delta', 'omega', 'ursa', 'canis', 'leo', 'aries', 'taurus', 'gemini'
];

async function checkServer(name) {
    return new Promise(resolve => {
        const req = https.request('https://vidstuck.xyz/backend/fuckoffniggawtaf', {
            method: 'POST',
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                'Referer': 'https://vidstuck.xyz/embed/movie/1022789',
                'Origin': 'https://vidstuck.xyz',
                'Content-Type': 'application/json'
            }
        }, res => {
            let b = '';
            res.on('data', c => b += c);
            res.on('end', () => {
                try {
                    const tok = JSON.parse(b);
                    if (tok && tok.token) {
                        const sUrl = `https://vidstuck.xyz/backend/servers/${name}?a7f39c821d604e5b9c71f36e1547b=1022789&c285f91ab306d28147a35632e816b=movie&6b491e7253ad84d392e7561a9384c=${name}&61d9a5274c8e3b29afd6384c291e6=${tok.ts}&c492f7a183d6502b1e7436c538a716d=${tok.token}&5e28c9147a306d1e829f3674b392a1=Inside+Out+2&b731e6c94f08269d725f8341c306e=2024&e164932c50216a39e5814b3027=2024-06-11`;
                        https.get(sUrl, { headers: { 'Referer': 'https://vidstuck.xyz/', 'User-Agent': 'Mozilla/5.0' } }, sRes => {
                            let sb = '';
                            sRes.on('data', sc => sb += sc);
                            sRes.on('end', () => {
                                try {
                                    const sData = JSON.parse(sb);
                                    if (sData.success === true) {
                                        resolve({ name, valid: true, linksCount: (sData.links || []).length, dubs: (sData.dubs || []).length });
                                    } else {
                                        resolve({ name, valid: false, err: sData.error || 'failed' });
                                    }
                                } catch(e) {
                                    resolve({ name, valid: false, err: 'non-json' });
                                }
                            });
                        }).on('error', () => resolve({ name, valid: false, err: 'net-err' }));
                    } else {
                        resolve({ name, valid: false, err: 'no-tok' });
                    }
                } catch(e) {
                    resolve({ name, valid: false, err: 'parse-tok-err' });
                }
            });
        });
        req.on('error', () => resolve({ name, valid: false, err: 'tok-req-err' }));
        req.write(JSON.stringify({
            'a7f39c821d604e5b9c71f36e1547b': '1022789',
            'c285f91ab306d28147a35632e816b': 'movie',
            '6b491e7253ad84d392e7561a9384c': name
        }));
        req.end();
    });
}

async function main() {
    console.log(`Scanning ${candidates.length} candidate servers...`);
    const results = [];
    for (let i = 0; i < candidates.length; i += 5) {
        const chunk = candidates.slice(i, i + 5);
        const res = await Promise.all(chunk.map(checkServer));
        for (const r of res) {
            if (r.valid) {
                console.log(`[FOUND SERVER] ${r.name}: ${r.linksCount} links, ${r.dubs} dubs`);
            }
        }
    }
    console.log('Done scanning.');
}

main();
