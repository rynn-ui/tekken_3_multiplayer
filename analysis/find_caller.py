with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import struct

# Let's find callers of 0x5ed8c0 or wherever the function starts.
# Let's find the function entry: scanning backwards from 0x5ed8c0
start = 0x5ed8c0
while start > 0x5ed000:
    word = struct.unpack_from("<I", data, start)[0]
    # Check for typical prologue: stp x29, x30, [sp, -xxx]!
    # stp x29, x30, [sp, ...] is (word & 0xffc003e0) == 0xa98003e0 or similar
    if (word & 0xffc003e0) == 0xa98003e0 or word == 0xd503233f: # paciasp
        print("Possible prologue at: 0x%x" % start)
        break
    start -= 4

func_start = start
print("Function entry point estimated at: 0x%x" % func_start)

# Scan for BL instructions to func_start
for i in range(0, 0xab7ab0, 4):
    word = struct.unpack_from("<I", data, i)[0]
    if (word & 0xfc000000) == 0x94000000: # BL
        imm26 = word & 0x03ffffff
        if imm26 & (1 << 25):
            imm26 -= (1 << 26)
        target = i + (imm26 << 2)
        if target == func_start:
            print(f"Call from 0x{i:x}")
