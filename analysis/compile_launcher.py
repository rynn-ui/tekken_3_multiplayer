import os, subprocess, shutil, zipfile

build_dir = "build_stubs"
if os.path.exists(build_dir):
    shutil.rmtree(build_dir)
os.makedirs(build_dir, exist_ok=True)

# 1. Write stub classes
stubs = {
    "com/retroarch/browser/preferences/util/ConfigFile.java": """
package com.retroarch.browser.preferences.util;
import java.io.IOException;
public class ConfigFile {
    public ConfigFile(String path) {}
    public void setString(String key, String value) {}
    public void write(String path) throws IOException {}
}
""",
    "com/retroarch/browser/preferences/util/UserPreferences.java": """
package com.retroarch.browser.preferences.util;
import android.content.Context;
public class UserPreferences {
    public static String getDefaultConfigPath(Context context) { return ""; }
}
""",
    "com/retroarch/browser/retroactivity/RetroActivityFuture.java": """
package com.retroarch.browser.retroactivity;
import android.app.Activity;
public class RetroActivityFuture extends Activity {}
""",
    "com/retroarch/browser/mainmenu/MainMenuActivity.java": """
package com.retroarch.browser.mainmenu;
import android.app.Activity;
import android.content.Intent;
public class MainMenuActivity extends Activity {
    public static void startRetroActivity(Intent intent, String r, String l, String c, String i, String d, String a) {}
}
"""
}

for path, code in stubs.items():
    fp = os.path.join(build_dir, path)
    os.makedirs(os.path.dirname(fp), exist_ok=True)
    with open(fp, "w") as f:
        f.write(code.strip())

# Copy MultiplayerLauncher.java into package directory
dest_ml = os.path.join(build_dir, "com/retroarch/browser/mainmenu/MultiplayerLauncher.java")
shutil.copy("analysis/MultiplayerLauncher.java", dest_ml)

android_jar = r"C:\Users\rudraksh\AppData\Local\Android\Sdk\platforms\android-33\android.jar"
d8_bat = r"C:\Users\rudraksh\AppData\Local\Android\Sdk\build-tools\34.0.0\d8.bat"
apktool_jar = r"D:\programming work\AVU mod\tools\apktool\apktool_3.0.3.jar"

# Collect all java files
java_files = []
for root, dirs, files in os.walk(build_dir):
    for f in files:
        if f.endswith(".java"):
            java_files.append(os.path.join(root, f))

print("Compiling java files:", len(java_files))
cmd_javac = ["javac", "--release", "8", "-cp", android_jar] + java_files
subprocess.check_call(cmd_javac)
print("Javac success!")

# Find generated class files for MultiplayerLauncher
ml_classes = []
for root, dirs, files in os.walk(os.path.join(build_dir, "com/retroarch/browser/mainmenu")):
    for f in files:
        if f.startswith("MultiplayerLauncher") and f.endswith(".class"):
            ml_classes.append(os.path.join(root, f))

print("Found compiled MultiplayerLauncher class files:", ml_classes)

# Dex only MultiplayerLauncher classes (not the stubs!)
out_dex_dir = os.path.join(build_dir, "dex_out")
os.makedirs(out_dex_dir, exist_ok=True)
cmd_d8 = [d8_bat, "--lib", android_jar, "--output", out_dex_dir] + ml_classes
subprocess.check_call(cmd_d8)
print("D8 success! Produced:", os.path.join(out_dex_dir, "classes.dex"))

# Baksmali the generated classes.dex
dummy_zip = os.path.join(build_dir, "launcher_dex.zip")
with zipfile.ZipFile(dummy_zip, "w") as z:
    z.write(os.path.join(out_dex_dir, "classes.dex"), "classes.dex")

out_smali = os.path.join(build_dir, "smali_out")
cmd_baksmali = ["java", "-jar", apktool_jar, "d", "-r", "-f", dummy_zip, "-o", out_smali]
subprocess.check_call(cmd_baksmali)

# Clean existing MultiplayerLauncher smali from smali/ if any
old_smali_dir = r"analysis/apktool/smali/com/retroarch/browser/mainmenu"
for f in os.listdir(old_smali_dir):
    if f.startswith("MultiplayerLauncher"):
        os.remove(os.path.join(old_smali_dir, f))

# Copy the smali files to analysis/apktool/smali_classes2/com/retroarch/browser/mainmenu/
src_smali_dir = os.path.join(out_smali, "smali/com/retroarch/browser/mainmenu")
target_smali_dir = r"analysis/apktool/smali_classes2/com/retroarch/browser/mainmenu"
os.makedirs(target_smali_dir, exist_ok=True)
# Clean old smali from smali_classes2 first
for f in os.listdir(target_smali_dir):
    if f.startswith("MultiplayerLauncher"):
        os.remove(os.path.join(target_smali_dir, f))

for f in os.listdir(src_smali_dir):
    src_file = os.path.join(src_smali_dir, f)
    dst_file = os.path.join(target_smali_dir, f)
    shutil.copy(src_file, dst_file)
    print("Copied smali file to target:", f)

print("COMPLETE SUCCESS!")
