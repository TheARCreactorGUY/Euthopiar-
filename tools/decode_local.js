const fs = require('fs');
const vm = require('vm');

const data = fs.readFileSync('tools/Ads-IamQT1xR.js', 'utf8');

// 1. Array and rotator: from 0 to where import starts
const importIdx = data.indexOf('import{');
const rotator = data.slice(0, importIdx);

// 2. _0x3584d5 and _0x1ecd: from 19867 to 23200
const ecdIdx = data.indexOf('function _0x3584d5');
const ecdEnd = data.indexOf('function be({');
const ecdCode = data.slice(ecdIdx, ecdEnd);

const fullCode = rotator + '\n' + ecdCode;
console.log('fullCode length:', fullCode.length);

const context = { console };
vm.createContext(context);
vm.runInContext(fullCode, context);

const m = data.match(/const\s+re=([^(]+)\('t7Eq',-0x15b\),I=([^(]+)\('ZpwB',-0x179\);/);
if (m) {
  const fnName = m[1];
  console.log('fnName:', fnName);
  const reVal = context[fnName]('t7Eq', -0x15b);
  const IVal = context[fnName]('ZpwB', -0x179);
  console.log('re =', reVal);
  console.log('I =', IVal);
  
  // Let's decode all unique calls in data!
  const regex = new RegExp(fnName + "\\('([^']+)',\\s*(-?0x[0-9a-fA-F]+|-?[0-9]+)\\)", 'g');
  let match;
  const decoded = {};
  while ((match = regex.exec(data)) !== null) {
    try {
      const res = context[fnName](match[1], parseInt(match[2]));
      decoded[match[0]] = res;
    } catch (e) {}
  }
  console.log('Total decoded strings:', Object.keys(decoded).length);
  for (const [k, v] of Object.entries(decoded)) {
    if (v && (v.includes('http') || v.includes('api') || v.includes('server') || v.includes('wasm') || v.includes('movie') || v.includes('tv'))) {
      console.log(`  ${k} -> "${v}"`);
    }
  }
} else {
  console.log('No match for re and I');
}
