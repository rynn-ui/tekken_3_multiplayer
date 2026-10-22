with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import capstone
cs = capstone.Cs(capstone.CS_ARCH_ARM64, capstone.CS_MODE_ARM)
cs.detail = True

def inspect(start, count=30):
    print(f"\n--- Disassembly at 0x{start:x} ---")
    for ins in cs.disasm(data[start:start+count*4], start):
        print(f"  0x{ins.address:08x}: {ins.mnemonic:8s} {ins.op_str}")

inspect(0x6140e0, 25)
inspect(0x60a170, 25)
inspect(0x61b930, 25)
