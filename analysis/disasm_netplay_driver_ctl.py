with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import capstone
cs = capstone.Cs(capstone.CS_ARCH_ARM64, capstone.CS_MODE_ARM)
cs.detail = True

start = 0x5ebff4
size = 0x248

print(f"Disassembling netplay_driver_ctl (0x{start:x}):")
for ins in cs.disasm(data[start:start+size], start):
    print(f"  0x{ins.address:08x}: {ins.mnemonic:8s} {ins.op_str}")
