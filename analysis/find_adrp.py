with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import struct

target_page = 0x3ef000
for i in range(0, 0xab7ab0, 4):
    word = struct.unpack_from("<I", data, i)[0]
    if (word & 0x9f000000) == 0x90000000: # ADRP
        immlo = (word >> 29) & 3
        immhi = (word >> 5) & 0x7ffff
        imm = (immhi << 2) | immlo
        if imm & (1 << 20):
            imm -= (1 << 21)
        base = (i & ~0xfff) + (imm << 12)
        if base == target_page:
            rd = word & 0x1f
            print(f"ADRP x{rd} at 0x{i:x}")
            # inspect following 10 instructions
            import capstone
            cs = capstone.Cs(capstone.CS_ARCH_ARM64, capstone.CS_MODE_ARM)
            for ins in cs.disasm(data[i:i+40], i):
                print(f"  0x{ins.address:08x}: {ins.mnemonic:8s} {ins.op_str}")
