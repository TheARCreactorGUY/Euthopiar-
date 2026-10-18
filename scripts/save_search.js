const https = require('https');
const fs = require('fs');

https.get('https://4khdhub.one/?s=punisher', { headers: { 'User-Agent': 'Mozilla/5.0' } }, res => {
    let d = '';
    res.on('data', c => d += c);
    res.on('end', () => {
        fs.writeFileSync('scripts/search_punisher.html', d);
        console.log('Saved search_punisher.html, length:', d.length);
    });
});
