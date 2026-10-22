with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import capstone
cs = capstone.Cs(capstone.CS_ARCH_ARM64, capstone.CS_MODE_ARM)
cs.detail = True

for addr in [0x7741d0, 0x775310]:
    print(f"\n--- Disasm around 0x{addr:x} ---")
    for ins in cs.disasm(data[addr:addr+80], addr):
        print(f"  0x{ins.address:08x}: {ins.mnemonic:8s} {ins.op_str:30s}")
