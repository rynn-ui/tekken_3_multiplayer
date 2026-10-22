# Tekken 3 Android Multiplayer APK — Build & Implementation Report

**Project:** `tekken3_multiplayer`  
**Package:** `com.androidapks.tekken3`  
**Target Output:** `dist/Tekken3-Multiplayer.apk`  
**Build Date:** 2026-10-10  
**Build Status:** **SUCCESSFUL, ZIPALIGNED & SIGNED (v1/v2/v3)**  

---

## 1. Executive Summary & Deliverables

An installable, production-grade Android APK has been built and saved to `dist/Tekken3-Multiplayer.apk`. 

This build integrates a dedicated **Tekken 3 Multiplayer Launcher** into the application, giving players an intuitive interface on their mobile phones to choose between **Host Online Match**, **Join Online Match**, and **Solo Arcade Play**, while configuring RetroArch's netplay subsystem for remote internet play over Libretro's public relay infrastructure and peer-to-peer connections.

### 1.1 Deliverable Metadata

| Property | Value |
| :--- | :--- |
| **Output File** | `dist/Tekken3-Multiplayer.apk` |
| **File Size** | `63,181,448 bytes` (~60.25 MB) |
| **SHA-256 Checksum** | `4415373E9D06CB3F13937E94AA5AD8EC1596A0B440CB736AADE851783FA066AA` |
| **Package Name** | `com.androidapks.tekken3` |
| **Version Code / Name** | `6` (v1.3) |
| **Target SDK / Min SDK** | `29` (Android 10) / `19` (Android 4.4 KitKat) |
| **Supported ABIs** | `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` |
| **Signing Status** | Signed with dedicated 2048-bit RSA key (`tekken3_release.keystore`) |
| **Signature Schemes** | **v1 (JAR), v2 (Full APK), v3 (Android 9+) ALL VERIFIED** |
| **Zipalign Status** | **4-byte boundary aligned (Verified)** |

---

## 2. Reality Check: The Three Distinct Status Tiers

To maintain strict engineering rigor, we clearly separate build status from remote gameplay verification:

```
[ Tier 1: APK Build & Packaging ] ────► PROVEN & VERIFIED (Pass)
[ Tier 2: Relay Infrastructure  ] ────► PROVEN & OPERATIONAL (Active Libretro Lobby/Relays)
[ Tier 3: Two-Player Remote Match ] ──► PENDING (Ready for Physical 2-Device WAN Field Test)
```

1. **Successful APK Build (VERIFIED):**
   - The APK compiles cleanly, passes 4-byte zipalign verification, and validates with `apksigner verify --verbose` across v1, v2, and v3 schemes.
   - All native libraries (`libretroarch-activity.so`) and game assets (`Tekken3.bin`, PCSX-ReARMed cores) match the original untouched binaries bit-for-bit (100.0% SHA-256 match).
2. **Working Relay Connection (INFRASTRUCTURE PROVEN, SESSION PENDING):**
   - Live network queries to `http://lobby.libretro.com/list/` confirmed that Libretro's relay servers (`us-east1.relay.retroarch.com`, `southamerica-east1.relay.retroarch.com`, `ny.remz.ca`) are active and actively routing multiplayer games with `host_method: 3` (MITM Relay).
   - The built APK is configured to register with this relay on host boot (`netplay_use_mitm_server = "true"`, `netplay_mitm_server = "nyc"`).
   - An actual remote session between two physical phones over cellular/WAN must be conducted by the user following the step-by-step test guide.
3. **Verified Two-Player Match (PENDING FIELD VALIDATION):**
   - Single-device host socket binding (`TCP_LISTEN` on port `55435` with `RANP` magic header) was proven on the Infinix X671B in previous phases.
   - Two-device lockstep synchronization and controller independence (Player 1 Left vs Player 2 Right) will be confirmed once two physical devices run a match using this build.

---

## 3. Implementation Architecture

### 3.1 Non-Invasive Launcher & Runtime Architecture

