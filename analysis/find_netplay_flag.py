with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import struct
import capstone

cs = capstone.Cs(capstone.CS_ARCH_ARM64, capstone.CS_MODE_ARM)
cs.detail = True

target_va = 0xb9ac7d
target_page = 0xb9a000
target_off = 0xc7d

print("Scanning for all references to 0xb9ac7d (page 0xb9a000, off 0xc7d)...")
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
                # LDRB/LDRH/LDR or ADD
                # check if register matches and immediate matches 0xc7d or nearby (0xc7c, 0xc7e)
                # Let's disassemble the pair
                insns = list(cs.disasm(data[i:i+off+4], i))
                if len(insns) >= 2:
                    last = insns[-1]
                    if "0xc7d" in last.op_str or "0xc7c" in last.op_str or "0xc7e" in last.op_str:
                        print(f"Ref at 0x{i:x}:")
                        for ins in insns:
                            print(f"    0x{ins.address:08x}: {ins.mnemonic:8s} {ins.op_str}")
