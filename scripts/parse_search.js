const fs = require('fs');
const html = fs.readFileSync('scripts/search_punisher.html', 'utf8');

const regex = /<a[^>]*href=["']([^"']+)["'][^>]*class=["']([^"']*)["'][^>]*>/gi;
let m;
while ((m = regex.exec(html)) !== null) {
    if (m[1].includes('punisher') || m[2].includes('card') || m[1].includes('-movie-') || m[1].includes('-series-')) {
        console.log('Match href:', m[1], 'class:', m[2]);
    }
}

// Check without class constraint
const movieLinkRegex = /<a[^>]*href=["']([^"']*(?:-movie-|-series-)[^"']*)["'][^>]*>/gi;
while ((m = movieLinkRegex.exec(html)) !== null) {
    console.log('Movie/Series link:', m[1]);
}
