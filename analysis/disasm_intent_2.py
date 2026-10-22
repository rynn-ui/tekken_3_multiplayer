with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import capstone
cs = capstone.Cs(capstone.CS_ARCH_ARM64, capstone.CS_MODE_ARM)
cs.detail = True

def get_str(addr):
    if 0 <= addr < len(data):
        end = data.find(b"\x00", addr)
        if end != -1 and end - addr < 100:
            return repr(data[addr:end])
    return ""

start = 0x5ee2dc
end = 0x5eea00

current_adrp = {}
for ins in cs.disasm(data[start:end], start):
    extra = ""
    if ins.mnemonic == "adrp":
        reg = ins.operands[0].reg
        imm = ins.operands[1].imm
        current_adrp[reg] = imm
    elif ins.mnemonic == "add" and len(ins.operands) >= 3:
        rd = ins.operands[0].reg
        rn = ins.operands[1].reg
        if rn in current_adrp and ins.operands[2].type == capstone.arm64.ARM64_OP_IMM:
            target = current_adrp[rn] + ins.operands[2].imm
            s = get_str(target)
            if s:
                extra = f" // {s}"
    elif ins.mnemonic == "ldr" and len(ins.operands) >= 2:
        rn = ins.operands[1].mem.base
        disp = ins.operands[1].mem.disp
        if rn in current_adrp:
            target = current_adrp[rn] + disp
            s = get_str(target)
            if s:
                extra = f" // {s}"
    print(f"  0x{ins.address:08x}: {ins.mnemonic:8s} {ins.op_str:30s}{extra}")
