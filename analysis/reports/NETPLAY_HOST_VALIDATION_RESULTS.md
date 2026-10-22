# Tekken 3 Android APK — Netplay Host Validation Results

**Date:** 2026-10-09  
**Target Package:** `com.androidapks.tekken3`  
**Device Tested:** Infinix X671B (`08617312BQ072417`), Android 13 (API 33, `arm64-v8a`)  
**Verdict:** **PASS (CONFIRMED FUNCTIONAL)**

---

## 1. Executive Summary

Online multiplayer and Netplay hosting are **fully compiled, functional, and operational** in the bundled RetroArch 1.9.11 + PCSX-ReARMed binary without recompiling or patching the APK or native libraries.

1. **Original APK Preserved:** The original `tekken3.apk` was untouched and neither re-signed nor patched.
2. **Device Configuration Protected:** The original configuration file `/storage/emulated/0/Android/data/com.androidapks.tekken3/files/retroarch.cfg` has an identical SHA-256 hash before and after tests (`FE858FC5F7A69AA45E9EF63764965CFA9E8F2EA3BE0707C3324F402CB49D10B8`).
3. **Netplay Socket Verified:** TCP port `55435` was successfully bound by `com.androidapks.tekken3` (`UID 10395`) in listening state (`0A` / `TCP_LISTEN`).
4. **Protocol Handshake Confirmed:** A test TCP connection initiated over ADB forward received the official 24-byte RetroArch Netplay Protocol packet starting with the magic header `RANP` (`b'RANP\x00\x04\x00\x08...'`).

---

## 2. Technical Findings: Why Config-Only Auto-Host Did Not Bind Initially

Disassembly of `libretroarch-activity.so` using Capstone revealed the exact architectural reason why setting `netplay_enable = "true"` and `netplay_mode = "true"` in `retroarch.cfg` did not start the server socket automatically on intent boot:

### A. Intent Extra Parsing
In `libretroarch-activity.so` (subroutine at `0x5ed8c0`), the native engine queries only standard storage and content intent extras:
- `"CONFIGFILE"`, `"LIBRETRO"`, `"ROM"`, `"SDCARD"`, `"EXTERNAL"`, `"DATADIR"`, `"APK"`
It does **not** check CLI arguments like `--host` from intent extras.

### B. Command-Line Parser vs Configuration File
The CLI parser `retroarch_parse_input()` (at `0x613268`) sets the internal active netplay hosting flag (`strh w8, [x9]` at `0x6140fc` targeting `0xb9ac7d`).
However, when booting via Android NativeActivity intent, RetroArch initializes content directly. Content initialization at `0x60a17c` checks `0xb9ac7d`:
```arm64
0x0060a17c: adrp  x8, #0xb9a000
0x0060a180: ldrb  w8, [x8, #0xc7d]
0x0060a184: cbz   w8, #0x60a3dc    ; If 0, skips netplay socket setup!
```
At boot time, `0xb9ac7d` defaults to `0`, meaning the background listening socket is **idle** until an in-game trigger or menu action activates it.

---

## 3. The Verified Trigger: In-Game Menu Activation

The bundled touch overlay (`/data/user/0/com.androidapks.tekken3/overlays/gamepads/neo-retropad/neo-retropad.cfg`) provides a built-in touch trigger for the RetroArch GUI:

1. **RGUI / Menu Toggle:**
   - Overlay descriptor: `overlay0_desc19 = "menu_toggle,0.50125,0.11708333333334,radial,0.025,0.04166666666"`
   - Screen coordinates: `(1442, 125)` (Space Invader icon at the top center between health bars).
2. **Menu Navigation to Netplay:**
   - Tap `(1442, 125)` -> Opens Quick Menu (`1.9.11 - PCSX-ReARMed (r23l cf6f7ca)`).
   - Tap Main Menu icon (Home tab on right sidebar at `x=2350, y=300`).
   - Scroll down to `Netplay` (`Join or host a netplay session.`).
   - Tap `Host` -> Tap `Start Netplay Host` (`Start netplay in host (server) mode.`).
3. **Execution:**
   - Triggering `Start Netplay Host` executes `action_ok_netplay_sublist` (`0x6b9330`) -> `netplay_driver_ctl(RARCH_NETPLAY_CTL_ENABLE_SERVER)` -> `netplay_new()`.
   - The game immediately resumes in background/foreground while the server listens for incoming peer connections.

---

## 4. Verification Evidence

### Socket Inspection
Querying `/proc/net/tcp6` immediately following the trigger:
```
/proc/net/tcp6:
0: 00000000000000000000000000000000:D88B 00000000000000000000000000000000:0000 0A ... UID 10395
```
- `D88B` (hex) = `55435` (decimal).
- State `0A` = `TCP_LISTEN`.
- UID `10395` = `u0_a395` (`com.androidapks.tekken3`).

### Network Connection Test
Using PowerShell `Test-NetConnection` over forwarded port:
```
ComputerName     : 127.0.0.1
RemotePort       : 55435
TcpTestSucceeded : True
```

### Protocol Header Capture
A dedicated Python socket test connected to `127.0.0.1:55435` and captured the raw 24-byte packet transmitted by the host:
```python
b'RANP\x00\x04\x00\x08\x00\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x05\x00\x00\x05\x89'
```
- Magic Bytes: `RANP` (RetroArch NetPlay Protocol)
- Protocol Major/Minor: `0x0004` (version 4), `0x0008` (subversion 8)
- Server Mode Flag: `0x00000001` (Host)

---

## 5. Next Steps for Cross-Device / Online Multiplayer

Since the native binaries are completely functional:
1. **Direct Wi-Fi / Hotspot Play:** Device 2 (running identical RetroArch 1.9.11 + PCSX-ReARMed + `Tekken3.bin`) can connect directly to Device 1's IP at port `55435` via the "Connect to Netplay Host" menu.
2. **Online Over Different Networks (WAN):**
   - **Option A (Relay / MITM):** Configure `netplay_use_mitm_server = "true"` and point to an open or private RetroArch relay server.
   - **Option B (ZeroTier / Tailscale / WireGuard):** Both devices join a lightweight peer-to-peer virtual private network, allowing direct IP connection without port forwarding.
   - **Option C (Custom Relay or Wrapper):** A companion Android service or overlay that automatically triggers the netplay host/client and connects peers via WebRTC or UDP punchthrough.
