# Kavesh-MC (Native Android App)

A native Kotlin + Jetpack Compose Android application for hosting PumpkinMC / Minecraft servers directly on Android without Anvil-MC's `429` tunnel rate limits.

## Features
- **1:1 Mobile UI:** Animated typewriter splash screen (`kavesh mc`), obsidian dark theme, floating bottom dock, Home dashboard, Live Console, File Manager with built-in TOML/properties editor, Create Server flow, Gameplay & Performance settings, Live Modrinth API browser, and CPU Benchmark.
- **Real Native Execution:** Runs `libpumpkin.so` inside an Android `ForegroundService` with `PARTIAL_WAKE_LOCK` so ColorOS/Android does not kill the server in the background.
- **Instant Live IP Display:** Automatically reads your phone's real local Wi-Fi IP (`192.168.x.x:25565`) and parses public tunnel addresses (`playit.gg` / `bore.pub`) onto both the Home server card and Live Console with one-tap copy.

## How to Build the APK (`kavesh-mc.apk`)

### Step 1: Add the ARM64 PumpkinMC Binary
1. Download the latest ARM64 `pumpkin` binary.
2. Rename it to **`libpumpkin.so`** and put it in:
   `app/src/main/jniLibs/arm64-v8a/libpumpkin.so`
3. *(Optional for public play outside Wi-Fi)* Put `playit` or `bore` ARM64 binary in the same folder as **`libplayit.so`** or **`libbore.so`**.

### Step 2: Compile the APK
- **Option A — Cloud Build (Fastest, zero laptop lag):**
  1. Upload this entire folder to a new GitHub repository.
  2. Go to the **Actions** tab → **Build Kavesh-MC APK** → click **Run workflow**.
  3. Download **`kavesh-mc-debug-apk`** from the Artifacts section and install it on your phone.
- **Option B — Android Studio:**
  Open this folder in Android Studio and click **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
