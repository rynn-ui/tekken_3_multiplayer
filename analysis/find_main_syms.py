with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import struct

e_shoff = struct.unpack_from("<Q", data, 0x28)[0]
e_shentsize = struct.unpack_from("<H", data, 0x3a)[0]
e_shnum = struct.unpack_from("<H", data, 0x3c)[0]
e_shstrndx = struct.unpack_from("<H", data, 0x3e)[0]
shstr_offset = struct.unpack_from("<Q", data, e_shoff + e_shstrndx * e_shentsize + 0x18)[0]

def get_str(base, off):
    end = data.find(b"\x00", base + off)
    return data[base + off:end].decode("ascii", errors="ignore")

for i in range(e_shnum):
    hdr = e_shoff + i * e_shentsize
    sh_name, sh_type, sh_flags, sh_addr, sh_offset, sh_size, sh_link, sh_info, sh_addralign, sh_entsize = struct.unpack_from("<IIQQQQIIQQ", data, hdr)
    name = get_str(shstr_offset, sh_name)
    if name == ".dynsym":
        strtab_hdr = e_shoff + sh_link * e_shentsize
        strtab_off = struct.unpack_from("<Q", data, strtab_hdr + 0x18)[0]
        num_syms = sh_size // sh_entsize
        for j in range(num_syms):
            sym_off = sh_offset + j * sh_entsize
            st_name, st_info, st_other, st_shndx, st_value, st_size = struct.unpack_from("<IBBHQQ", data, sym_off)
            sym_name = get_str(strtab_off, st_name)
            if "main" in sym_name.lower():
                print(f"  {sym_name}: value=0x{st_value:x}, size=0x{st_size:x}")
