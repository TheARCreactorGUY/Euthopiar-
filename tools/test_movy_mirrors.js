const D = [
  0x428a2f98, 0x71374491, 0xb5c0fbcf >>> 0, 0xe9b5dba5 >>> 0,
  0x3956c25b, 0x59f111f1, 0x923f82a4 >>> 0, 0xab1c5ed5 >>> 0,
  0xd807aa98 >>> 0, 0x12835b01, 0x243185be, 0x550c7dc3,
  0x72be5d74, 0x80deb1fe >>> 0, 0x9bdc06a7 >>> 0, 0xc19bf174 >>> 0
];

function o(e) {
  e = e >>> 0;
  let x = (e ^ (e >>> 16)) >>> 0;
  x = Math.imul(x, 0x85ebca6b) >>> 0;
  x = (x ^ (x >>> 13)) >>> 0;
  x = Math.imul(x, 0xc2b2ae35) >>> 0;
  return (x ^ (x >>> 16)) >>> 0;
}

function u(e, a) {
  e = e >>> 0;
  const shift = a & 31;
  if (shift === 0) return e;
  return ((e << shift) | (e >>> (32 - shift))) >>> 0;
}

function decrypt(encB64, seedStr, mediaId) {
  let normalized = encB64.trim().replace(/-/g, '+').replace(/_/g, '/');
  const padLen = (4 - (normalized.length % 4)) % 4;
  normalized += '='.repeat(padLen);
  const binaryStr = atob(normalized);
  const t = binaryStr.length;
  const raw = new Uint8Array(t);
  for (let i = 0; i < t; i++) raw[i] = binaryStr.charCodeAt(i);

  const sValues = new Uint32Array(61);
  const sPresent = new Uint8Array(61);
  let h = 0x811c9dc5 >>> 0;
  for (let i = 0; i < seedStr.length; i++) h = Math.imul((h ^ seedStr.charCodeAt(i)) >>> 0, 0x1000193) >>> 0;

  const mediaIdLow32 = Number(BigInt(mediaId) & 0xFFFFFFFFn) >>> 0;
  let n = o((o(h) ^ o((mediaIdLow32 ^ (0x9e3779b9 >>> 0)) >>> 0)) >>> 0);

  for (let e = 0; e < 8; e++) {
    if (((e * (e + 1)) & 1) === 0) {
      const a = n % 61;
      n = u((n + (0x9e3779b9 >>> 0)) >>> 0, 7 + (7 & e));
      sValues[a] = (n ^ o(n)) >>> 0;
      sPresent[a] = 1;
      n = o((n + a) >>> 0);
    } else {
      sValues[e] = D[15 & e];
      sPresent[e] = 1;
    }
  }

  let acc = o(((0xa5a5a5a5 >>> 0) ^ n) >>> 0);
  const keystream = new Uint8Array(t);
  let lCounter = 0;
  let eIdx = 0;
  while (eIdx < t) {
    const dVal = acc % 61;
    const isPresent = sPresent[dVal];
    const iFlag = isPresent ? 0xffffffff : 0;
    const rVal = isPresent ? sValues[dVal] : 0;
    const cVal = Math.imul(0x9e3779b9 >>> 0, (lCounter + 1)) >>> 0;
    lCounter++;
    const tXor = acc;
    const sXor = (rVal ^ cVal) >>> 0;
    const bVal = ((tXor ^ sXor) | ((tXor & sXor & iFlag) >>> 0)) >>> 0;
    const rot1 = u((bVal + acc) >>> 0, 31 & dVal);
    const rot2 = u(acc, 31 & (dVal * 7));
    acc = o(((rot1 ^ rot2) + (0x9e3779b9 >>> 0)) >>> 0);
    sValues[dVal] = acc;
    sPresent[dVal] = 1;
    const kw = acc;

    keystream[eIdx++] = kw & 0xFF;
    if (eIdx < t) keystream[eIdx++] = (kw >>> 8) & 0xFF;
    if (eIdx < t) keystream[eIdx++] = (kw >>> 16) & 0xFF;
    if (eIdx < t) keystream[eIdx++] = (kw >>> 24) & 0xFF;
  }

  for (let i = 0; i < t; i++) raw[i] = raw[i] ^ keystream[i];
  if (raw[0] !== 109 || raw[1] !== 118 || raw[2] !== 109 || raw[3] !== 49) return null;
  return new TextDecoder().decode(raw.subarray(4));
}

const mirrors = [
  'miami', 'boise', 'austin', 'atlanta', 'seattle', 'denver',
  'phoenix', 'portland', 'dallas', 'tampa', 'orlando',
  'delhi', 'paris', 'munich', 'berlin', 'cancun'
];

async function checkMirrors() {
  const seedResp = await fetch('https://api.wecollege.net/seed?mediaId=95350', {
    headers: { 'Referer': 'https://www.movy.sx/', 'Origin': 'https://www.movy.sx' }
  });
  const { seed } = await seedResp.json();
  console.log('Seed:', seed);

  const results = await Promise.allSettled(mirrors.map(async m => {
    const url = 'https://api.wecollege.net/' + m + '/sources?title=Lanterns&mediaType=tv&year=2026&tmdbId=95350&seasonId=1&episodeId=1&totalSeasons=1&enc=2&seed=' + encodeURIComponent(seed);
    const r = await fetch(url, {
      headers: { 'Referer': 'https://www.movy.sx/', 'Origin': 'https://www.movy.sx' }
    });
    if (!r.ok) return { mirror: m, ok: false, status: r.status };
    const enc = await r.text();
    const dec = decrypt(enc, seed, 95350);
    if (!dec) return { mirror: m, ok: false, reason: 'decrypt_failed' };
    const parsed = JSON.parse(dec);
    const count = (parsed.sources || []).length + (parsed.playlist ? 1 : 0);
    return { mirror: m, ok: true, streamCount: count, subs: (parsed.subtitles || []).length };
  }));

  for (const r of results) {
    if (r.status === 'fulfilled') {
      const v = r.value;
      console.log(v.mirror + ': ' + (v.ok ? `SUCCESS (${v.streamCount} streams, ${v.subs} subs)` : `FAILED (${v.status || v.reason})`));
    } else {
      console.log('Error:', r.reason);
    }
  }
}
checkMirrors();