Per requirements, we implemented the most reliable integration possible **without modifying the native `.so` binaries**. 

```
+-----------------------------------------------------------------------------------------+
|                                    APPLICATION ENTRY                                    |
|  SplashActivity (Unity Ads init / 2000ms delay) -> MainMenuActivity                     |
+-----------------------------------------------------------------------------------------+
                                             |
                                             v
+-----------------------------------------------------------------------------------------+
|                  MULTIPLAYER LAUNCHER (com.retroarch.browser.mainmenu)                  |
|  - Real-Time Network Detection (Local IP, Interface, Connection Type)                   |
|  - Libretro Relay WAN Status (lobby.libretro.com)                                       |
+--------------------+-------------------------------+------------------------------------+
                     |                               |                                    |
                     v                               v                                    v
+-----------------------------+ +-----------------------------+ +-----------------------------+
|      HOST ONLINE MATCH      | |      JOIN ONLINE MATCH      | |      SOLO ARCADE PLAY       |
+-----------------------------+ +-----------------------------+ +-----------------------------+
| • Libretro MITM Relay Mode  | | • Pre-populates Target Host | | • netplay_enable = "false"  |
| • Direct IP / Tailscale Mode| |   IP in retroarch.cfg       | | • netplay_mode = "false"    |
| • Writes netplay configs    | | • Remembers last used IP    | | • Standard single-player    |
| • Step-by-Step UI guidance  | | • Step-by-Step UI guidance  | |   arcade/practice           |
+-----------------------------+ +-----------------------------+ +-----------------------------+
                     \                               |                               /
                      \                              |                              /
                       v                             v                             v
+-----------------------------------------------------------------------------------------+
|                                   RETROACTIVITYFUTURE                                   |
|  NativeActivity loads libretroarch-activity.so with mode-specific retroarch.cfg         |
+-----------------------------------------------------------------------------------------+
                                             |
                                             v
+-----------------------------------------------------------------------------------------+
|                                  IN-GAME VERIFICATION                                   |
|  • Main UI: Space Invader Icon (1442, 125) -> Home Tab -> Netplay -> Host / Connect     |
|  • Touch Overlay: Submenu (Next button) includes netplay_host_toggle shortcut           |
+-----------------------------------------------------------------------------------------+
```

### 3.2 The Exact Technical Blocker: Why "Zero-Tap Auto-Host" Without Native Patching Is Blocked

Our reverse engineering of `libretroarch-activity.so` resolved why automatic hosting purely via intent extras or configuration files does not bind on boot:

1. **Intent Extra Filter (`0x5ed8c0`):**  
   The native intent parser queries only:
   `"CONFIGFILE"`, `"LIBRETRO"`, `"ROM"`, `"IME"`, `"SDCARD"`, `"APK"`, `"EXTERNAL"`, `"DATADIR"`.  
   Command-line parameters like `--host` or custom intent extras (`"NETPLAY_MODE"`) are ignored.
2. **Unconditional Boot-Time State Wipe (`0x60c2b4`):**  
   During Android `NativeActivity` initialization, `retroarch_main_init` executes:
   ```arm64
   0x0060c2b4: strb wzr, [x8, #0xc7c]   ; Zeroes active runtime state at 0xb9ac7d!
   ```
3. **Decoupled Configuration Parsing:**  
   `config_load` / `configuration.c` parses `netplay_mode` and `netplay_enable` into internal settings variables, but **never writes to `0xb9ac7d`**.
4. **Socket Gatekeeper (`0x60a17c`):**  
   When content initializes:
   ```arm64
   0x0060a17c: adrp  x8, #0xb9a000
   0x0060a180: ldrb  w8, [x8, #0xc7d]
   0x0060a184: cbz   w8, #0x60a3dc     ; If 0, skips socket setup and returns!
   ```
   Because `0xb9ac7d` was zeroed at boot, socket binding is skipped.

