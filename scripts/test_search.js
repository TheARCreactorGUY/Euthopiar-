const https = require('https');

https.get('https://4khdhub.one/?s=punisher', { headers: { 'User-Agent': 'Mozilla/5.0' } }, res => {
    let d = '';
    res.on('data', c => d += c);
    res.on('end', () => {
        console.log('Search status:', res.statusCode, 'Length:', d.length);
        const cardRegex = /<a[^>]*class=["']movie-card["'][^>]*href=["']([^"']+)["']/gi;
        let m;
        while ((m = cardRegex.exec(d)) !== null) {
            console.log('Found Card:', m[1]);
        }
    });
});
