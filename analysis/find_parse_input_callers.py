with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import struct

# Let's find function start of retroarch_parse_input
# Scanning backwards from 0x613c00
start = 0x613c00
while start > 0x612000:
    word = struct.unpack_from("<I", data, start)[0]
    if (word & 0xffc003e0) == 0xa98003e0 or word == 0xd503233f:
        print("Possible start:", hex(start))
        break
    start -= 4

func_start = start
print("retroarch_parse_input entry point:", hex(func_start))

# Now find calls to func_start
for i in range(0, 0xab7ab0, 4):
    word = struct.unpack_from("<I", data, i)[0]
    if (word & 0xfc000000) == 0x94000000: # BL
        imm26 = word & 0x03ffffff
        if imm26 & (1 << 25):
            imm26 -= (1 << 26)
        t = i + (imm26 << 2)
        if t == func_start:
            print(f"Call from 0x{i:x}")
