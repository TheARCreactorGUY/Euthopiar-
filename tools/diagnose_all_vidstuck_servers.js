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

function postJson(url, data, headers) {
    return new Promise((resolve, reject) => {
        const u = new URL(url);
        const req = https.request(u, {
            method: 'POST',
            headers: { ...headers, 'Content-Length': Buffer.byteLength(data) }
        }, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                try { resolve(JSON.parse(body)); } catch (e) { reject(e); }
            });
        });
        req.on('error', reject);
        req.write(data);
        req.end();
    });
}

function fetchRaw(url, headers = {}) {
    return new Promise((resolve, reject) => {
        const u = new URL(url);
        const req = https.request(u, { method: 'GET', headers }, (res) => {
            let chunks = [];
            res.on('data', chunk => chunks.push(chunk));
            res.on('end', () => {
                resolve({
                    statusCode: res.statusCode,
                    headers: res.headers,
                    body: Buffer.concat(chunks).toString('utf8'),
                    rawBuf: Buffer.concat(chunks)
                });
            });
        });
        req.on('error', reject);
        req.end();
    });
}

async function testServer(serverKey, tmdbId = '1022789') {
    console.log(`\n================ Testing ${serverKey} ================`);
    const defaultHeaders = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Referer': `https://vidstuck.xyz/embed/movie/${tmdbId}`,
        'Origin': 'https://vidstuck.xyz',
        'Content-Type': 'application/json'
    };

    try {
        const tokData = await postJson('https://vidstuck.xyz/backend/fuckoffniggawtaf', JSON.stringify({
            'a7f39c821d604e5b9c71f36e1547b': tmdbId,
            'c285f91ab306d28147a35632e816b': 'movie',
            '6b491e7253ad84d392e7561a9384c': serverKey
        }), defaultHeaders);

        const sUrl = `https://vidstuck.xyz/backend/servers/${serverKey}?a7f39c821d604e5b9c71f36e1547b=${tmdbId}&c285f91ab306d28147a35632e816b=movie&6b491e7253ad84d392e7561a9384c=${serverKey}&61d9a5274c8e3b29afd6384c291e6=${tokData.ts}&c492f7a183d6502b1e7436c538a716d=${tokData.token}&5e28c9147a306d1e829f3674b392a1=Inside+Out+2&b731e6c94f08269d725f8341c306e=2024&e164932c50216a39e5814b3027=2024-06-11`;
        
        const sData = await fetchRaw(sUrl, { 'Referer': 'https://vidstuck.xyz/', 'User-Agent': defaultHeaders['User-Agent'] });
        const sJson = JSON.parse(sData.body);
        console.log(`Server ${serverKey} raw response:`, JSON.stringify({ linksCount: sJson.links?.length, dubsCount: sJson.dubs?.length }));

        if (!sJson.links || sJson.links.length === 0) {
            console.log(`No links for ${serverKey}`);
            return;
        }

        for (let i = 0; i < sJson.links.length; i++) {
            const l = sJson.links[i];
            const dec = decryptCryptoJs(l.link, passphrase);
            const streamUrl = dec.startsWith('http') ? dec : `https://vidstuck.xyz${dec}`;
            console.log(`Link [${i}]: type=${l.type}, res=${l.resolution}, decUrl=${streamUrl.slice(0, 100)}...`);

            // Test fetching without Referer vs with Referer
            const manifestRes = await fetchRaw(streamUrl, { 
                'User-Agent': defaultHeaders['User-Agent'],
                'Referer': 'https://vidstuck.xyz/'
            });
            console.log(`Manifest HTTP status: ${manifestRes.statusCode}, Content-Type: ${manifestRes.headers['content-type']}, Body length: ${manifestRes.rawBuf.length}`);

            // Inspect body content
            const isM3u8 = manifestRes.body.includes('#EXTM3U');
            const isMpd = manifestRes.body.includes('<MPD');
            console.log(`Is M3U8? ${isM3u8}, Is MPD? ${isMpd}`);

            if (isM3u8) {
                const lines = manifestRes.body.split('\n');
                const subPlaylist = lines.find(line => line.startsWith('http'));
                if (subPlaylist) {
                    console.log(`Fetching child playlist: ${subPlaylist.slice(0, 80)}...`);
                    const childRes = await fetchRaw(subPlaylist.trim(), {
                        'User-Agent': defaultHeaders['User-Agent'],
                        'Referer': 'https://vidstuck.xyz/'
                    });
                    console.log(`Child playlist status: ${childRes.statusCode}, Body snippet: ${childRes.body.slice(0, 150)}...`);
                    const segLine = childRes.body.split('\n').find(line => line.startsWith('http'));
                    if (segLine) {
                        console.log(`Fetching segment: ${segLine.slice(0, 80)}...`);
                        const segRes = await fetchRaw(segLine.trim(), {
                            'User-Agent': defaultHeaders['User-Agent'],
                            'Referer': 'https://vidstuck.xyz/'
                        });
                        console.log(`Segment status: ${segRes.statusCode}, Content-Type: ${segRes.headers['content-type']}, Length: ${segRes.rawBuf.length}, First byte: 0x${segRes.rawBuf[0]?.toString(16)}`);
                    }
                }
            } else if (isMpd) {
                // Parse Representation and codecs
                const repMatches = manifestRes.body.match(/<Representation[^>]+>/g);
                if (repMatches) {
                    console.log(`DASH Representations:`, repMatches.map(r => {
                        const id = r.match(/id="([^"]+)"/)?.[1];
                        const codecs = r.match(/codecs="([^"]+)"/)?.[1];
                        const w = r.match(/width="([^"]+)"/)?.[1];
                        const h = r.match(/height="([^"]+)"/)?.[1];
                        return { id, codecs, w, h };
                    }));
                }
                const initMatch = manifestRes.body.match(/initialization="([^"]+)"/);
                if (initMatch) {
                    const initUrl = initMatch[1].replace(/&amp;/g, '&');
                    console.log(`Fetching DASH init segment: ${initUrl.slice(0, 80)}...`);
                    const initRes = await fetchRaw(initUrl, {
                        'User-Agent': defaultHeaders['User-Agent'],
                        'Referer': 'https://vidstuck.xyz/'
                    });
                    console.log(`Init segment status: ${initRes.statusCode}, Content-Type: ${initRes.headers['content-type']}, Length: ${initRes.rawBuf.length}, has ftyp? ${initRes.rawBuf.includes(Buffer.from('ftyp'))}`);
                }
                const mediaMatch = manifestRes.body.match(/media="([^"]+)"/);
                if (mediaMatch) {
                    let chunkUrl = mediaMatch[1].replace(/&amp;/g, '&')
                        .replace(/\$RepresentationID\$/g, '0')
                        .replace(/\$Number%05d\$/g, '00001');
                    console.log(`Fetching DASH chunk 1: ${chunkUrl.slice(0, 80)}...`);
                    const chunkRes = await fetchRaw(chunkUrl, {
                        'User-Agent': defaultHeaders['User-Agent'],
                        'Referer': 'https://vidstuck.xyz/'
                    });
                    console.log(`Chunk 1 status: ${chunkRes.statusCode}, Content-Type: ${chunkRes.headers['content-type']}, Length: ${chunkRes.rawBuf.length}, has moof? ${chunkRes.rawBuf.includes(Buffer.from('moof'))}`);
                }
            }
        }
    } catch (e) {
        console.error(`Error testing ${serverKey}:`, e);
    }
}

async function run() {
    await testServer('atlas');
    await testServer('meow');
    await testServer('orion');
    await testServer('centaurus');
}

run();
