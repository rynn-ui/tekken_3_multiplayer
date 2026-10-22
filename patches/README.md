# Smali Bytecode Hooks & Patches

This directory contains the surgical Smali bytecode patches applied to the base APK (`com.androidapks.tekken3`).

## 1. `MainMenuActivity.smali`
- **Location:** `smali/com/retroarch/browser/mainmenu/MainMenuActivity.smali`
- **Modifications:**
  1. **Launcher Redirection (`finalStartup`):**
     Replaces the default direct game launch timer with a static call to `MultiplayerLauncher.showLauncher(this)`.
     ```smali
     .method public finalStartup()V
         .locals 0

         invoke-static {p0}, Lcom/retroarch/browser/mainmenu/MultiplayerLauncher;->showLauncher(Lcom/retroarch/browser/mainmenu/MainMenuActivity;)V

         return-void
     .end method
     ```
  2. **Silencing `ListActivity` Layout Invariant (`onContentChanged`):**
     Because `MainMenuActivity` inherits from Android's `PreferenceActivity` $\rightarrow$ `ListActivity`, overriding content views without an `android.R.id.list` causes a fatal runtime exception (`RuntimeException: Your content must have a ListView whose id attribute is 'android.R.id.list'`). Overriding `onContentChanged()` as a no-op neutralizes this crash.
     ```smali
     .method public onContentChanged()V
         .locals 0

         return-void
     .end method
     ```
  3. **Android 13+ Permission Progression:**
     Ensures that denied legacy external storage permissions do not stall or hang the application on modern Android versions.

## 2. `SplitCompatApplication.smali`
- **Location:** `smali/com/google/android/play/core/splitcompat/SplitCompatApplication.smali`
- **Modifications:**
  In `attachBaseContext`, injects `MultiDex.install(Context)` to ensure that classes located in secondary DEX files (`classes2.dex`, where `MultiplayerLauncher` resides) are properly resolved at runtime across all Android OEM skins.
  ```smali
  invoke-static {p0}, Landroidx/multidex/MultiDex;->install(Landroid/content/Context;)V
  ```
