with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

# Let's inspect strings around 0x4064a5
pos = 0x4064a5
print(data[pos-200:pos+300])
