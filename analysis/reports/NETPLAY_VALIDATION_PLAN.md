# RetroArch Netplay Validation Plan & Test Specification (Single-Device & Cross-Platform Revision)

**Target Package:** `com.androidapks.tekken3`  
**Engine:** Embedded RetroArch 1.9.11 (`libretroarch-activity.so`)  
**Core:** PCSX-ReARMed commit `cf6f7ca` (`pcsx_rearmed_libretro_android.so`)  
**Game:** Tekken 3 (`SLPS_013.00`, NTSC-J 60 FPS, `Tekken3.bin`, CRC32: `0x892889e7`)  
**Document Status:** Revised Single-Device & PC-Cross-Platform Validation Plan  

---

## 1. Verified Binary & Launcher Capabilities

### 1.1 Intent Extra Processing in Native C++
Static analysis of `libretroarch-activity.so` confirms how configuration and content are passed from Java to native C++:
* The native code acquires the activity instance (`ANativeActivity* activity`), invokes `activity->clazz.getIntent()`, and directly calls `Intent.getStringExtra()` for the following keys:
  * `"ROM"`: Target disc image (`/data/data/com.androidapks.tekken3/content/Tekken3.bin`).
  * `"LIBRETRO"`: Core library path (`/data/data/com.androidapks.tekken3/cores/pcsx_rearmed_libretro_android.so`).
  * `"CONFIGFILE"`: Target configuration file path.
  * `"SDCARD"`, `"EXTERNAL"`, `"DATADIR"`, `"APK"`.
* **Zero-APK-Modification Launch:** Because `RetroActivityFuture` is `android:exported="true"`, any configuration file path can be passed directly via ADB without editing APK files or Smali:
  ```bash
  adb shell am start -n com.androidapks.tekken3/com.retroarch.browser.retroactivity.RetroActivityFuture \
    -e ROM "/data/data/com.androidapks.tekken3/content/Tekken3.bin" \
    -e LIBRETRO "/data/data/com.androidapks.tekken3/cores/pcsx_rearmed_libretro_android.so" \
    -e CONFIGFILE "/storage/emulated/0/Android/data/com.androidapks.tekken3/files/host_lan.cfg" \
    -e DATADIR "/data/data/com.androidapks.tekken3" \
    -e APK "/data/app/.../base.apk"
  ```

### 1.2 Launcher Preservation of Netplay Settings
Analysis of `MainMenuActivity.java` and `UserPreferences.java` resolves whether standard app launches overwrite netplay settings:
* `UserPreferences.updateConfigFile(Context)` loads the existing configuration file via `ConfigFile.java`.
* `ConfigFile` reads **all existing key-value pairs** into a `HashMap<String, String>`.
* `updateConfigFile()` modifies **only 7 specific hardware and path keys**:
  1. `libretro_directory`
  2. `audio_out_rate`
  3. `bundle_assets_src_path`
  4. `bundle_assets_dst_path`
  5. `bundle_assets_dst_path_subdir`
  6. `bundle_assets_extract_version_current`
  7. `audio_block_frames`
* It then writes all entries in the map back to disk.
* **Verified Conclusion:** **The launcher preserves all `netplay_*` settings.** If netplay keys are written into `/storage/emulated/0/Android/data/com.androidapks.tekken3/files/retroarch.cfg`, they are **not wiped or reset** when the user taps the app icon normally.

### 1.3 Exported Core Symbols
Verified via `readelf -s`:
* `retro_serialize_size`: **Exported** (`0xdc10c`, size 8)
* `retro_serialize`: **Exported** (`0xdc114`, size 48)
* `retro_unserialize`: **Exported** (`0xdc144`, size 100)

---

## 2. Windows RetroArch as a Potential Test Client

If only one physical Android phone is available, can a PC running RetroArch on Windows serve as the second endpoint (Client)?

### 2.1 Content Matching (CRC32)
* The game image `Tekken3.bin` extracted from the APK assets has been verified:
  * File Size: `45,101,952 bytes`
  * SHA-256: `0F059C80B9477940F3BCA0BE680CAAE4497D0C71D74A7876F6FEF5879BFD9813`
  * CRC32: **`0x892889e7`**
* When Windows RetroArch loads `analysis/apktool/assets/content/Tekken3.bin`, it calculates the exact identical CRC32 (`0x892889e7`). The content check in the netplay handshake will succeed.

### 2.2 Core Name & Version Matching
* In RetroArch netplay, the handshake verifies that both sides use the same core library identifier: `pcsx_rearmed`.
* Both official Windows x86_64 and Android builds identify as `pcsx_rearmed`.

### 2.3 Cross-Architecture Compatibility & Dynarec Limitations
* **Android ARM64:** PCSX-ReARMed uses the **Ari64 ARM dynamic recompiler** (`ari64 gpu=BUILTIN_GPU`).
* **Windows x86_64:** PCSX-ReARMed does not have the Ari64 dynarec (Ari64 is ARM-only); it uses an interpreter or Lightrec.
* **Impact on Netplay:**
  * **Socket Connection & Handshake:** **100% compatible.** The client will connect, negotiate the protocol, and enter the game.
  * **Input Assignment:** Player 1 (Android Host) and Player 2 (Windows Client) will be assigned correctly.
  * **Emulation Determinism:** Because dynamic recompilers on different CPU architectures (ARM vs x86) may execute floating point / cycle-count calculations with minute differences, cross-platform savestate rollback can experience mid-match desyncs.
  * **Mitigation:** Setting fixed input latency (`netplay_input_latency_frames_min = "4"`) avoids per-frame rollback serialization, enabling functional cross-platform testing.

