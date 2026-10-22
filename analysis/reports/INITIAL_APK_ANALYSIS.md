# Initial APK Reverse Engineering & Engine Analysis Report

**Target Package:** `com.androidapks.tekken3`  
**Application Name:** Tekken 3  
**Analysis Date:** 2026-10-09  
**Analysis Mode:** Read-Only Static Decompilation & Architectural Audit  

---

## 1. APK Metadata & Integrity

| Property | Value |
| :--- | :--- |
| **Filename** | `tekken3.apk` |
| **Path** | `D:\programming work\tekken3_multiplayer\tekken3.apk` |
| **File Size** | `63,052,988 bytes` (~60.13 MB) |
| **SHA-256 Hash** | `CA68CAD9A3CBD8A781C0805BE520D956F9062E91E7CD33DBD53A2298A6871713` |
| **Package Name** | `com.androidapks.tekken3` |
| **Version Code** | `6` |
| **Version Name** | `1.3` |
| **Min SDK** | `19` (Android 4.4 KitKat) |
| **Target SDK** | `29` (Android 10) |
| **Compile SDK** | `33` (Android 13) |
| **Main Launcher Activity** | `com.retroarch.browser.mainmenu.SplashActivity` |
| **Game Activity** | `com.retroarch.browser.retroactivity.RetroActivityFuture` |
| **Native Library Architectures (lib/)** | `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` |
| **Bundled Core Architectures (assets/)** | `arm64-v8a` (`pcsx_rearmed_libretro_android.so`), `armeabi-v7a` (`pcsx_rearmed_libretro_android_armv7.so`) |

---

## 2. Tooling & Environment Verification

All decompilation and binary inspection tools were discovered locally on the workstation and verified functional without installing external packages:

* **Java Runtime:** Eclipse Adoptium OpenJDK 21.0.11 (`C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin\java.exe`)
* **APKTool:** v3.0.3 (`D:\programming work\AVU mod\tools\apktool\apktool_3.0.3.jar`)
* **JADX:** v1.5.6 (`D:\programming work\AVU mod\tools\jadx\bin\jadx.bat`)
* **Android SDK Build Tools (aapt/aapt2):** v34.0.0 (`C:\Users\rudraksh\AppData\Local\Android\Sdk\build-tools\34.0.0\aapt.exe`)
* **Android SDK Platform Tools (adb):** (`C:\Users\rudraksh\AppData\Local\Android\Sdk\platform-tools\adb.exe`)
* **Binary Analysis Utilities:** MinGW GCC readelf (`C:\MinGW\bin\readelf.exe`), Python 3.11.6

---

## 3. Decompilation Execution & Output Structure

### 3.1 Commands Executed

1. **APKTool Decoding:**
   ```powershell
   java -jar "D:\programming work\AVU mod\tools\apktool\apktool_3.0.3.jar" d "tekken3.apk" -o "analysis\apktool" -f
   ```
   *Status:* Exited with code `0`. Smali bytecode (`classes.dex` -> `smali`, `classes2.dex` -> `smali_classes2`), `AndroidManifest.xml`, resources, assets, and native libraries decoded completely.

2. **JADX Decompilation:**
   ```powershell
   & "D:\programming work\AVU mod\tools\jadx\bin\jadx.bat" -d "analysis\jadx" "tekken3.apk"
   ```
   *Status:* Exited with code `1` due to 86 class recovery warnings across 4,204 total classes.
   *Error Analysis:* All 86 warnings were isolated to third-party SDK dependencies (`androidx.core`, `androidx.recyclerview`, `kotlinx.coroutines`, `com.google.android.gms`, `com.unity3d`). Zero decompilation failures occurred in the application or RetroArch codebases (`com.retroarch.*`).

### 3.2 Output Directory Layout

```
tekken3_multiplayer/
├── tekken3.apk                         # Original pristine APK
├── analysis/
│   ├── original/
│   │   └── tekken3.apk                 # Preserved reference copy
│   ├── apktool/
│   │   ├── AndroidManifest.xml         # Decoded manifest
│   │   ├── apktool.yml                 # Metadata & build flags
│   │   ├── assets/
│   │   │   ├── content/Tekken3.bin     # Embedded PS1 game image (45.1 MB)
│   │   │   ├── cores/                  # Embedded libretro emulator cores
│   │   │   ├── overlays/               # On-screen gamepad overlay configs & textures
│   │   │   └── playlists/              # RetroArch playlist entries (Tekken3.lpl)
│   │   ├── lib/                        # Compiled native libraries (libretroarch-activity.so)
│   │   ├── res/                        # Android resources & layouts
│   │   ├── smali/                      # Primary dex Smali bytecode
│   │   └── smali_classes2/             # Secondary dex Smali bytecode
│   ├── jadx/
│   │   ├── sources/                    # Decompiled Java source tree
│   │   └── resources/                  # Decompiled resource assets
│   └── reports/
│       └── INITIAL_APK_ANALYSIS.md     # This report
```

