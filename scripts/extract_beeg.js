const { execSync } = require('child_process');
const js = execSync('curl.exe -s --resolve beeg.com:443:104.21.56.86 https://beeg.com/dist/main.9653fb14.js', { maxBuffer: 30 * 1024 * 1024 }).toString();

const regex = /endpoints\.store[^;]{0,200}/g;
const matches = js.match(regex) || [];
console.log("Store calls:", matches);
