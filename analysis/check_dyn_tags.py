with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import struct

e_phoff = struct.unpack_from("<Q", data, 0x20)[0]
e_phnum = struct.unpack_from("<H", data, 0x38)[0]

dyn_off = None
dyn_sz = None
for i in range(e_phnum):
    p_type, p_flags, p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_align = struct.unpack_from("<IIQQQQQQ", data, e_phoff + i * 56)
    if p_type == 2: # PT_DYNAMIC
        dyn_off = p_offset
        dyn_sz = p_filesz
        print(f"PT_DYNAMIC at offset 0x{dyn_off:x}, size 0x{dyn_sz:x}")

if dyn_off:
    num_tags = dyn_sz // 16
    for i in range(num_tags):
        d_tag, d_val = struct.unpack_from("<Qq", data, dyn_off + i * 16)
        if d_tag in (7, 8, 9, 23, 0x6000000f, 0x60000010, 0x6fffe000, 0x6fffe001):
            print(f"Tag 0x{d_tag:x}: val 0x{d_val:x}")
