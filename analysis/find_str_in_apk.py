import os

search_str = b"Checking arguments passed from intent"
for root, dirs, files in os.walk("analysis/apktool"):
    for f in files:
        p = os.path.join(root, f)
        try:
            with open(p, "rb") as fp:
                c = fp.read()
                if search_str in c:
                    print(f"Found in: {p} ({len(c)} bytes)")
        except Exception:
            pass