### 3.3 Dependency & Split APK Analysis

Although `AndroidManifest.xml` references `com.google.android.play.core.splitcompat.SplitCompatApplication` and `RetroActivityCommon.java` retains Play Core `SplitInstallManagerFactory` listeners, the method `isPlayStoreBuild()` is hardcoded to return `false`.

All required runtime components:
* Game ROM (`assets/content/Tekken3.bin`)
* Cores (`assets/cores/pcsx_rearmed_libretro_android.so`)
* Playlists (`assets/playlists/Tekken3.lpl`)
* Touch overlays (`assets/overlays/gamepads/neo-retropad/`)

are completely embedded within this single base APK. **The APK is self-contained and monolithic; it does not depend on split APKs or remote Google Play Feature Delivery.**

---

## 4. Game Engine & Architecture Identification

### 4.1 Frontend / Emulator Host

* **Engine:** Libretro **RetroArch Frontend**
* **Frontend Version:** `RetroArch 1.9.11` (Compiled December 31, 2024)
* **Hosting Binary:** `libretroarch-activity.so` (Subclassed via Android `android.app.NativeActivity`)
* **Repackaging Author:** Repackaged by third-party entity (BaigTech / Sarmad Anjum) with Unity Ads (Game ID `4910884`), Firebase Analytics, and an automated startup sequence directing directly into Tekken 3.

### 4.2 PlayStation Emulator Core

* **Core Name:** **PCSX-ReARMed**
* **Core Version / Commit:** `PCSX-ReARMed cf6f7ca` (PCSX v1.9)
* **Execution Engine:** Ari64 ARM dynamic recompiler (`ari64 gpu=BUILTIN_GPU`)
* **Binary Locations:**
  * ARM64: `assets/cores/pcsx_rearmed_libretro_android.so` (ELF 64-bit LSB shared object, ARM aarch64)
  * ARM32: `assets/cores/pcsx_rearmed_libretro_android_armv7.so` (ELF 32-bit LSB shared object, ARM EABI5)
* **Loading Mechanism:** Bundled directly in APK assets. Upon execution, RetroArch extracts assets to `/data/data/com.androidapks.tekken3/cores/`, and `MainMenuActivity.java` invokes `RetroActivityFuture` specifying `LIBRETRO` intent extras matching the device CPU architecture (`aarch64` vs 32-bit ARM).

### 4.3 Game ROM Details

* **File:** `assets/content/Tekken3.bin`
* **File Size:** `45,101,952 bytes` (~43.01 MB)
* **SHA-256 Hash:** `0F059C80B9477940F3BCA0BE680CAAE4497D0C71D74A7876F6FEF5879BFD9813`
* **Format:** Raw 2352-byte/sector PlayStation CD-ROM Mode 2 Form 1 image (Sync pattern `00 FF FF FF FF FF FF FF FF FF FF 00`, Volume Identifier `ION PSX_CD`)
* **Disc ID:** **`SLPS_013.00`** (Tekken 3 Original Japanese Release, NTSC-J, 60 FPS)

### 4.4 Input Handling Architecture

1. **Android Native Event Loop:**  
   `RetroActivityFuture` inherits `NativeActivity`. Android OS input events are received directly in C/C++ inside `libretroarch-activity.so` via `AInputQueue` (`AInputQueue_getEvent`, `AMotionEvent`, `AKeyEvent`) attached to `ALooper`.
2. **Touch Screen Controls:**  
   Touch events are intercepted by RetroArch's native overlay subsystem (`input_overlay.c`). The layout is defined by `assets/overlays/gamepads/neo-retropad/neo-retropad.cfg`:
   * Screen touches inside radial and rectangular zones map to standard `RetroPad` controls (`Up`, `Down`, `Left`, `Right`, `Cross`, `Circle`, `Square`, `Triangle`, `L1`, `R1`, `L2`, `R2`, `Select`, `Start`).
   * An RGUI toggle button (`overlay0_desc19`) is mapped at normalized screen position `(0.501, 0.117)` to open the RetroArch internal menu.
3. **Core Mapping:**  
   The Libretro environment routes `RetroPad` states to `pcsx_rearmed` port 0 (Player 1) via standard digital controller emulation.
4. **Physical Gamepads:**  
   Physical Bluetooth / USB gamepads are detected via Android `InputDevice` and handled directly by RetroArch's internal autoconf driver.

### 4.5 Native Source Code Status

* **APK Contents:** Contains **only compiled shared libraries** (`.so`), with symbol tables largely stripped. No C/C++ source code is present in the package.
* **Open Source Provenance:** Both RetroArch (`libretroarch-activity.so`) and PCSX-ReARMed (`pcsx_rearmed_libretro_android.so`) are open-source projects licensed under GNU GPL. Their source code is publicly available upstream.

