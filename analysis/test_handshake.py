import socket
import binascii

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.settimeout(5.0)
try:
    s.connect(("127.0.0.1", 55435))
    print("Connected to RetroArch netplay port 55435!")
    data = s.recv(1024)
    print(f"Received {len(data)} bytes: {binascii.hexlify(data).decode('ascii')}")
    print("Raw bytes repr:", repr(data))
except Exception as e:
    print(f"Connection/read error: {e}")
finally:
    s.close()
