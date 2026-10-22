# Two-Device Multiplayer Test Guide — Tekken 3 Netplay Validation

**Project:** `tekken3_multiplayer`  
**Package:** `com.androidapks.tekken3`  
**Date:** 2026-10-10  
**Status:** Ready for Physical Validation  

---

## 0. Objective

Prove that two real devices can connect, complete the netplay handshake, synchronize game state, and play an independent two-player Tekken 3 match — **without modifying or rebuilding anything.**

The test is successful only when:
- Client TCP connects to Host.
- `RANP` handshake completes (CRC match, protocol version match).
- Initial savestate transfers without timeout.
- Host controls Player 1 (left), Client controls Player 2 (right).
- Both players select characters and fight ≥1 full round without desync.

---

## 1. Prerequisites

### 1.1 Device Requirements

| Role | Device | Minimum Spec |
|:-----|:-------|:-------------|
| **Host (Device 1)** | Infinix X671B (your test device) | Android 5+ (API 21+), ARM64 or ARM32 |
| **Client (Device 2)** | Friend's Android phone | Android 5+ (API 21+), ARM64 or ARM32 |

> [!IMPORTANT]
> Both devices **must** run the identical `tekken3.apk` (`SHA-256: CA68CAD9...`). The netplay handshake rejects mismatched ROM CRC values (`0x892889e7`). Installing a different build or a different Tekken 3 ISO will cause an immediate disconnect.

### 1.2 APK Installation on Device 2

Transfer `tekken3.apk` to the friend's phone and install it:

```bash
# Option A: USB cable + ADB
adb install tekken3.apk

# Option B: Send the file via Telegram/Drive/USB, tap to install
```

**First-launch requirement:** Open the app once on Device 2 and let the splash screen finish. This extracts the bundled ROM and core to internal storage:
- `/data/data/com.androidapks.tekken3/content/Tekken3.bin`
- `/data/data/com.androidapks.tekken3/cores/pcsx_rearmed_libretro_android.so`

Close the app after the game screen appears. This confirms the extraction succeeded.

### 1.3 What NOT to Change

- Do **not** edit `retroarch.cfg` on either device before the first test.
- Do **not** sideload a different RetroArch build or core.
- Do **not** enable "Netplay → Use Relay Server" until Scenario C below.

---

## 2. Network Scenarios (Pick One)

### Scenario A — Wi-Fi Hotspot (Recommended First Test)

**Why start here:** Zero configuration, zero internet, 1–3 ms ping, deterministic.

| Step | Action |
|:-----|:-------|
| 1 | **Host phone** → Settings → Portable Wi-Fi Hotspot → Turn ON. |
| 2 | **Client phone** → Settings → Wi-Fi → Connect to Host's hotspot network. |
| 3 | Host IP is always **`192.168.43.1`** (Android default gateway). |

> [!TIP]
> If the hotspot gateway differs on your device, check it:  
> **Host phone** → Settings → About Phone → Status → IP Address.  
> Or from ADB: `adb shell ip addr show wlan0`

### Scenario B — Same Wi-Fi Router (LAN)

Both phones connected to the same home/office Wi-Fi network.

| Step | Action |
|:-----|:-------|
| 1 | Both phones connect to the same Wi-Fi SSID. |
| 2 | **Host phone** → Settings → Wi-Fi → Connected network → Note the IP (e.g. `192.168.1.45`). |
| 3 | Share this IP with the Client player verbally, text, or screenshot. |

> [!WARNING]
> Some routers enable "AP Isolation" (also called "Client Isolation" or "Guest Mode"), which blocks device-to-device traffic. If the client cannot connect, check router settings or fall back to Scenario A.

### Scenario C — Different Networks (WAN / Internet)

Both players are on separate networks (different Wi-Fi, mobile data, etc.). Three sub-options:

#### C1. Built-In Libretro Relay (Easiest, Higher Latency)

1. **Host only** — before starting netplay, navigate in RetroArch menu:  
   `Netplay → Host → Use Relay Server → ON`  
   Select the closest relay region (e.g., `nyc`, `madrid`).
2. Start netplay host as normal (Section 3 below).
3. The host's game room appears on `lobby.libretro.com`. The client can browse the lobby from: `Netplay → Refresh Room List` and tap the Tekken 3 room.
4. Alternatively, if the client knows the relay-assigned IP, they can connect directly.

> [!WARNING]
> Relay adds 80–150+ ms round-trip. Tekken 3 at 60 FPS is highly sensitive to input delay. This option proves connectivity but may feel sluggish for competitive play.

