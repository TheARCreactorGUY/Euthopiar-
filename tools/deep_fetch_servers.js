const https = require('https');
const crypto = require('crypto');

function evpKdf(password, salt, keyLen = 32, ivLen = 16) {
    let d = Buffer.alloc(0);
    let d_i = Buffer.alloc(0);
    while (d.length < keyLen + ivLen) {
        d_i = crypto.createHash('md5').update(Buffer.concat([d_i, Buffer.from(password, 'utf8'), salt])).digest();
        d = Buffer.concat([d, d_i]);
    }
    return {
        key: d.subarray(0, keyLen),
        iv: d.subarray(keyLen, keyLen + ivLen)
    };
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

function postJson(url, data, headers) {
    return new Promise((resolve, reject) => {
        const u = new URL(url);
        const req = https.request(u, {
            method: 'POST',
            headers: {
                ...headers,
                'Content-Length': Buffer.byteLength(data)
            }
        }, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => resolve(JSON.parse(body)));
        });
        req.on('error', reject);
        req.write(data);
        req.end();
    });
}

function fetchRaw(url, headers) {
    return new Promise((resolve, reject) => {
        const u = new URL(url);
        const req = https.request(u, {
            method: 'GET',
            headers
        }, (res) => {
            let chunks = [];
            res.on('data', chunk => chunks.push(chunk));
            res.on('end', () => {
                const buf = Buffer.concat(chunks);
                resolve({
                    statusCode: res.statusCode,
                    headers: res.headers,
                    body: buf.toString('utf8'),
                    rawBuf: buf
                });
            });
        });
        req.on('error', reject);
        req.end();
    });
}

async function testServer(srv) {
    const defaultHeaders = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
        'Referer': 'https://vidstuck.xyz/embed/movie/1022789',
        'Origin': 'https://vidstuck.xyz',
        'Content-Type': 'application/json'
    };

    const tokenBody = JSON.stringify({
        'a7f39c821d604e5b9c71f36e1547b': '1022789',
        'c285f91ab306d28147a35632e816b': 'movie',
        '6b491e7253ad84d392e7561a9384c': srv
    });

    const tokData = await postJson('https://vidstuck.xyz/backend/fuckoffniggawtaf', tokenBody, defaultHeaders);
    const { ts, token } = tokData;

    const url = `https://vidstuck.xyz/backend/servers/${srv}?a7f39c821d604e5b9c71f36e1547b=1022789&c285f91ab306d28147a35632e816b=movie&6b491e7253ad84d392e7561a9384c=${srv}&61d9a5274c8e3b29afd6384c291e6=${ts}&c492f7a183d6502b1e7436c538a716d=${token}&5e28c9147a306d1e829f3674b392a1=Inside+Out+2&b731e6c94f08269d725f8341c306e=2024&e164932c50216a39e5814b3027=2024-06-11`;
    
    const data = await fetchRaw(url, { 'User-Agent': defaultHeaders['User-Agent'], 'Referer': 'https://vidstuck.xyz/' });
    const jsonResp = JSON.parse(data.body);
    console.log(`\n================= Server: ${srv} =================`);
    console.log('Success:', jsonResp.success);
    const links = jsonResp.links || [];
    for (const l of links) {
        const dec = decryptCryptoJs(l.link, passphrase);
        const fullUrl = dec.startsWith('http') ? dec : `https://vidstuck.xyz${dec}`;
        console.log(`  Decrypted URL: ${fullUrl}`);
        console.log(`  Reported type: ${l.type}, res: ${l.resolution}`);

        // Now let's fetch this stream URL!
        const streamResp = await fetchRaw(fullUrl, {
            'User-Agent': defaultHeaders['User-Agent'],
            'Referer': 'https://vidstuck.xyz/embed/movie/1022789',
            'Origin': 'https://vidstuck.xyz'
        });
        console.log(`  Stream fetch status: ${streamResp.statusCode}`);
        console.log(`  Stream content-type: ${streamResp.headers['content-type']}`);
        console.log(`  Stream content snippet:\n${streamResp.body.slice(0, 300)}`);
    }
}

async function main() {
    for (const srv of ['atlas', 'meow', 'orion', 'centaurus', 'andromeda']) {
        try {
            await testServer(srv);
        } catch(e) {
            console.error(`Error testing ${srv}:`, e.message);
        }
    }
}

main();