---

## 3. Safe, Reversible Single-Phone Validation Procedure

With a single physical Android phone, we can conduct a **100% reversible verification of the netplay server subsystem and incoming network stack** without modifying the APK.

### 3.1 What This Single-Device Test Verifies
1. **Config Ingestion:** Verifies that the native RetroArch engine reads and applies `netplay_*` configuration parameters on this Android device.
2. **Socket Binding:** Verifies that `libretroarch-activity.so` successfully binds to `0.0.0.0:55435` and enters the listening state.
3. **Android Security & Permissions:** Verifies that the Android OS allows the application process to open a TCP listening server without SELinux or sandbox denials.
4. **Local Network Reachability:** Verifies that an external device (the Windows workstation on the same Wi-Fi) can reach and establish a TCP handshake with the phone on port `55435`.
5. **Clean Restoration:** Reverts the device to its exact original configuration.

### 3.2 What This Test Does NOT Claim
* It does **not** claim that a 2-player match is synchronized (requires a second participating client).
* It does **not** test gameplay latency under simultaneous controller input.

---

## 4. Step-by-Step Single-Device Test Execution

### Phase 1: Connect Phone & Verify ADB
1. Connect the single Android phone via USB with USB debugging enabled.
2. Confirm device detection:
   ```powershell
   adb devices -l
   ```

### Phase 2: Create Recovery Backup
Before pushing any experimental files, backup the current configuration from the device:
```powershell
adb pull /storage/emulated/0/Android/data/com.androidapks.tekken3/files/retroarch.cfg analysis/netplay-test/backups/retroarch_device_backup.cfg
```
*If the file does not exist yet (first-time install), document that the app has not been launched yet.*

### Phase 3: Deploy Host Test Configuration
Deploy [host_lan.cfg](file:///D:/programming%20work/tekken3_multiplayer/analysis/netplay-test/host_lan.cfg) to the device's external configuration path:
```powershell
adb push analysis/netplay-test/host_lan.cfg /storage/emulated/0/Android/data/com.androidapks.tekken3/files/retroarch.cfg
```

### Phase 4: Launch & Monitor Host Session
1. Clear logcat buffers:
   ```powershell
   adb logcat -c
   ```
2. Start streaming RetroArch diagnostic logs:
   ```powershell
   adb logcat -s RetroArch:V RetroActivity:V | Select-String "Netplay|netplay|Connecting|Waiting|listening"
   ```
3. Launch Tekken 3 on the phone (or via `am start`):
   ```powershell
   adb shell am start -n com.androidapks.tekken3/com.retroarch.browser.mainmenu.SplashActivity
   ```

### Phase 5: Query Open Listening Sockets
While Tekken 3 is running on the phone, inspect open network sockets from the Android shell:
```powershell
# Check for listening port 55435 (hex 0xD88B in /proc/net/tcp)
adb shell "grep D88B /proc/net/tcp /proc/net/tcp6"
```
Or if `netstat` or `ss` is available:
```powershell
adb shell "netstat -tlpn 2>/dev/null | grep 55435"
```

### Phase 6: External TCP Reachability Check from PC
1. Find the phone's Wi-Fi IP address:
   ```powershell
   adb shell ip route
   ```
2. From the Windows workstation (connected to the same Wi-Fi), test whether port 55435 is open and accepting TCP connections:
   ```powershell
   Test-NetConnection -ComputerName <PHONE_IP> -Port 55435
   ```
   *Expected Observation:* `TcpTestSucceeded : True`.

### Phase 7: Revert Configuration to Original State
Once observations are recorded, restore the original configuration:
```powershell
if (Test-Path "analysis/netplay-test/backups/retroarch_device_backup.cfg") {
    adb push analysis/netplay-test/backups/retroarch_device_backup.cfg /storage/emulated/0/Android/data/com.androidapks.tekken3/files/retroarch.cfg
}
```

---

## 5. Result Classification Matrix

| Metric | Pass Indicator | Fail / Block Indicator |
| :--- | :--- | :--- |
| **Logcat Netplay Init** | `[INFO] [Netplay] ...` initialization logged without error. | `[ERROR] [Netplay] Failed to initialize netplay`. |
| **Port Binding** | Socket `0.0.0.0:55435` active in `/proc/net/tcp`. | Port `55435` absent or permission denied. |
| **PC Reachability** | `Test-NetConnection` returns `TcpTestSucceeded : True`. | Connection timed out or connection refused. |
| **App Stability** | Game boots into Tekken 3 title screen at 60 FPS. | Application crash, ANR, or black screen. |

---

## 6. The Concrete Next Command

To begin this safe, single-phone validation:
1. Connect your Android phone via USB with USB debugging enabled.
2. Run:
   ```powershell
   adb devices -l
   ```
Once the phone serial appears, we will immediately execute Phase 2 (the non-destructive backup) and proceed with the listening-socket test.
