with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import struct

rela_off = 0xf9190
rela_sz = 0x41bb0
num = rela_sz // 24

# Let's search for addend == 0x206a18
for i in range(num):
    r_off, r_info, r_addend = struct.unpack_from("<QQq", data, rela_off + i * 24)
    r_type = r_info & 0xffffffff
    if r_addend == 0x206a18:
        print(f"Match for 0x206a18! off=0x{r_off:x}, type=0x{r_type:x}, addend=0x{r_addend:x}")
