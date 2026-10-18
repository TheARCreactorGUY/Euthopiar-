const https = require('https');
const crypto = require('crypto');

function evpKdf(password, salt, keyLen = 32, ivLen = 16) {
    let d = Buffer.alloc(0); let d_i = Buffer.alloc(0);
    while (d.length < keyLen + ivLen) {
        d_i = crypto.createHash('md5').update(Buffer.concat([d_i, Buffer.from(password, 'utf8'), salt])).digest();
        d = Buffer.concat([d, d_i]);
    }
    return { key: d.subarray(0, keyLen), iv: d.subarray(keyLen, keyLen + ivLen) };
}
function decryptCryptoJs(cipherB64, passphrase) {
    const raw = Buffer.from(cipherB64, 'base64');
    const salt = raw.subarray(8, 16);
    const ciphertext = raw.subarray(16);
    const { key, iv } = evpKdf(passphrase, salt);
    const decipher = crypto.createDecipheriv('aes-256-cbc', key, iv);
    return Buffer.concat([decipher.update(ciphertext), decipher.final()]).toString('utf8');
}
const passphrase = '7f4c9e2a81d63b05c4f7a9e8126d3b50e1a8c7f23d9465ab0c6e9f1d4a7b832c';

async function main() {
    const defaultHeaders = {
        'User-Agent': 'Mozilla/5.0',
        'Referer': 'https://vidstuck.xyz/embed/movie/1022789',
        'Origin': 'https://vidstuck.xyz',
        'Content-Type': 'application/json'
    };

    const tok = await new Promise(r => {
        const req = https.request('https://vidstuck.xyz/backend/fuckoffniggawtaf', { method: 'POST', headers: defaultHeaders }, res => {
            let b = ''; res.on('data', c => b+=c); res.on('end', () => r(JSON.parse(b)));
        });
        req.write(JSON.stringify({'a7f39c821d604e5b9c71f36e1547b': '1022789', 'c285f91ab306d28147a35632e816b': 'movie', '6b491e7253ad84d392e7561a9384c': 'meow'}));
        req.end();
    });

    const sUrl = `https://vidstuck.xyz/backend/servers/meow?a7f39c821d604e5b9c71f36e1547b=1022789&c285f91ab306d28147a35632e816b=movie&6b491e7253ad84d392e7561a9384c=meow&61d9a5274c8e3b29afd6384c291e6=${tok.ts}&c492f7a183d6502b1e7436c538a716d=${tok.token}&5e28c9147a306d1e829f3674b392a1=Inside+Out+2&b731e6c94f08269d725f8341c306e=2024&e164932c50216a39e5814b3027=2024-06-11`;
    const sRes = await new Promise(r => {
        https.get(sUrl, { headers: { 'Referer': 'https://vidstuck.xyz/' } }, res => {
            let b = ''; res.on('data', c => b+=c); res.on('end', () => r(JSON.parse(b)));
        });
    });

    const dec = decryptCryptoJs(sRes.links[0].link, passphrase);
    const fullMasterUrl = dec.startsWith('http') ? dec : `https://vidstuck.xyz${dec}`;
    const master = await new Promise(r => {
        https.get(fullMasterUrl, { headers: { 'Referer': 'https://vidstuck.xyz/' } }, res => {
            let b = ''; res.on('data', c => b+=c); res.on('end', () => r(b));
        });
    });

    const audioMatch = master.match(/URI=\"([^\"]+)\"/);
    console.log('Audio Playlist URL:', audioMatch ? audioMatch[1] : null);
    if (audioMatch) {
        const audioM3u8 = await new Promise(r => {
            https.get(audioMatch[1], { headers: { 'Referer': 'https://vidstuck.xyz/' } }, res => {
                let b = ''; res.on('data', c => b+=c); res.on('end', () => r(b));
            });
        });
        console.log('Audio M3U8 snippet:\n', audioM3u8.slice(0, 500));
        const firstAudioSeg = audioM3u8.split('\n').find(l => l.startsWith('http'));
        if (firstAudioSeg) {
            const segRes = await new Promise(r => {
                https.get(firstAudioSeg.trim(), { headers: { 'Referer': 'https://vidstuck.xyz/' } }, res => {
                    let b = []; res.on('data', c => b.push(c));
                    res.on('end', () => r({ status: res.statusCode, headers: res.headers, len: Buffer.concat(b).length }));
                });
            });
            console.log('Audio Seg Res:', segRes);
        }
    }
}
main();
