const https = require('https');

const apiKey = process.env.TMDB_API_KEY || '';
const url = `https://api.themoviedb.org/3/search/tv?api_key=${apiKey}&query=Marvel%27s+The+Punisher&first_air_date_year=2017`;

https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, res => {
    let d = '';
    res.on('data', c => d += c);
    res.on('end', () => {
        const json = JSON.parse(d);
        const item = json.results?.[0];
        console.log('TMDB Match:', item?.name, 'ID:', item?.id, 'Rating:', item?.vote_average, 'Backdrop:', item?.backdrop_path);
        
        if (item?.id) {
            https.get(`https://api.themoviedb.org/3/tv/${item.id}/season/1?api_key=${apiKey}`, { headers: { 'User-Agent': 'Mozilla/5.0' } }, sRes => {
                let sd = '';
                sRes.on('data', c => sd += c);
                sRes.on('end', () => {
                    const sJson = JSON.parse(sd);
                    console.log('Season 1 episodes:', sJson.episodes?.length);
                    for (const ep of (sJson.episodes || []).slice(0, 3)) {
                        console.log(`  E${ep.episode_number}: "${ep.name}" | Still: ${ep.still_path}`);
                    }
                });
            });
        }
    });
});