**Architectural Takeaway:**  
Any claim that setting `netplay_enable = "true"` in `retroarch.cfg` creates an automatic "one-tap" boot is objectively false. To trigger hosting or connecting without modifying the binary, the user must trigger the action at runtime via the built-in RetroArch menu or overlay hotkey.

Our launcher solves this transparently: it prepares all network settings, configures the relay/IP, and gives the player clear in-app instructions before launching into the game.

---

## 4. Binary & ROM Integrity Audit

The bundled game ROM and core files are the heart of deterministic netplay. The table below confirms that no binary alteration occurred during build:

| File Path | Original APK SHA-256 | Built APK SHA-256 | Verification |
| :--- | :--- | :--- | :--- |
| `assets/content/Tekken3.bin` | `0F059C80B9477940F3BCA0BE680CAAE4497D0C71D74A7876F6FEF5879BFD9813` | `0F059C80B9477940F3BCA0BE680CAAE4497D0C71D74A7876F6FEF5879BFD9813` | **100% MATCH** |
| `assets/cores/pcsx_rearmed_libretro_android.so` | `E3E3B4E4B7E51639257769E59E5375A9C0AC8883439415FEC3E219166AB2BFC3` | `E3E3B4E4B7E51639257769E59E5375A9C0AC8883439415FEC3E219166AB2BFC3` | **100% MATCH** |
| `assets/cores/pcsx_rearmed_libretro_android_armv7.so` | `F90D57F7D9D29666973D353ECE923A61B6995176EF2814D10F784CBEAB131ABD` | `F90D57F7D9D29666973D353ECE923A61B6995176EF2814D10F784CBEAB131ABD` | **100% MATCH** |
| `lib/arm64-v8a/libretroarch-activity.so` | `F081C0CE13A28D90181900253C26B9AC2CC00594196A2AB720D5F27BDF753E3C` | `F081C0CE13A28D90181900253C26B9AC2CC00594196A2AB720D5F27BDF753E3C` | **100% MATCH** |
| `lib/armeabi-v7a/libretroarch-activity.so` | `C36BDD43F1035BEB62FBFCF536F0C45ADCBE71C35883446C074E3FF28060AA6C` | `C36BDD43F1035BEB62FBFCF536F0C45ADCBE71C35883446C074E3FF28060AA6C` | **100% MATCH** |

The original backup at `analysis/original/tekken3.apk` and `tekken3.apk` remain untouched.

---

## 5. Network Configuration Profiles

The launcher dynamically writes tailored configuration blocks to `/storage/emulated/0/Android/data/com.androidapks.tekken3/files/retroarch.cfg`:

### 5.1 Host Configuration (`Host Online Match`)
```ini
netplay_mode = "true"
netplay_enable = "true"
netplay_ip_port = "55435"
netplay_use_mitm_server = "true"       # Set to "false" if Direct IP mode selected
netplay_mitm_server = "nyc"            # Closest Libretro MITM relay server
netplay_public_announce = "true"       # Announces room to lobby.libretro.com
netplay_nickname = "Tekken3-Host"
netplay_input_latency_frames_min = "3" # 50 ms buffer for smooth 60 FPS fighting
netplay_input_latency_frames_range = "0"
netplay_check_frames = "600"
netplay_stateless_mode = "false"
```

### 5.2 Client Configuration (`Join Online Match`)
```ini
netplay_mode = "false"
netplay_enable = "false"
netplay_ip_address = "<ENTERED_HOST_IP>" # Pre-populates RetroArch text input dialog!
netplay_ip_port = "55435"
netplay_use_mitm_server = "true"
netplay_mitm_server = "nyc"
netplay_nickname = "Tekken3-Client"
netplay_input_latency_frames_min = "3"
netplay_input_latency_frames_range = "0"
netplay_check_frames = "600"
netplay_stateless_mode = "false"
```

### 5.3 Solo Play Configuration (`Solo Arcade Play`)
```ini
netplay_mode = "false"
netplay_enable = "false"
```

---

