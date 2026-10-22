# Project Memory & Knowledge Base: Tekken 3 Android Multiplayer

**Project Workspace:** `D:\programming work\tekken3_multiplayer`  
**Latest Update:** October 10, 2026  
**Latest Deliverable:** [dist/Tekken3-Multiplayer-Tailscale.apk](file:///d:/programming%20work/tekken3_multiplayer/dist/Tekken3-Multiplayer-Tailscale.apk) (Crash-fixed & Production Signed)  

---

## 1. Project Overview, Core Constraints & Objectives

- **Project Goal:** Enable seamless two-player online and local multiplayer for the extracted Android Tekken 3 APK (`com.androidapks.tekken3`), allowing two Android players on separate mobile devices and different networks (cellular/Wi-Fi) to play together.
- **Strict Non-Destructive Invariant:**
  - Zero modification or hex-patching of the PlayStation 1 game ROM (`assets/content/Tekken3.bin`).
  - Zero modification or recompilation of the native emulator libraries (`libretroarch-activity.so`, `pcsx_rearmed_libretro_android.so`).
  - Any ROM byte alteration changes the CRC32 checksum (`0x892889e7`), causing RetroArch's netplay protocol to reject connections due to checksum mismatch and breaking frame rollback determinism.
- **Architectural Approach:**
  - Retain the untouched original APK backup [tekken3.apk](file:///d:/programming%20work/tekken3_multiplayer/tekken3.apk).
  - Reverse engineer RetroArch 1.9.11 intent handling, netplay runtime flags, and configuration parsing.
  - Implement an in-app retro launcher overlay at the Android Activity layer (`com.retroarch.browser.mainmenu.MainMenuActivity`) to provide Room Code hosting, joining, status polling, and Tailscale mesh networking without modifying the underlying emulator core.

---

## 2. Hardware, Environment & Physical Device Profiles

- **Primary Physical Test Device:** Infinix X671B (Serial: `08617312BQ072417`)
- **Operating System:** Android 13 (API 33, Linux Kernel 5.10, Architecture: `arm64-v8a`)
- **Package Name:** `com.androidapks.tekken3`
- **Application UID:** `u0_a395` (Linux UID: `10395`)
- **Base APK Path on Device:**  
  `/data/app/~~bq896_yllG0mKJP1DvJ_PA==/com.androidapks.tekken3-YDJ5H-ppE04ysoJD2pcdpg==/base.apk`
- **Application Sandbox Storage:**
  - App-specific external storage: `/storage/emulated/0/Android/data/com.androidapks.tekken3/files/`
  - Internal application data: `/data/user/0/com.androidapks.tekken3/` (untrusted sandbox; non-debuggable)
  - Active configuration path: `/storage/emulated/0/Android/data/com.androidapks.tekken3/files/retroarch.cfg`
- **Device Configuration Backup:**
  - SHA-256: `FE858FC5F7A69AA45E9EF63764965CFA9E8F2EA3BE0707C3324F402CB49D10B8`
  - Local Path: [analysis/netplay-test/backups/retroarch_device_backup.cfg](file:///d:/programming%20work/tekken3_multiplayer/analysis/netplay-test/backups/retroarch_device_backup.cfg) (105,848 bytes, 3,124 lines)
- **ADB File Permission Invariant:**
  - Overwriting `retroarch.cfg` directly via `adb push` sets ownership to `shell:ext_data_rw` (`644`), which causes `EACCES (Permission denied)` crashes in `UserPreferences.java` on app launch.
  - Safe pattern: Deploy configurations under app-accessible paths with `chmod 666` or write them programmatically via Java/Smali within the app process.

---

## 3. APK & Engine Architecture

- **Original APK:** [tekken3.apk](file:///d:/programming%20work/tekken3_multiplayer/tekken3.apk) (63,052,988 bytes)
- **Underlying Engine:** RetroArch 1.9.11 (Git commit `cf6f7ca`, compiled Dec 31, 2024)
- **Emulation Core:** PCSX-ReARMed (`pcsx_rearmed_libretro_android.so`, Ari64 ARM dynamic recompiler)
- **Bundled Game Disc / ROM:**
  - File: `assets/content/Tekken3.bin` (47,330,224 bytes, ~45.1 MB)
  - Game ID: `SLPS_013.00` (PlayStation NTSC-J release, 60 FPS)
  - Header CRC32: `0x892889e7`
  - SHA-256: `0F059C80B9477940F3BCA0BE680CAAE4497D0C71D74A7876F6FEF5879BFD9813`
- **Exported Savestate Symbols:**
  - `retro_serialize` (Exported)
  - `retro_unserialize` (Exported)
  - `retro_serialize_size` (Exported)
- **Netplay Engine in Binary:** Fully compiled into `libretroarch-activity.so` (`netplay_new`, `netplay_poll`, `netplay_sync_pre/post_frame`, `netplay_handshake`, `netplay_driver_ctl`, `action_ok_netplay_sublist`).

---

## 4. Reverse Engineering Findings (`libretroarch-activity.so`)

### 4.1 Intent Handling & Extra Filtering
- Intent handler subroutine at `0x5ed8c0` explicitly processes:
  - `"CONFIGFILE"`: overrides active configuration file path.
  - `"LIBRETRO"`: path to the core `.so`.
  - `"ROM"`: path to game ROM.
  - `"SDCARD"`, `"EXTERNAL"`, `"DATADIR"`, `"APK"`: system paths.
- Does **not** parse command-line parameters (such as `--host` or `--connect`) from Intent extras.

### 4.2 Netplay Runtime Flag & Boot Wipe Blocker
- The internal active netplay flag resides at virtual address `0xb9ac7d` (16-bit state: `0x0001` = host/server, `0x0101` = client).
- On Android `NativeActivity` boot, `retroarch_main_init` unconditionally clears this flag:
  ```arm64
  0x0060c2b4: strb wzr, [x8, #0xc7c]   ; Zeroes active runtime state at 0xb9ac7d!
  ```
- Content load check at `0x60a17c`:
  ```arm64
  0x0060a17c: adrp  x8, #0xb9a000
  0x0060a180: ldrb  w8, [x8, #0xc7d]
  0x0060a184: cbz   w8, #0x60a3dc     ; If 0, skips netplay socket initialization!
  ```
- **Architectural Implication:** Merely setting `netplay_enable = "true"` and `netplay_mode = "true"` in `retroarch.cfg` does **not** bind the listening socket on boot. Headless auto-hosting is wiped by engine boot code; netplay must be triggered either via the in-game overlay menu or an internal runtime action.

### 4.3 In-Game Touch Overlay Navigation Path
- Overlay configuration: `/data/user/0/com.androidapks.tekken3/overlays/gamepads/neo-retropad/neo-retropad.cfg`
- Descriptor: `overlay0_desc19 = "menu_toggle,0.50125,0.11708333333334,radial,0.025,0.04166666666"` (`rgui.png`)
- Screen Coordinates on 2400x1080 display: `(1442, 125)` (Space Invader icon between health bars).
- Menu path to Host:
  1. Tap Space Invader icon `(1442, 125)` -> Opens Quick Menu.
  2. Tap Home icon on right sidebar -> Main Menu tab.
  3. Scroll to `Netplay` (`Join or host a netplay session.`).
  4. Tap `Host` -> Tap `Start Netplay Host`.
  5. RetroArch calls `action_ok_netplay_sublist` (`0x6b9330`) -> `netplay_driver_ctl(4)` (`RARCH_NETPLAY_CTL_ENABLE_SERVER`) -> `netplay_new()`.

---

## 5. Live Single-Device Validation Results

**Verdict: PASS (Empirically Confirmed on Infinix X671B)**

1. **TCP Port Bound:**
   - Reading `/proc/net/tcp6` confirmed TCP port `0xD88B` (`55435`) listening in dual-stack IPv4/IPv6 mode under UID `10395` (`com.androidapks.tekken3`).
2. **Local Socket Probe:**
   - Port forwarded via `adb forward tcp:55435 tcp:55435`.
   - PowerShell `Test-NetConnection -ComputerName 127.0.0.1 -Port 55435` returned `TcpTestSucceeded : True`.
3. **Official Protocol Handshake Captured:**
   - Socket client connected to port 55435 and captured the 24-byte RetroArch Netplay header:
     ```
     b'RANP\x00\x04\x00\x08\x00\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x05\x00\x00\x05\x89'
     ```
     - Magic: `RANP` (RetroArch NetPlay)
     - Protocol Version: `0x0004`, `0x0008` (Version 4.8)
     - Server Mode Flag: `0x00000001` (Host mode verified)
4. **Game Stability:** Tekken 3 ran at full 60 FPS while the server socket remained active in the background.

---

## 6. Phase 1: Room-Code Multiplayer Architecture (`Tekken3-Multiplayer.apk`)

To provide an arcade-authentic user experience, we built an interactive room-code multiplayer launcher.

### 6.1 Smali Routing Hook & Multidex Layout
- Target Activity: `com.retroarch.browser.mainmenu.MainMenuActivity`.
- In `MainMenuActivity.smali`, the asset extraction and initialization flow passes control to `MultiplayerLauncher.showLauncher(this)`.
- **Dex Limit Constraint:** `classes.dex` contained 65,511 method IDs (leaving only 25 free slots before Dalvik 64K overflow).
- **Solution:** All new launcher classes were placed in a secondary DEX (`classes2.dex` / `smali_classes2`).

### 6.2 Retro UI Design
- Authentic Tekken 3 arcade aesthetics: Carbon `#08090C` background, beveled `#FF2A00`–`#FFCC00` flame borders, arcade yellow titles, and high-contrast monospace code cards.
- Mode Selection:
  1. `ARCADE MODE` (Solo Play)
  2. `★ MULTIPLAYER MODE ★` (Submenu)
  3. `VS MODE`
  4. `PRACTICE MODE`
  5. `OPTIONS`
- Multiplayer Submenu:
  - `CREATE ROOM`: Generates room code, displays IP, one-tap Copy/Share, status polling.
  - `JOIN ROOM`: Pastes room code, validates IP, pre-configures client connection.
  - `SERVER / RELAY SETTINGS`: Allows configuring external signaling servers.
  - `BACK`: Returns to main menu.

### 6.3 Room Signaling Server ([analysis/room_server.py](file:///d:/programming%20work/tekken3_multiplayer/analysis/room_server.py))
- Lightweight Python REST service for peer coordination:
  - `GET /health`: Health check and active room count.
  - `POST /api/rooms/create`: Registers host IP, port 55435, and room code (10-minute TTL).
  - `GET /api/rooms/<code>`: Polling endpoint for host to detect guest arrival.
  - `POST /api/rooms/<code>/join`: Guest joins and obtains host connection coordinates.
  - `POST /api/rooms/<code>/ready`: Both players signal readiness.
  - `DELETE /api/rooms/<code>`: Cleans up room on cancel.
- Validated with 100% pass rate in [analysis/test_room_lifecycle.py](file:///d:/programming%20work/tekken3_multiplayer/analysis/test_room_lifecycle.py).

### 6.4 Zero-Infrastructure Base36 Smart Room Codes
- If the signaling server is unreachable or offline, the launcher falls back to Base36 IP encoding:
  - Packs 32-bit IPv4 address into a Base36 integer string prefixed with `TK-` (e.g., `192.168.1.50` -> `TK-1QJNZ2`).
  - Guest decodes `TK-1QJNZ2` directly back to `192.168.1.50` with zero server dependency.

---

## 7. Remote Readiness Audit & WAN Reality Check

In [`MULTIPLAYER_REMOTE_READINESS_AUDIT.md`](file:///d:/programming%20work/tekken3_multiplayer/MULTIPLAYER_REMOTE_READINESS_AUDIT.md), an audit of `Tekken3-Multiplayer.apk` established critical architectural facts:

1. **Signaling Server:** Defaulted to `http://127.0.0.1:8080`, running only in local smart-code mode unless an external server was deployed.
2. **NAT Traversal Gap:** Base36 smart codes encode private RFC 1918 addresses (`192.168.1.x`). Without STUN, UPnP, or relay tunneling, remote phones on different Wi-Fi/cellular networks cannot reach private IP addresses.
3. **Libretro MITM Relay Discovery:** RetroArch MITM relays require negotiating a `mitm_session` string with `us-east1.relay.retroarch.com:55435`. The basic launcher wrote the local IP rather than coordinating relay sessions.
4. **Resolution:** Tailscale mesh networking was chosen as the most robust, zero-cost solution for direct peer-to-peer WAN play without requiring cloud hosting or router port forwarding.

---

## 8. Tailscale Companion Integration & Proof-of-Concept

### 8.1 Architectural Evaluation: Model B Selected
- Evaluated three approaches in [`TAILSCALE_INTEGRATION_FEASIBILITY.md`](file:///d:/programming%20work/tekken3_multiplayer/TAILSCALE_INTEGRATION_FEASIBILITY.md):
  - **Model A (Embedded Gomobile Client):** Bundles Go runtime + WireGuard in APK. Rejected due to +35MB binary bloat, OAuth/credential leakage, and Android single-`VpnService` conflict.
  - **Model B (Companion Deep-Link Flow - RECOMMENDED):** Relies on the official Tailscale app for VPN tunnel management while Tekken 3 auto-detects the virtual IP interface. Zero APK bloat, 100% secure user SSO, and completely preserves untouched native `.so` files.
  - **Model C (Libretro Relay):** Uses public relays, but introduces 80–120ms latency and requires relay session token coordination.

### 8.2 Technical Implementations in `MultiplayerLauncher.java`
1. **RFC 6598 IPv4 Detection:**
   - Detects Carrier-Grade NAT addresses in `100.64.0.0/10` (`100.64.0.0`–`100.127.255.255`) on active TUN/VPN network interfaces.
2. **Network Prioritization:**
   - `getBestNetworkProfile()` automatically prioritizes an active Tailscale IP over local Wi-Fi.
3. **Base36 Decoder Prefix Fix:**
   - Fixed bug where `code.replace("TK", "")` corrupted room codes containing the letters `TK` in their Base36 body. Replaced with safe leading substring stripping (`clean.startsWith("TK-")` / `"TK"`).
4. **Pre-Flight Host Reachability Probe (`HostProbeWorker`):**
   - Client probes host port `55435` via TCP before launching RetroArch:
     - *Port Open:* Confirms host is live and listening.
     - *Connection Refused:* Host phone is reached over Tailscale, but host hasn't started Netplay yet.
     - *Socket Timeout:* Host is unreachable (Tailscale disconnected or node not shared).
5. **Automated Verification:**
   - Automated test suite in [analysis/test_tailscale_poc.py](file:///d:/programming%20work/tekken3_multiplayer/analysis/test_tailscale_poc.py) verified 509 test vectors with 100% pass rate.

---

## 9. Physical Hardware Crash Root Cause Analysis & Resolution (Oct 10, 2026)

### 9.1 The Problem
Upon installing `dist/Tekken3-Multiplayer-Tailscale.apk` on physical test devices (Phone 1 and Phone 2), the application crashed immediately upon launch.

### 9.2 The Five Diagnosed Root Causes

1. **PreferenceActivity / ListActivity ID Fatal Invariant:**
   - `MainMenuActivity` inherits from `PreferenceActivity` -> `ListActivity`.
   - Calling `activity.setContentView(scrollView)` triggered `ListActivity.onContentChanged()`.
   - Android's internal `ListActivity` implementation expects a view with id `@android:id/list` (`0x0102000a`). Because the custom layout did not provide this, Android threw an uncaught fatal exception:
     `RuntimeException: Your content must have a ListView whose id attribute is 'android.R.id.list'`.
2. **Legacy `firstLaunch` Process Killer:**
   - Baseline `MainMenuActivity` checked the `"firstLaunch"` shared preference. On fresh installs, it scheduled `MainMenuActivity$5` which called `Runtime.getRuntime().exit(1)` after 5000ms, abruptly killing the process.
3. **Android 13/14 Permission Denial Stall:**
   - Modern Android (API 33+) denies `READ_EXTERNAL_STORAGE` because media permissions are scoped.
   - Baseline `onRequestPermissionsResult` only called `finalStartup()` if permissions were granted, causing a permanent freeze on Android 13/14 devices.
4. **MultiDex Secondary Dex Resolution Risk:**
   - `SplitCompatApplication` did not explicitly invoke `MultiDex.install(Context)`, risking `NoClassDefFoundError` when loading `MultiplayerLauncher` from `classes2.dex` on certain ROMs.
5. **NetworkInterface NPE:**
   - `NetworkInterface.getNetworkInterfaces()` can return `null` on restricted Android states, causing `NullPointerException` when passed to `Collections.list(...)`.

### 9.3 Applied & Verified Fixes

1. **`onContentChanged()` Override & Dummy ListView:**
   - Injected a no-op override of `onContentChanged()` into [MainMenuActivity.smali](file:///d:/programming%20work/tekken3_multiplayer/analysis/apktool/smali/com/retroarch/browser/mainmenu/MainMenuActivity.smali) to silence `ListActivity` validation.
   - Injected a hidden dummy `ListView` with ID `android.R.id.list` (`0x0102000a`) into the root layout in [MultiplayerLauncher.java](file:///d:/programming%20work/tekken3_multiplayer/analysis/MultiplayerLauncher.java).
2. **Synchronous Asset Extraction (`ensureAssetsExtracted`):**
   - Added Java asset extraction logic in `MultiplayerLauncher.java` that unpacks ROM content and core libraries in ~0.2s without requiring VM restarts or scheduled timers.
   - Streamlined `finalStartup()` in Smali to bypass the 5-second timer and directly call `MultiplayerLauncher.showLauncher(this)`.
3. **Unconditional Permission Progression:**
   - Patched `MainMenuActivity.onRequestPermissionsResult` in Smali to unconditionally proceed to `finalStartup()`, preventing stalls on Android 13/14.
4. **MultiDex Application Injection:**
   - Injected `MultiDex.install(this)` into `SplitCompatApplication.smali` (`attachBaseContext`) to ensure `classes2.dex` is loaded immediately.
5. **Null-Safe Network Enumeration:**
   - Wrapped `NetworkInterface.getNetworkInterfaces()` in null checks in `MultiplayerLauncher.java`.
6. **Top-Level Crash Guard:**
   - Added `try / catch (Throwable)` around menu construction in `MultiplayerLauncher.java`, ensuring that any unexpected UI exception cleanly falls back to starting standard Arcade mode.
7. **Activity Finish on Engine Launch:**
   - Added `activity.finish()` when launching `RetroActivityFuture` so the launcher activity does not conflict with the native fullscreen surface.

---

## 10. Production Deliverables & Verification Matrix

All production artifacts are compiled, verified, and located in the `dist/` directory.

### 10.1 APK Deliverables

| Deliverable | Size (Bytes) | SHA-256 Checksum | Signature Schemes | Status |
| :--- | :--- | :--- | :--- | :--- |
| **[Tekken3-Multiplayer-Tailscale.apk](file:///d:/programming%20work/tekken3_multiplayer/dist/Tekken3-Multiplayer-Tailscale.apk)** | `63,189,640` | `F0DE0B64D3C97200B6987B4BA8892B2A56305FFC295319EB73F1B8AFC19FA1E0` | v1, v2, v3 Verified | **Latest Candidate (Crash-Fixed)** |
| **[Tekken3-Multiplayer.apk](file:///d:/programming%20work/tekken3_multiplayer/dist/Tekken3-Multiplayer.apk)** | `63,185,544` | `C5E38C4066C865A569AAAAA9C7F6635DFA253870976385777CA046A06E4581AB` | v1, v2, v3 Verified | Preserved Base Release |
| **[tekken3.apk](file:///d:/programming%20work/tekken3_multiplayer/tekken3.apk)** | `63,052,988` | `835F50BB20A45D8CD6C163539886D49114D3C523A7304AE0993A604473C339F0` | Original Store Signature | Original Untouched Backup |

### 10.2 Byte-Level Binary & Content Audit (100% Unmodified)

Every byte of the emulator core, native libraries, and game ROM in `Tekken3-Multiplayer-Tailscale.apk` matches the original `tekken3.apk` bit-for-bit:

| Archive Entry | Original SHA-256 | Candidate SHA-256 | Status |
| :--- | :--- | :--- | :---: |
| `assets/content/Tekken3.bin` | `0F059C80B9477940F3BCA0BE680CAAE4497D0C71D74A7876F6FEF5879BFD9813` | `0F059C80B9477940F3BCA0BE680CAAE4497D0C71D74A7876F6FEF5879BFD9813` | **100% MATCH** |
| `assets/cores/pcsx_rearmed_libretro_android.so` | `E3E3B4E4B7E51639257769E59E5375A9C0AC8883439415FEC3E219166AB2BFC3` | `E3E3B4E4B7E51639257769E59E5375A9C0AC8883439415FEC3E219166AB2BFC3` | **100% MATCH** |
| `assets/cores/pcsx_rearmed_libretro_android_armv7.so` | `F90D57F7D9D29666973D353ECE923A61B6995176EF2814D10F784CBEAB131ABD` | `F90D57F7D9D29666973D353ECE923A61B6995176EF2814D10F784CBEAB131ABD` | **100% MATCH** |
| `lib/arm64-v8a/libretroarch-activity.so` | `F081C0CE13A28D90181900253C26B9AC2CC00594196A2AB720D5F27BDF753E3C` | `F081C0CE13A28D90181900253C26B9AC2CC00594196A2AB720D5F27BDF753E3C` | **100% MATCH** |
| `lib/armeabi-v7a/libretroarch-activity.so` | `C36BDD43F1035BEB62FBFCF536F0C45ADCBE71C35883446C074E3FF28060AA6C` | `C36BDD43F1035BEB62FBFCF536F0C45ADCBE71C35883446C074E3FF28060AA6C` | **100% MATCH** |

- **Keystore:** [tekken3_release.keystore](file:///d:/programming%20work/tekken3_multiplayer/tekken3_release.keystore) (2048-bit RSA, Alias: `tekken3`, Pass: `tekken3multi`).
- **Signing Tool:** `apksigner sign` with 4-byte zipalign verification.

---

## 11. Two-Phone Physical Match Verification Procedure

To conduct a live remote match between two friends on separate physical Android devices (e.g. Phone 1 on Wi-Fi, Phone 2 on 4G/5G mobile data):

### 11.1 Setup (One-Time)
1. Install [dist/Tekken3-Multiplayer-Tailscale.apk](file:///d:/programming%20work/tekken3_multiplayer/dist/Tekken3-Multiplayer-Tailscale.apk) on both Phone 1 and Phone 2.
2. Install the official **Tailscale** app from Google Play Store on both phones.
3. Sign into Tailscale on both phones (using either the same account or sharing Phone 1's node to Phone 2 via Tailscale Admin -> Share).
4. Confirm both phones display green connected status in Tailscale and show `100.x.y.z` addresses.

### 11.2 Host Match (Phone 1)
1. Open Tekken 3.
2. Confirm the top banner indicates: `● TAILSCALE ACTIVE: 100.x.y.z (REMOTE READY)` in cyan.
3. Tap **`★ MULTIPLAYER MODE ★`** -> **`▶ CREATE ROOM`**.
4. Note the generated 6-character room code (e.g., `TK-RU7KM9`).
5. Tap **`📋 COPY CODE`** or **`📤 SHARE`** and send the code to your friend.
6. Tap **`START FIGHT`**. Game loads into Tekken 3.
7. Once game loads:
   - Tap **Space Invader icon** (between health bars at top center).
   - Tap **Home icon** (right menu bar).
   - Scroll down to **Netplay** -> tap **Start Netplay Host**.
   - On-screen confirmation: `Netplay server started on port 55435`.

### 11.3 Join Match (Phone 2)
1. Open Tekken 3 on Phone 2.
2. Tap **`★ MULTIPLAYER MODE ★`** -> **`▶ JOIN ROOM`**.
3. Enter or paste the room code (`TK-RU7KM9`).
4. Tap **`VALIDATE & PROBE`**.
5. Client verifies host reachability over Tailscale and displays:  
   `✅ Host is LIVE & reachable at 100.x.y.z! Ready to connect!`.
6. Tap **`CONNECT & FIGHT`**. Game loads into Tekken 3.
7. Once game loads:
   - Tap **Space Invader icon** -> **Home icon** -> **Netplay**.
   - Tap **Connect to Netplay Host** (host IP is pre-filled automatically).
   - On-screen confirmation: `Connected to Netplay Host!`.

### 11.4 In-Game Fight
- Navigate to the Character Selection screen.
- Phone 1 (Host) controls **Player 1** (Left fighter).
- Phone 2 (Guest) controls **Player 2** (Right fighter).
- Verify independent cursor movement, character select locks, and 3-round 60 FPS fighting.

---

## 12. Artifact, Report & Code Reference Index

### Comprehensive Reports
- [INITIAL_APK_ANALYSIS.md](file:///d:/programming%20work/tekken3_multiplayer/analysis/reports/INITIAL_APK_ANALYSIS.md): Initial APK component, manifest, and engine analysis.
- [NETPLAY_VALIDATION_PLAN.md](file:///d:/programming%20work/tekken3_multiplayer/analysis/reports/NETPLAY_VALIDATION_PLAN.md): Single-device host validation test plan.
- [NETPLAY_HOST_VALIDATION_RESULTS.md](file:///d:/programming%20work/tekken3_multiplayer/analysis/reports/NETPLAY_HOST_VALIDATION_RESULTS.md): Detailed protocol trace and `RANP` socket capture.
- [HOST_JOIN_IMPLEMENTATION_PLAN.md](file:///d:/programming%20work/tekken3_multiplayer/analysis/reports/HOST_JOIN_IMPLEMENTATION_PLAN.md): Architectural design for non-invasive host/join launcher.
- [TWO_DEVICE_TEST_GUIDE.md](file:///d:/programming%20work/tekken3_multiplayer/analysis/reports/TWO_DEVICE_TEST_GUIDE.md): Physical two-device verification guide.
- [MULTIPLAYER_BUILD_REPORT.md](file:///d:/programming%20work/tekken3_multiplayer/MULTIPLAYER_BUILD_REPORT.md): Build, signing, and integrity verification report for the initial multiplayer build.
- [MULTIPLAYER_ROOM_IMPLEMENTATION_REPORT.md](file:///d:/programming%20work/tekken3_multiplayer/MULTIPLAYER_ROOM_IMPLEMENTATION_REPORT.md): In-depth report on room code UX, signaling server, and Base36 fallbacks.
- [MULTIPLAYER_REMOTE_READINESS_AUDIT.md](file:///d:/programming%20work/tekken3_multiplayer/MULTIPLAYER_REMOTE_READINESS_AUDIT.md): Comprehensive audit of remote internet readiness and NAT gaps.
- [TAILSCALE_INTEGRATION_FEASIBILITY.md](file:///d:/programming%20work/tekken3_multiplayer/TAILSCALE_INTEGRATION_FEASIBILITY.md): Feasibility study of Model A (embedded) vs. Model B (companion) vs. Model C (relay).
- [TAILSCALE_POC_BUILD_REPORT.md](file:///d:/programming%20work/tekken3_multiplayer/TAILSCALE_POC_BUILD_REPORT.md): Build and automated testing report for Tailscale companion networking.

### Core Source Files & Scripts
- [MultiplayerLauncher.java](file:///d:/programming%20work/tekken3_multiplayer/analysis/MultiplayerLauncher.java): Complete Java source for the retro UI, Tailscale detection, Base36 coding, socket probing, and crash-safe asset extraction.
- [MainMenuActivity.smali](file:///d:/programming%20work/tekken3_multiplayer/analysis/apktool/smali/com/retroarch/browser/mainmenu/MainMenuActivity.smali): Smali hooks with `onContentChanged()` override, unconditional permission progression, and launcher routing.
- [SplitCompatApplication.smali](file:///d:/programming%20work/tekken3_multiplayer/analysis/apktool/smali/com/google/android/play/core/splitcompat/SplitCompatApplication.smali): Multidex initialization hook.
- [room_server.py](file:///d:/programming%20work/tekken3_multiplayer/analysis/room_server.py): Lightweight REST signaling server.
- [test_room_lifecycle.py](file:///d:/programming%20work/tekken3_multiplayer/analysis/test_room_lifecycle.py): Automated test suite for room server endpoints.
- [test_tailscale_poc.py](file:///d:/programming%20work/tekken3_multiplayer/analysis/test_tailscale_poc.py): Automated test suite for Tailscale detection, IP prioritization, Base36 encoding, and socket probing.
- [compile_launcher.py](file:///d:/programming%20work/tekken3_multiplayer/analysis/compile_launcher.py): Build toolchain compiling Java stubs, invoking D8, generating smali, and placing into `smali_classes2`.
- [verify_build.py](file:///d:/programming%20work/tekken3_multiplayer/analysis/verify_build.py): Cryptographic hash verification script.
