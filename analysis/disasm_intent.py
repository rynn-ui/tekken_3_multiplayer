import capstone
from capstone import arm64

with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

# Target string address: 0x3ef348 (file offset = VA in non-PIE or check ELF load base)
# Let's check ELF load base first
import struct
# In 64-bit ELF, read program headers
e_phoff = struct.unpack_from("<Q", data, 0x20)[0]
e_phnum = struct.unpack_from("<H", data, 0x38)[0]
print("Program headers:", e_phnum, "at", hex(e_phoff))

load_segments = []
for i in range(e_phnum):
    p_type, p_flags, p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_align = struct.unpack_from("<IIQQQQQQ", data, e_phoff + i * 56)
    if p_type == 1: # PT_LOAD
        load_segments.append((p_offset, p_filesz, p_vaddr, p_memsz, p_flags))
        print(f"LOAD: offset 0x{p_offset:x}-0x{p_offset+p_filesz:x} -> VA 0x{p_vaddr:x}-0x{p_vaddr+p_memsz:x} flags {p_flags}")

# For typical Android .so, VA == file offset (or base 0)
str_va = 0x3ef348
print("Target string VA:", hex(str_va))

# Now let's scan executable sections for ADRP + ADD pointing to str_va (or near it)
target_page = str_va & ~0xfff
target_off = str_va & 0xfff

cs = capstone.Cs(capstone.CS_ARCH_ARM64, capstone.CS_MODE_ARM)
cs.detail = True

# We can scan the text segment
# Let's find the text segment from load segments
text_seg = [s for s in load_segments if s[4] & 1][0] # executable
text_off, text_sz, text_va = text_seg[0], text_seg[1], text_seg[2]
print(f"Text segment: offset 0x{text_off:x}, size 0x{text_sz:x}, va 0x{text_va:x}")

code_bytes = data[text_off:text_off+text_sz]

# Let's do a fast scan for instructions
# ADRP rd, target_page: ADRP instruction encoding
# Instead of disassembling all 4MB, let's look for candidate ADRP instructions
# ADRP has op0=1, op=00, bits [31]=1, [28:24]=10000
matches = []
for i in range(0, len(code_bytes)-8, 4):
    pc = text_va + i
    word = struct.unpack_from("<I", code_bytes, i)[0]
    if (word & 0x9f000000) == 0x90000000: # ADRP
        # calculate imm
        immlo = (word >> 29) & 3
        immhi = (word >> 5) & 0x7ffff
        imm = (immhi << 2) | immlo
        if imm & (1 << 20):
            imm -= (1 << 21)
        base = (pc & ~0xfff) + (imm << 12)
        if base == target_page:
            # check next instruction
            w2 = struct.unpack_from("<I", code_bytes, i+4)[0]
            # ADD Xd, Xn, #imm12: (w2 & 0xffc00000) == 0x91000000
            if (w2 & 0x11c00000) == 0x11000000: # ADD
                imm12 = (w2 >> 10) & 0xfff
                if imm12 == target_off:
                    matches.append((pc, hex(pc)))

print("Matches pointing to string:", matches)

if matches:
    func_pc = matches[0][0]
    # Disassemble around func_pc
    start_dis = max(text_va, func_pc - 100)
    end_dis = min(text_va + text_sz, func_pc + 1000)
    dis_bytes = code_bytes[start_dis - text_va : end_dis - text_va]
    for ins in cs.disasm(dis_bytes, start_dis):
        print(f"0x{ins.address:08x}: {ins.mnemonic:8s} {ins.op_str}")
