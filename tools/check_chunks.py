import re
import base64

with open(r'C:\Euthopiar\test_eups_repo\sources\CinejoyWasmEngine.kt', 'r', encoding='utf-8') as f:
    text = f.read()

m1 = re.search(r'B64_CHUNK1\s*=\s*"([^"]+)"', text)
m2 = re.search(r'B64_CHUNK2\s*=\s*"([^"]+)"', text)

c1 = m1.group(1) if m1 else ""
c2 = m2.group(1) if m2 else ""

print(f"C1 length: {len(c1)}, C2 length: {len(c2)}")
total = c1 + c2
print(f"Total b64 length: {len(total)}")
try:
    decoded = base64.b64decode(total)
    print(f"Decoded wasm length: {len(decoded)} bytes")
    print(f"Wasm magic bytes: {decoded[:4]}")
except Exception as e:
    print(f"B64 decode failed: {e}")
