const https = require('https');

https.get('https://player.vidzee.wtf/assets/Ads-IamQT1xR.js', {
  headers: { 'Referer': 'https://player.vidzee.wtf/', 'User-Agent': 'Mozilla/5.0' }
}, (res) => {
  let data = '';
  res.on('data', chunk => data += chunk);
  res.on('end', () => {
    // Extract everything from beginning of file up to import
    const importIdx = data.indexOf('import{');
    const headerCode = data.slice(0, importIdx);
    
    // Evaluate headerCode in a sandbox
    const vm = require('vm');
    const context = {};
    vm.createContext(context);
    vm.runInContext(headerCode, context);
    
    // Now extract _0x3584d5 and print I and re
    console.log('Evaluated header code.');
    // Look at how re and I were called:
    // const re=_0x3584d5('t7Eq',-0x15b),I=_0x3584d5('ZpwB',-0x179);
    const m = data.match(/const\s+re=([^(]+)\('t7Eq',-0x15b\),I=([^(]+)\('ZpwB',-0x179\);/);
    if (m) {
      const fnName = m[1];
      const reVal = context[fnName]('t7Eq', -0x15b);
      const IVal = context[fnName]('ZpwB', -0x179);
      console.log('re =', reVal);
      console.log('I =', IVal);
      
      // Let's decode all calls to _0x3584d5 or fnName in the file!
      const regex = new RegExp(fnName + "\\('([^']+)',\\s*([^)]+)\\)", 'g');
      let match;
      const decodedMap = {};
      while ((match = regex.exec(data)) !== null) {
        try {
          const arg1 = match[1];
          const arg2 = eval(match[2]);
          const result = context[fnName](arg1, arg2);
          decodedMap[match[0]] = result;
        } catch(e) {}
      }
      console.log('Decoded calls count:', Object.keys(decodedMap).length);
      for (const [k, v] of Object.entries(decodedMap)) {
        if (v && (v.includes('http') || v.includes('api') || v.includes('server') || v.includes('embed') || v.includes('movie') || v.includes('tv') || v.includes('wasm'))) {
          console.log(`  ${k} -> "${v}"`);
        }
      }
    } else {
      console.log('Could not match re and I pattern');
    }
  });
});
