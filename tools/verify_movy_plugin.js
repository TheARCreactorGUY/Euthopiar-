const movy = require("./movy.js");

async function run() {
  console.log("=== Testing Movy TV Series (Lanterns S1E1, TMDB 95350) ===");
  const t0 = Date.now();
  const tvStreams = await movy.getStreams(95350, "tv", 1, 1);
  const t1 = Date.now();
  console.log(`Resolved ${tvStreams.length} streams in ${t1 - t0}ms`);
  if (tvStreams.length > 0) {
    console.log("Sample TV Stream #1:", {
      name: tvStreams[0].name,
      quality: tvStreams[0].quality,
      type: tvStreams[0].type,
      url: tvStreams[0].url.substring(0, 60) + "...",
      subtitlesCount: (tvStreams[0].subtitles || []).length
    });
  }

  console.log("\n=== Testing Movy Movie (Fight Club, TMDB 550) ===");
  const t2 = Date.now();
  const movieStreams = await movy.getStreams(550, "movie", null, null);
  const t3 = Date.now();
  console.log(`Resolved ${movieStreams.length} streams in ${t3 - t2}ms`);
  if (movieStreams.length > 0) {
    console.log("Sample Movie Stream #1:", {
      name: movieStreams[0].name,
      quality: movieStreams[0].quality,
      type: movieStreams[0].type,
      url: movieStreams[0].url.substring(0, 60) + "...",
      subtitlesCount: (movieStreams[0].subtitles || []).length
    });
  }

  console.log("\n[SUCCESS] Both tests passed.");
}

run().catch(console.error);
