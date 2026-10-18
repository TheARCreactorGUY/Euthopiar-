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

const ueIdx = data.indexOf('ue=_0x4eb450=>[');
const ueEnd = data.indexOf('],de=');
const ueCode = data.slice(ueIdx, ueEnd + 1);

console.log('ue code raw:', ueCode);

// Let's execute ue(['Hindi', 'English', 'Bengali']) in context
vm.runInContext(ueCode, context);
const servers = context.ue(['Hindi', 'English', 'Bengali']);
console.log('VIDZEE SERVERS FOR HINDI/ENGLISH/BENGALI:');
console.log(JSON.stringify(servers, null, 2));
