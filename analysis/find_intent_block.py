with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

s = b"Checking arguments passed from intent"
idx = data.find(s)
print("Checking arguments at: 0x%x" % idx)

# Let's inspect the entire string block around this
start = max(0, idx - 100)
end = min(len(data), idx + 500)
print(data[start:end].split(b'\x00'))
