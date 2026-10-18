const fs = require('fs');
const vm = require('vm');

const code = fs.readFileSync('scripts/script8.js', 'utf8');

const p1 = code.indexOf('function _0x29c2(');
const p2 = code.indexOf('function _0x4c76(');
const p3 = code.indexOf('}document[') + 1;

const fn29c2 = code.substring(p1, p2);
const fn4c76 = code.substring(p2, p3);

const endIdx = code.indexOf(',String[');
let iife = code.substring(0, endIdx);
iife = iife.replace(';(function', ';(function') + ');';

const sandbox = {};
vm.createContext(sandbox);

vm.runInContext(fn29c2 + '\n' + fn4c76 + '\n' + iife, sandbox);

let deobf = code;
const callRegex = /_0x(?:3d2b01|4c76|5a9ee3|518d00|3e92d4|4d5850|12d857|5dbd2b)\((0x[0-9a-fA-F]+)\)/g;
deobf = deobf.replace(callRegex, (match, hex) => {
    try {
        const val = vm.runInContext(`_0x4c76(${hex})`, sandbox);
        return JSON.stringify(val);
    } catch (e) {
        return match;
    }
});

fs.writeFileSync('scripts/script8_clean.js', deobf);
console.log('Deobfuscated file written to scripts/script8_clean.js');
