with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

import struct

# In RetroArch, config_file entries are structs:
# struct config_bool_setting { const char *ident; bool *target; bool def_value; ... }
# Let's find the string "netplay_mode" in .rodata
pos = data.find(b"\x00netplay_mode\x00")
if pos != -1:
    pos += 1
    print(f"'netplay_mode' at 0x{pos:x}")
    # Now find where this string address is referenced in the binary
    # It could be an offset in .data or .rodata
    b8 = struct.pack("<Q", pos)
    b4 = struct.pack("<I", pos)
    for i in range(len(data)-8):
        if data[i:i+8] == b8:
            print(f"64-bit reference at 0x{i:x}")
            # print surrounding 64 bytes
            print(" Context:", data[max(0, i-16):i+48].hex())
