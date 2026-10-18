const https = require('https');
const crypto = require('crypto');

function evpKdf(password, salt, keyLen = 32, ivLen = 16) {
    let d = Buffer.alloc(0);
    let d_i = Buffer.alloc(0);
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

async function test() {
    const tokReq = https.request('https://vidstuck.xyz/backend/fuckoffniggawtaf', {
        method: 'POST',
        headers: {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
            'Referer': 'https://vidstuck.xyz/embed/movie/1022789',
            'Origin': 'https://vidstuck.xyz',
            'Content-Type': 'application/json'
        }
    }, res => {
        let b = ''; res.on('data', c => b+=c); res.on('end', () => {
            const tok = JSON.parse(b);
            const url = 'https://vidstuck.xyz/backend/servers/orion?a7f39c821d604e5b9c71f36e1547b=1022789&c285f91ab306d28147a35632e816b=movie&6b491e7253ad84d392e7561a9384c=orion&61d9a5274c8e3b29afd6384c291e6=' + tok.ts + '&c492f7a183d6502b1e7436c538a716d=' + tok.token + '&5e28c9147a306d1e829f3674b392a1=Inside+Out+2&b731e6c94f08269d725f8341c306e=2024&e164932c50216a39e5814b3027=2024-06-11';
            https.get(url, { headers: { 'Referer': 'https://vidstuck.xyz/' } }, sRes => {
                let sb = ''; sRes.on('data', sc => sb+=sc); sRes.on('end', () => {
                    const sData = JSON.parse(sb);
                    const dec = decryptCryptoJs(sData.links[0].link, passphrase);
                    https.get('https://vidstuck.xyz' + dec, { headers: { 'Referer': 'https://vidstuck.xyz/' } }, mRes => {
                        let mb = ''; mRes.on('data', mc => mb+=mc); mRes.on('end', () => {
                            const mediaMatch = mb.match(/media="([^"]+)"/);
                            if (mediaMatch) {
                                console.log('MEDIA TEMPLATE:', mediaMatch[1]);
                                const realChunkUrl = mediaMatch[1]
                                    .replace('$RepresentationID$', '0')
                                    .replace('$Number%05d$', '00001')
                                    .replace('$Number$', '1');
                                console.log('REAL CHUNK URL:', realChunkUrl);
                                https.get(realChunkUrl, { headers: { 'Referer': 'https://vidstuck.xyz/' } }, cRes => {
                                    console.log('CHUNK STATUS:', cRes.statusCode);
                                    console.log('CHUNK HEADERS:', cRes.headers);
                                    let cb = []; cRes.on('data', chunk => cb.push(chunk)); cRes.on('end', () => {
                                        const buf = Buffer.concat(cb);
                                        console.log('CHUNK LENGTH:', buf.length);
                                        console.log('FIRST 16 BYTES:', buf.subarray(0, 16).toString('hex'));
                                    });
                                });
                            }
                        });
                    });
                });
            });
        });
    });
    tokReq.write(JSON.stringify({'a7f39c821d604e5b9c71f36e1547b': '1022789', 'c285f91ab306d28147a35632e816b': 'movie', '6b491e7253ad84d392e7561a9384c': 'orion'}));
    tokReq.end();
}
test();
