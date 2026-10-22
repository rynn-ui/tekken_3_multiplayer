import struct

with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

e_shoff = struct.unpack_from("<Q", data, 0x28)[0]
e_shentsize = struct.unpack_from("<H", data, 0x3a)[0]
e_shnum = struct.unpack_from("<H", data, 0x3c)[0]
e_shstrndx = struct.unpack_from("<H", data, 0x3e)[0]

shstr_hdr = e_shoff + e_shstrndx * e_shentsize
shstr_offset = struct.unpack_from("<Q", data, shstr_hdr + 0x18)[0]

def get_str(off):
    end = data.find(b"\x00", off)
    return data[off:end].decode("ascii", errors="ignore")

for i in range(e_shnum):
    hdr = e_shoff + i * e_shentsize
    sh_name, sh_type, sh_flags, sh_addr, sh_offset, sh_size = struct.unpack_from("<IIQQQQ", data, hdr)
    name = get_str(shstr_offset + sh_name)
    if sh_offset <= 0x3ef348 < sh_offset + sh_size:
        print(f"File offset 0x3ef348 is in section '{name}' (addr=0x{sh_addr:x}, off=0x{sh_offset:x}, size=0x{sh_size:x})")
        va = sh_addr + (0x3ef348 - sh_offset)
        print(f"Calculated VA = 0x{va:x}")
