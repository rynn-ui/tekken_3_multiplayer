with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

# Let's inspect around 0x37aa3d
pos = 0x37aa3d
print("Around 0x37aa3d:")
print(data[pos-50:pos+150].split(b'\x00'))

# Let's inspect around 0x212d4e
pos2 = 0x212d4e
print("\nAround 0x212d4e:")
print(data[pos2-50:pos2+150].split(b'\x00'))
