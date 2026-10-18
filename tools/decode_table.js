const fs = require('fs');

function _0x3f4d(f,n){f-=207;let t=_0x541b(),d=t[f];void 0===_0x3f4d.hEIjmK&&(_0x3f4d.QaCpTk=function(f){let n="",t="";for(let t=0,d,e,r=0;e=f.charAt(r++);~e&&(d=t%4?64*d+e:e,t++%4)&&(n+=String.fromCharCode(255&d>>(-2*t&6))))e="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/=".indexOf(e);for(let f=0,d=n.length;f<d;f++)t+="%"+("00"+n.charCodeAt(f).toString(16)).slice(-2);return decodeURIComponent(t)},_0x3f4d.JthhyC={},_0x3f4d.hEIjmK=!0);let e=f+t[0],r=_0x3f4d.JthhyC[e];return r?d=r:(d=_0x3f4d.QaCpTk(d),_0x3f4d.JthhyC[e]=d),d}

// Read the function _0x541b from the file
const code = fs.readFileSync('tools/inspect_viduki_chunk_code.js', 'utf8');