#### C2. Tailscale / ZeroTier VPN (Best Latency for WAN)

1. Both players install **Tailscale** (free, no root) from Google Play Store.
2. Both sign in and join the same Tailscale network (one player creates it, shares invite link).
3. Each device receives a stable virtual IP (e.g., `100.85.12.3`).
4. Tailscale performs automatic NAT traversal → direct peer-to-peer tunnel.
5. Client connects to Host's Tailscale IP at port `55435`.

> [!TIP]
> Tailscale direct connections typically achieve 25–50 ms over 4G/5G — far better than a relay.

#### C3. Port Forwarding (Advanced)

1. Host's router forwards external TCP port `55435` to Host phone's local IP.
2. Host shares their public IP (check `whatismyip.com`) with the Client.
3. Client enters the public IP in RetroArch's connect dialog.

> [!CAUTION]
> Port forwarding exposes the device to the internet. Only use this if you understand the security implications. Use Tailscale/ZeroTier instead.

---

## 3. Step-by-Step: Starting the Host (Device 1)

These steps are performed on the **Host phone** (Infinix X671B).

### 3.1 Launch the Game

Open `Tekken 3` from the app drawer. Wait for the game to fully load (you should see the Tekken 3 title screen or demo fight).

### 3.2 Open RetroArch Menu

Tap the **Space Invader icon** at the top center of the screen.

- **Overlay coordinates:** Approximately `x=50%, y=12%` of the screen.
- On the Infinix X671B (2880×1280), this is near pixel `(1442, 125)`.
- The icon sits between the two health bar ends.

The RGUI Quick Menu opens (dark blue/purple menu with text entries).

### 3.3 Navigate to Netplay

1. Tap the **Home tab icon** on the right sidebar (house/home icon at approximately `x=2350, y=300` on the Infinix screen). This switches from Quick Menu to the Main Menu.
2. Scroll down through the Main Menu entries until you see:  
   **`Netplay`** — `Join or host a netplay session.`
3. Tap `Netplay`.

### 3.4 Start the Host

1. Tap **`Host`**.
2. Tap **`Start Netplay Host`** — (`Start netplay in host (server) mode.`).

The menu closes and the game resumes. The host server is now listening.

### 3.5 Verify the Host is Listening (Optional, requires ADB)

If ADB is connected:

```bash
adb shell "grep D88B /proc/net/tcp6"
```

Expected output (confirms port 55435 in LISTEN state):
```
 0: 00000000000000000000000000000000:D88B 00000000000000000000000000000000:0000 0A ... uid:10395
```

- `D88B` hex = `55435` decimal.
- State `0A` = `TCP_LISTEN`.

If you see this line, the host is ready for a client connection.

### 3.6 Note the Host IP

- **Hotspot:** `192.168.43.1`
- **LAN Wi-Fi:** Check Settings → Wi-Fi → Connected → IP Address.
- **Tailscale:** Check Tailscale app → your device's `100.x.y.z` IP.

Communicate this IP to the Client player.

---

## 4. Step-by-Step: Connecting the Client (Device 2)

