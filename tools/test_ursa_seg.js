const https = require('https');

const segUrl = 'https://fragrant-scene-eda6.24-4d4.workers.dev/a?y=k81E7XGHydr8csdzRnA5zTrV8ZtsRQhk-LMRWoMIkD8OI3D7XxwjHNZAmaI2zrYYt5kPgW4cKNp7a-Hguq1FM4rYghNzgGbrIUlWlK9wnsdr7-3ZsFcIc_54CabE&h=K8vs';

https.get(segUrl, {
    headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Referer': 'https://vidstuck.xyz/'
    }
}, res => {
    console.log('Ursa Seg Status:', res.statusCode);
    console.log('Ursa Seg Headers:', res.headers);
    let chunks = [];
    res.on('data', c => chunks.push(c));
    res.on('end', () => {
        const buf = Buffer.concat(chunks);
        console.log('Ursa Seg Length:', buf.length);
        console.log('Ursa First 16 bytes (hex):', buf.subarray(0, 16).toString('hex'));
        console.log('Ursa First byte 0x47 (sync byte for TS)?', buf[0] === 0x47);
        console.log('Ursa Contains ftyp/moof?', buf.includes(Buffer.from('ftyp')) || buf.includes(Buffer.from('moof')));
    });
});
