with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import struct

target = 0x5ed700
b8 = struct.pack("<Q", target)

for i in range(len(data)-8):
    if data[i:i+8] == b8:
        print(f"64-bit pointer to 0x5ed700 at 0x{i:x}")

# Also check BL to anywhere between 0x5ed6f0 and 0x5ed710
for i in range(0, 0xab7ab0, 4):
    word = struct.unpack_from("<I", data, i)[0]
    if (word & 0xfc000000) == 0x94000000: # BL
        imm26 = word & 0x03ffffff
        if imm26 & (1 << 25):
            imm26 -= (1 << 26)
        t = i + (imm26 << 2)
        if 0x5ed6f0 <= t <= 0x5ed710:
            print(f"BL to 0x{t:x} at 0x{i:x}")