These steps are performed on the **Client phone** (friend's device).

### 4.1 Ensure Network Connectivity

Confirm the Client phone is on the same network as the Host:
- **Hotspot:** Connected to Host's Wi-Fi hotspot.
- **LAN:** Connected to the same Wi-Fi SSID.
- **Tailscale:** VPN active, showing a `100.x.y.z` IP.

Quick connectivity test (if ADB is available):
```bash
adb shell ping -c 3 <HOST_IP>
```
You should see replies with <5 ms latency on LAN/hotspot.

### 4.2 Launch the Game

Open `Tekken 3` on the Client phone. Wait for the game to fully load.

### 4.3 Open RetroArch Menu

Tap the **Space Invader icon** at the top center of the screen (same position as on Host — approximately center-x, ~12% from top).

### 4.4 Navigate to Netplay

1. Tap the **Home tab icon** on the right sidebar.
2. Scroll down to **`Netplay`**.
3. Tap `Netplay`.

### 4.5 Connect to Host

1. Tap **`Connect to Netplay Host`**.
2. An on-screen keyboard (OSK) dialog appears asking for the Host IP address.
3. Type the Host IP address exactly:
   - Hotspot example: `192.168.43.1`
   - LAN example: `192.168.1.45`
   - Tailscale example: `100.85.12.3`
4. Tap **OK** / **Enter**.

### 4.6 What Happens Next

If the connection succeeds:
1. RetroArch initiates a TCP connection to `<HOST_IP>:55435`.
2. The host sends its 24-byte `RANP` protocol header.
3. The client replies with its own handshake.
4. The host sends a CRC verification challenge. If the ROM CRC matches (`0x892889e7`), the handshake proceeds.
5. The host serializes its current game state (savestate ~2.8 MB) and transmits it to the client.
6. The client loads the savestate and enters synchronized lockstep mode.
7. The menu closes on the client. Both devices are now showing the same game frame.

**Expected time:** 2–5 seconds on LAN/hotspot, up to 15 seconds over relay/WAN.

---

## 5. Playing the Match

### 5.1 Controller Assignment

| Device | Player | Screen Position |
|:-------|:-------|:----------------|
| Host (Device 1) | **Player 1** | Left side fighter |
| Client (Device 2) | **Player 2** | Right side fighter |

Both players use their phone's touch overlay (`neo-retropad`). Inputs are independent — each player's touches only control their own fighter.

### 5.2 Character Selection

If the host was at the title screen when netplay started:
1. Both players should see the title screen.
2. Host presses Start → enters character select.
3. Each player independently selects their character using their own D-pad and buttons.

If the host was already in a match:
1. The client loads into the exact game state the host is in.
2. Finish the current match, then proceed to character select for a fresh round.

### 5.3 Gameplay Verification

During the fight, visually verify on both screens:
- [ ] Both screens show the same characters in the same arena.
- [ ] Health bars change when either player lands a hit.
- [ ] Host's inputs only move Player 1 (left fighter).
- [ ] Client's inputs only move Player 2 (right fighter).
- [ ] No freezes, stutters, or "waiting for peer" messages lasting >1 second on hotspot/LAN.

---

## 6. Verification Checklist

Complete this checklist after the test. Every box must be checked for a **PASS**.

### Connection

- [ ] Client TCP connection to Host IP:55435 was established (no timeout, no "connection refused").
- [ ] No "CRC mismatch" or "incompatible core" error appeared.
- [ ] Initial savestate transfer completed (game state synchronized on both screens).

### Gameplay

- [ ] Host controls Player 1 exclusively.
- [ ] Client controls Player 2 exclusively.
- [ ] Both players completed character selection independently.
- [ ] At least 1 full round of fighting was completed.
- [ ] Health bars were consistent across both screens (no desync where one screen shows a KO but the other doesn't).

### Stability

- [ ] No crash or force-close on either device during the match.
- [ ] No desync requiring manual reconnection during the test.
- [ ] Frame rate remained at or near 60 FPS on both devices (no severe slowdown).

### Network (record these values)

- [ ] **Connection method used:** ___ (Hotspot / LAN / Relay / Tailscale / Port Forward)
- [ ] **Approximate perceived input delay:** ___ frames / ___ ms
- [ ] **Any "waiting for peer" pauses?** Yes / No — if yes, duration: ___
- [ ] **Match duration before any issue:** ___ minutes

---

## 7. Troubleshooting

### 7.1 Client Cannot Connect (Connection Refused / Timeout)

| Symptom | Likely Cause | Fix |
|:--------|:-------------|:----|
| "Connection refused" | Host netplay not started, or wrong IP. | Re-do Section 3. Verify with `grep D88B /proc/net/tcp6`. |
| Timeout (hangs forever) | Firewall, AP isolation, or wrong network. | Confirm both devices are on the same network. Try Scenario A (hotspot). |
| Connects but immediately drops | ROM CRC mismatch. | Ensure both devices have the exact same `tekken3.apk` installed. |

### 7.2 CRC Mismatch Error

The `tekken3.apk` bundles `Tekken3.bin` with CRC32 `0x892889e7`. If the client installed a different APK version or a different Tekken 3 ISO, the handshake will fail with a CRC error.

**Fix:** Ensure both devices installed the exact same APK file. Uninstall and reinstall from the same source.

### 7.3 Savestate Transfer Fails / Timeout

Large savestates (~2.8 MB) can take a few seconds. On very slow Wi-Fi or high-latency relay connections, the transfer may timeout.

**Fix:**
- Switch to hotspot (Scenario A) for the initial test.
- Ensure no other heavy downloads or streaming on the same network.

### 7.4 Desync During Match

If health bars diverge or players see different game states:

1. The `PCSX-ReARMed` dynarec may have non-deterministic behavior under certain conditions.
2. Try adding input latency tuning to `retroarch.cfg` on both devices:

```ini
netplay_input_latency_frames_min = "3"
netplay_input_latency_frames_range = "0"
netplay_check_frames = "600"
```

Config file location: `/storage/emulated/0/Android/data/com.androidapks.tekken3/files/retroarch.cfg`

**Note:** Record the exact moment of desync (round number, character combo, etc.) for debugging. This data is critical for evaluating whether PCSX-ReARMed savestates are reliable for netplay.

### 7.5 Cannot Find the Space Invader Icon

The overlay icon may be invisible or repositioned on some screen ratios. Try tapping the very top center of the screen (between the two health bars). If still hidden:

1. Connect via ADB.
2. Send a menu toggle input event:
   ```bash
   adb shell input keyevent 82
   ```
   (Keycode 82 = MENU, which some RetroArch builds map to RGUI toggle.)

---

## 8. Logging & Evidence Collection

Capture these during the test to produce a proper validation report.

### 8.1 Before Connecting (Host Side, via ADB)

```bash
# Confirm host is listening
adb shell "grep D88B /proc/net/tcp6"

# Log current netplay config
adb shell "cat /storage/emulated/0/Android/data/com.androidapks.tekken3/files/retroarch.cfg | grep netplay"

# Get host IP
adb shell "ip addr show wlan0"
```

### 8.2 After Client Connects (Host Side, via ADB)

```bash
# Confirm an ESTABLISHED connection exists
adb shell "grep D88B /proc/net/tcp6"
```

You should now see **two** lines:
- One in state `0A` (LISTEN — the server socket).
- One in state `01` (ESTABLISHED — the active peer connection).

### 8.3 Screenshots

Take a screenshot of both screens showing the same game frame during a fight:

```bash
# From ADB on each device
adb exec-out screencap -p > host_screenshot.png
adb exec-out screencap -p > client_screenshot.png
```

### 8.4 RetroArch Log (If Available)

RetroArch may write verbose logs to:
```
/storage/emulated/0/Android/data/com.androidapks.tekken3/files/retroarch.log
```

Enable verbose logging before the test:
1. Open RetroArch menu → Settings → Logging.
2. Set `Log Verbosity` → ON.
3. Set `Log to File` → ON.

After the test:
```bash
adb pull /storage/emulated/0/Android/data/com.androidapks.tekken3/files/retroarch.log
```

---

## 9. What This Test Proves (and What It Doesn't)

### PASS Means:
- The bundled RetroArch 1.9.11 + PCSX-ReARMed binary supports real two-player netplay.
- The existing in-game menu flow is sufficient to host and join.
- The ROM and core are deterministic enough for at least short matches.
- **We can proceed to building the launcher UI (Solo / Host / Join).**

### PASS Does NOT Mean:
- Internet play works reliably (that requires separate WAN testing with relay/Tailscale).
- Extended matches (10+ rounds) won't desync (needs longer stress testing).
- The UX is acceptable for a casual user (the menu path is 4 taps deep — the launcher will simplify this).

### FAIL Means:
- Record the exact failure point and error messages.
- Capture `retroarch.log` from both devices.
- Document whether the failure is in handshake, savestate transfer, or gameplay desync.
- We may need to investigate PCSX-ReARMed savestate determinism or core version compatibility before proceeding.

---

## 10. Quick Reference Card

Print or screenshot this for the person running the test:

```
╔══════════════════════════════════════════════════════════╗
║             TEKKEN 3 NETPLAY QUICK REFERENCE             ║
╠══════════════════════════════════════════════════════════╣
║                                                          ║
║  HOST (Device 1):                                        ║
║  1. Open Tekken 3                                        ║
║  2. Tap Space Invader icon (top center)                  ║
║  3. Tap Home tab (right sidebar)                         ║
║  4. Scroll → Netplay → Host → Start Netplay Host        ║
║  5. Tell friend your IP: ___________                     ║
║                                                          ║
║  CLIENT (Device 2):                                      ║
║  1. Connect to same network as Host                      ║
║  2. Open Tekken 3                                        ║
║  3. Tap Space Invader icon (top center)                  ║
║  4. Tap Home tab (right sidebar)                         ║
║  5. Scroll → Netplay → Connect to Netplay Host          ║
║  6. Enter Host IP → OK                                   ║
║                                                          ║
║  VERIFY:                                                 ║
║  ✓ Both screens show same game state                     ║
║  ✓ Host = Player 1 (left), Client = Player 2 (right)    ║
║  ✓ Fight 1 full round without desync                     ║
║                                                          ║
║  HOTSPOT IP:     192.168.43.1                            ║
║  NETPLAY PORT:   55435                                   ║
║  ROM CRC32:      0x892889e7                              ║
║                                                          ║
╚══════════════════════════════════════════════════════════╝
```