---

## 5. Existing Netplay Capabilities & Feasibility

### 5.1 Bundled Netplay Infrastructure

Deep inspection of `libretroarch-activity.so` reveals that **the full RetroArch Netplay subsystem is compiled into the binary**:

* **Core Netplay Symbols Present:**
  * `netplay_enable`, `netplay_mode` (Host / Client / Spectator)
  * `netplay_ip_address`, `netplay_ip_port` (Default TCP port `55435`)
  * `netplay_use_mitm_server`, `netplay_mitm_server` (Libretro relay / MITM traversal)
  * `netplay_nat_traversal`, `netplay_announce_nat_traversal` (UPnP port mapping)
  * `netplay_input_latency_frames_min`, `netplay_input_latency_frames_range` (Input delay tuning)
  * `netplay_stateless_mode`, `netplay_sync_pre_frame`, `netplay_sync_post_frame`
  * `netplay_request_device_p1`, `netplay_request_device_p2` (Player 1 vs Player 2 assignment)
* **Core Serialization Hooks:**
  `pcsx_rearmed_libretro_android.so` implements the required Libretro serialization callbacks:
  * `retro_serialize`
  * `retro_unserialize`
  * `retro_serialize_size`

### 5.2 Why Multiplayer is Currently Inactive

In this specific APK:
1. `MainMenuActivity.java` sets up an `Intent` pointing directly to local single-player execution (`ROM`, `LIBRETRO`, `CONFIGFILE`).
2. `UserPreferences.java` generates `retroarch.cfg` with defaults that leave `netplay_mode` / `netplay_enable` unconfigured (`false`).
3. There is no user interface in the launcher allowing a user to host or join an online room.

### 5.3 Technical Assessment for Playing Across Different Networks (Wi-Fi / Mobile)

| Challenge | Impact on Tekken 3 | Feasible Solution |
| :--- | :--- | :--- |
| **NAT / Carrier-Grade NAT (CGNAT)** | Mobile carriers and home routers block incoming unsolicited connections on port 55435. | 1. Enable RetroArch's built-in **MITM relay server** (`netplay_use_mitm_server = true`), which routes traffic via outbound TCP.<br>2. Run a lightweight dedicated relay/signaling proxy.<br>3. P2P overlay (e.g. Tailscale / WireGuard or WebRTC data channel bridge). |
| **Network Latency & Jitter** | Tekken 3 is a fast 60 FPS fighting game. Desyncs or lag spikes ruin gameplay. | PCSX-ReARMed savestates are ~2.5–3.5 MB, making per-frame rollback heavy on mobile CPUs. **Deterministic lockstep with fixed input delay** (`netplay_input_latency_frames_min = 3` to `5` frames) provides smooth 60 FPS fighting game netplay over 40–80 ms connections. |
| **ROM / Core Determinism** | Both players must run byte-identical ROMs and core settings or a desynchronization occurs. | Solved automatically: both players will be running this exact APK with identical ROM (`SLPS_013.00`) and identical core (`PCSX-ReARMed cf6f7ca`). |
| **Player 1 vs Player 2 Input** | Client must control Player 2; Host must control Player 1. | RetroArch Netplay automatically maps Host to port 0 (Player 1) and connecting Client to port 1 (Player 2). |

---

## 6. Unresolved Questions & Technical Unknowns

1. **Public Relay Availability:** Does the default Libretro MITM relay server (`relay.retroarch.com`) accept connections from RetroArch 1.9.11 builds without an API token, or should a private lightweight TCP relay server be configured?
2. **PCSX-ReARMed Savestate Sync Overhead:** Does the bundled PCSX-ReARMed core run stably in RetroArch netplay without mid-game desyncs caused by Ari64 dynarec non-determinism, or does it require switching the core CPU mode or tuning serialization flags?
3. **Menu Access vs Dedicated Lobby UI:** Can users connect via RetroArch's built-in RGUI Netplay menu (which is already compiled into the binary), or is a native Android "Host / Join" dialog in Java required for a seamless one-tap user experience?

---

## 7. Recommended Next Steps

1. **Test Netplay via Configuration (`retroarch.cfg`):**  
   Configure `retroarch.cfg` parameters (`netplay_enable`, `netplay_mode`, `netplay_ip_address`, `netplay_use_mitm_server`) to evaluate how the embedded RetroArch 1.9.11 and PCSX-ReARMed core behave in network play between two test endpoints.
2. **Evaluate Relay / NAT Traversal Options:**  
   Verify whether RetroArch's built-in relay server is responsive, or implement a minimal TCP relay / room coordinator to connect two mobile devices without port forwarding.
3. **Design the Multiplayer Flow:**  
   Design an Android-level Host/Join dialogue in `MainMenuActivity` that allows two players to share a room code or direct IP and automatically configures RetroArch before launching `RetroActivityFuture`.
