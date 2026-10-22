import subprocess
import re

# Let's find callers of netplay_new or references to netplay_enable / netplay_mode
with open(r"analysis\apktool\lib\arm64-v8a\libretroarch-activity.so", "rb") as f:
    data = f.read()

# Let's search for netplay strings and config keys
keys = [
    b"netplay_enable",
    b"netplay_mode",
    b"netplay_ip_address",
    b"netplay_tcp_udp_port",
    b"netplay_start_as_spectator",
    b"netplay_stateless_mode",
    b"RARCH_NETPLAY_CTL_INIT",
    b"--host",
    b"-H",
    b"--connect",
    b"-C",
    b"netplay_driver",
]

for k in keys:
    matches = [m.start() for m in re.finditer(re.escape(k), data)]
    print(f"Key {k}: found at offsets {[hex(x) for x in matches]}")
