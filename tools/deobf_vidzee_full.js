const fs = require('fs');
const vm = require('vm');

const data = fs.readFileSync('tools/Ads-IamQT1xR.js', 'utf8');
const importIdx = data.indexOf('import{');
const rotator = data.slice(0, importIdx);
const ecdIdx = data.indexOf('function _0x3584d5');
const ecdEnd = data.indexOf('function be({');
const ecdCode = data.slice(ecdIdx, ecdEnd);
const fullCode = rotator + '\n' + ecdCode;

const context = { console };
vm.createContext(context);
vm.runInContext(fullCode, context);

// Replace all calls in Ads-IamQT1xR.js with their decoded string literals!
let deobf = data;

// Find all functions that wrap _0x3584d5:
// e.g. function _0x3e952c(_0x134b3a,_0x1f3f3e){return _0x3584d5(_0x1f3f3e,_0x134b3a-0x75f);}
const wrappers = [];
const wRegex = /function\s+(_0x[a-f0-9]+)\(([^)]+)\)\{\s*return\s+_0x3584d5\(([^)]+)\);\s*\}/g;
let wm;
while ((wm = wRegex.exec(data)) !== null) {
  wrappers.push({ name: wm[1], code: wm[0] });
}
console.log('Found wrappers:', wrappers.length);

// Let's decode calls for each wrapper
for (const w of wrappers) {
  vm.runInContext(w.code, context);
  const callRegex = new RegExp(w.name + "\\(([^)]+)\\)", 'g');
  let cm;
  while ((cm = callRegex.exec(data)) !== null) {
    try {
      const val = vm.runInContext(cm[0], context);
      deobf = deobf.split(cm[0]).join(JSON.stringify(val));
    } catch(e) {}
  }
}

// Write deobfuscated JS
fs.writeFileSync('tools/Ads_deobfuscated.js', deobf, 'utf8');
console.log('Saved tools/Ads_deobfuscated.js');

// Now let's print functions ne, ae, oe from deobf!
for (const fn of ['async function ne', 'async function ae', 'async function oe']) {
  const idx = deobf.indexOf(fn);
  if (idx !== -1) {
    console.log(`\n=== ${fn} ===`);
    console.log(deobf.slice(idx, idx + 1200));
  }
}
