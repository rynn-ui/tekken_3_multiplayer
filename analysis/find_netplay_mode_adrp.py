with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import struct
import capstone

cs = capstone.Cs(capstone.CS_ARCH_ARM64, capstone.CS_MODE_ARM)
cs.detail = True

target_va = 0x206a18
target_page = target_va & ~0xfff
target_off = target_va & 0xfff

print("Scanning for ADRP+ADD to 0x%x (page 0x%x, off 0x%x)..." % (target_va, target_page, target_off))
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
            for off in [4, 8, 12, 16]:
                if i + off + 4 > 0xab7ab0: break
                w2 = struct.unpack_from("<I", data, i + off)[0]
                if (w2 & 0x11c00000) == 0x11000000:
                    rn = (w2 >> 5) & 0x1f
                    imm12 = (w2 >> 10) & 0xfff
                    if rn == rd and imm12 == target_off:
                        print(f"FOUND MATCH AT 0x{i:x}!")
                        for ins in cs.disasm(data[i-16:i+100], i-16):
                            print(f"  0x{ins.address:08x}: {ins.mnemonic:8s} {ins.op_str}")
                        break