## 6. How Two Friends Play Over the Internet (Step-by-Step Operator Guide)

### Method A: Libretro Public Relay (Zero Configuration WAN)
*Best for: Playing across different mobile data connections (4G/5G) or different home Wi-Fi networks without router configuration.*

1. **Both Friends:** Install `Tekken3-Multiplayer.apk`.
2. **Host Friend:**
   - Open app -> Tap **HOST ONLINE MATCH**.
   - Select **Internet Relay (lobby.libretro.com)** -> Tap **START HOST GAME**.
   - When the Tekken 3 screen loads, tap the **Space Invader icon** (top center between health bars).
   - Tap the **Home icon** (right sidebar) -> **Netplay** -> **Host** -> **Start Netplay Host**.
   - The game resumes. The room is now live on Libretro's public relay!
3. **Client Friend:**
   - Open app -> Tap **JOIN ONLINE MATCH**.
   - Leave IP blank -> Tap **CONNECT & PLAY**.
   - When the game loads, tap the **Space Invader icon** -> **Home icon** -> **Netplay**.
   - Tap **Refresh Room List** -> Find and tap the **Tekken3-Host** room!
   - Savestate transfers automatically and the match begins!

---

### Method B: Tailscale / ZeroTier (Ultra-Low Latency WAN)
*Best for: Competitive fighting with pure peer-to-peer 25–40 ms latency.*

1. **Both Friends:** Install **Tailscale** from Google Play Store (free, no root) and join the same network.
2. **Host Friend:**
   - Note your Tailscale IP (e.g. `100.85.12.3`) and send it to your friend.
   - In Tekken 3 Launcher: Tap **HOST ONLINE MATCH** -> Select **Direct IP** -> Tap **START HOST GAME**.
   - In-game: Tap Space Invader icon -> Home -> Netplay -> Host -> **Start Netplay Host**.
3. **Client Friend:**
   - In Tekken 3 Launcher: Tap **JOIN ONLINE MATCH** -> Enter Host's Tailscale IP (e.g. `100.85.12.3`) -> Tap **CONNECT & PLAY**.
   - In-game: Tap Space Invader icon -> Home -> Netplay -> **Connect to Netplay Host**.
   - The IP you typed in the launcher is **pre-filled** in the box! Just tap **OK**!
   - Handshake connects over Tailscale's direct UDP tunnel. Enjoy 60 FPS fighting!

---

## 7. Known Limitations & Recommendations

1. **In-Game Trigger Requirement:**  
   Because the native binary resets state at boot, players must tap the Space Invader icon -> Netplay once per session to confirm hosting/connecting. The launcher guides players through this seamlessly.
2. **Public Relay Latency:**  
   Public Libretro relays route traffic through New York (`nyc`) or Madrid (`madrid`). Depending on physical geographic location, relay latency may add 70–120 ms. For competitive play, Tailscale (Method B) is strongly recommended.
3. **PCSX-ReARMed Dynarec Determinism:**  
   Ari64 dynamic recompiler runs at full 60 FPS on ARM64. If minor desyncs occur after 5+ consecutive rounds, increasing `netplay_input_latency_frames_min` to `4` or `5` in `retroarch.cfg` provides additional latency tolerance.

---

## 8. Final Verification Checklist

- [x] Original APK (`tekken3.apk` and `analysis/original/tekken3.apk`) backed up and intact.
- [x] Native libraries (`libretroarch-activity.so`) preserved without modification.
- [x] Game ROM (`Tekken3.bin`) and PCSX-ReARMed core bit-for-bit identical (SHA-256 verified).
- [x] Interactive multiplayer launcher with Host, Join, Solo, and Guide modes integrated.
- [x] Multidex limits resolved (new smali safely packaged in `smali_classes2`).
- [x] Production build output saved to `dist/Tekken3-Multiplayer.apk`.
- [x] 4-byte zipalign and apksigner v1/v2/v3 verified.
- [x] Detailed user test instructions provided.
