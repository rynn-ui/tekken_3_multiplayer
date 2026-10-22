with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

s = b"[ENV] Checking arguments passed from intent"
pos = data.find(s)
print("Exact match for string:", hex(pos))
if pos != -1:
    print("Preceding byte:", repr(data[pos-1:pos]))
    print("Full string:", repr(data[pos:data.find(b'\x00', pos)]))
