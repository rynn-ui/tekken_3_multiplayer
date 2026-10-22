import os, subprocess, shutil

os.makedirs('temp_test', exist_ok=True)
java_src = """package test;
public class TestHello {
    public static String getMsg() { return "Hello Smali"; }
}
"""
with open('temp_test/TestHello.java', 'w') as f:
    f.write(java_src)

android_jar = r'C:\Users\rudraksh\AppData\Local\Android\Sdk\platforms\android-33\android.jar'
d8_bat = r'C:\Users\rudraksh\AppData\Local\Android\Sdk\build-tools\34.0.0\d8.bat'

subprocess.check_call(['javac', '-cp', android_jar, 'temp_test/TestHello.java'])
subprocess.check_call([d8_bat, '--output', 'temp_test', 'temp_test/TestHello.class'])
print('Generated classes.dex! Size:', os.path.getsize('temp_test/classes.dex'))
