import hashlib, zipfile, os

def get_file_sha256(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest().upper()

def get_zip_entry_sha256(zip_path, entry_name):
    h = hashlib.sha256()
    with zipfile.ZipFile(zip_path) as z:
        with z.open(entry_name) as f:
            while chunk := f.read(65536):
                h.update(chunk)
    return h.hexdigest().upper()

apk_dist = 'dist/Tekken3-Multiplayer.apk'
apk_orig = 'tekken3.apk'

print('APK Output File:', apk_dist)
print('APK Output Size:', os.path.getsize(apk_dist), 'bytes')
print('APK SHA-256:', get_file_sha256(apk_dist))

critical_entries = [
    'assets/content/Tekken3.bin',
    'assets/cores/pcsx_rearmed_libretro_android.so',
    'assets/cores/pcsx_rearmed_libretro_android_armv7.so',
    'lib/arm64-v8a/libretroarch-activity.so',
    'lib/armeabi-v7a/libretroarch-activity.so'
]

print('\n--- Content & Core Integrity Check ---')
for e in critical_entries:
    orig_hash = get_zip_entry_sha256(apk_orig, e)
    dist_hash = get_zip_entry_sha256(apk_dist, e)
    status = "MATCH" if orig_hash == dist_hash else "MISMATCH"
    print(f'[{status}] {e}')
    print(f'   Original: {orig_hash}')
    print(f'   Dist:     {dist_hash}')
