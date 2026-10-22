with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import struct

# Table at 0x158118
tbl_off = 0x158118
print("Jump table bytes at 0x158118:")
entries = list(data[tbl_off:tbl_off+16])
print(entries)

for i, e in enumerate(entries[:8]):
    cmd = i + 4
    target = 0x5ec08c + (e << 2)
    print(f"Cmd {cmd}: offset +0x{e*4:x} -> target 0x{target:x}")
