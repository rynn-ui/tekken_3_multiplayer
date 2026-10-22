import struct

with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

# Check dynamic relocations
# In 64-bit ELF, Elf64_Rela is 24 bytes: r_offset (8), r_info (8), r_addend (8)
e_shoff = struct.unpack_from("<Q", data, 0x28)[0]
e_shentsize = struct.unpack_from("<H", data, 0x3a)[0]
e_shnum = struct.unpack_from("<H", data, 0x3c)[0]
e_shstrndx = struct.unpack_from("<H", data, 0x3e)[0]
shstr_offset = struct.unpack_from("<Q", data, e_shoff + e_shstrndx * e_shentsize + 0x18)[0]

def get_str(off):
    end = data.find(b"\x00", off)
    return data[off:end].decode("ascii", errors="ignore")

for i in range(e_shnum):
    hdr = e_shoff + i * e_shentsize
    sh_name, sh_type, sh_flags, sh_addr, sh_offset, sh_size, sh_link, sh_info, sh_addralign, sh_entsize = struct.unpack_from("<IIQQQQIIQQ", data, hdr)
    name = get_str(shstr_offset + sh_name)
    if "rela" in name:
        print(f"Relocation section {name}: off=0x{sh_offset:x}, size=0x{sh_size:x}, entsize=0x{sh_entsize:x}")
        # Search for addend == 0x3ef348
        count = sh_size // sh_entsize
        for j in range(count):
            r_off, r_info, r_addend = struct.unpack_from("<QQq", data, sh_offset + j * sh_entsize)
            if r_addend == 0x3ef348:
                print(f"Found relocation in {name}: offset=0x{r_off:x}, info=0x{r_info:x}, addend=0x{r_addend:x}")
