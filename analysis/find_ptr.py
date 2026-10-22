with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import struct

target_va = 0x3ef348
b8 = struct.pack("<Q", target_va)
b4 = struct.pack("<I", target_va)

print("Searching for 64-bit ptr:", [hex(m.start()) for m in __import__("re").finditer(__import__("re").escape(b8), data)])
print("Searching for 32-bit ptr:", [hex(m.start()) for m in __import__("re").finditer(__import__("re").escape(b4), data)])
