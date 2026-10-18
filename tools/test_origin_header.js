const https = require('https');

const initUrl = 'https://sacdn.hakunaymatata.com/dash/5246367121529078256_0_0_1080_h265_410/init-stream0.m4s?Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9zYWNkbi5oYWt1bmF5bWF0YXRhLmNvbS9kYXNoLzUyNDYzNjcxMjE1MjkwNzgyNTZfMF8wXzEwODBfaDI2NV80MTAvKiIsIkNvbmRpdGlvbiI6eyJEYXRlTGVzc1RoYW4iOnsiQVdTOkVwb2NoVGltZSI6MTc5MTk4NTU5M319fV19&Signature=tun4rTwV3FECcB6OYvpkjv7ZZEoWURY-pT4lbKpyBeyd8sv4ZwxbK64Wwmf4hHExOhPkxtdXQA9gCIX7qRmfLqKznQVoOrtUq1LuF52QQJ2Sy5C7p9owrQJYJBvR%7EBCBk0N4XqYCU3XgosOLws7tOmNPv9-YLfxzhpBQ0odbNunQwvRpHpCfFD2WvWilswTDuk-JT4W7GHuXtLKGYK33jFCiJz1v%7EVZ3g6KlGjnI2Tji96Tm3-YZPy0hBRnloLN2oVtu8zo2lgJkkyLyo-xyZNawj1kXsbBSNG4-vMln74fSyo3swDYx8Lf0onshXmmSqg7xM9PtmSoV0pPG59nIbQ__&Key-Pair-Id=KMHN1LQ1HEUPL';

// Test 1: with Origin and exact embed Referer
console.log('Testing with Origin and exact embed Referer:');
const req1 = https.request(initUrl, {
    method: 'GET',
    headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
        'Referer': 'https://vidstuck.xyz/embed/movie/1022789',
        'Origin': 'https://vidstuck.xyz'
    }
}, res => {
    console.log('Test 1 Status:', res.statusCode);
    console.log('Test 1 Headers:', res.headers);
    let b = []; res.on('data', c => b.push(c));
    res.on('end', () => console.log('Test 1 Length:', Buffer.concat(b).length));
});
req1.end();
