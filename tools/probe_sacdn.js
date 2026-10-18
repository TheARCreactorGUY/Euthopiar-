const https = require('https');

const initUrl = 'https://sacdn.hakunaymatata.com/dash/5246367121529078256_0_0_1080_h265_410/init-stream0.m4s?Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9zYWNkbi5oYWt1bmF5bWF0YXRhLmNvbS9kYXNoLzUyNDYzNjcxMjE1MjkwNzgyNTZfMF8wXzEwODBfaDI2NV80MTAvKiIsIkNvbmRpdGlvbiI6eyJEYXRlTGVzc1RoYW4iOnsiQVdTOkVwb2NoVGltZSI6MTc5MTk4NTU5M319fV19&Signature=tun4rTwV3FECcB6OYvpkjv7ZZEoWURY-pT4lbKpyBeyd8sv4ZwxbK64Wwmf4hHExOhPkxtdXQA9gCIX7qRmfLqKznQVoOrtUq1LuF52QQJ2Sy5C7p9owrQJYJBvR%7EBCBk0N4XqYCU3XgosOLws7tOmNPv9-YLfxzhpBQ0odb';

const req = https.request(initUrl, {
    method: 'GET',
    headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Referer': 'https://vidstuck.xyz/'
    }
}, res => {
    console.log('Status:', res.statusCode);
    console.log('Headers:', res.headers);
    let chunks = [];
    res.on('data', c => chunks.push(c));
    res.on('end', () => {
        const buf = Buffer.concat(chunks);
        console.log('Length:', buf.length);
        console.log('First 32 bytes (hex):', buf.subarray(0, 32).toString('hex'));
        console.log('First 32 bytes (ascii):', buf.subarray(0, 32).toString('ascii'));
    });
});
req.on('error', e => console.error('Error:', e));
req.end();
