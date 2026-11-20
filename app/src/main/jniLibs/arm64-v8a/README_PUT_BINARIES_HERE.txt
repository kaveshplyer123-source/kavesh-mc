HOW TO BUNDLE REAL ARM64 SERVER & TUNNEL BINARIES IN KAVESH-MC:

Android blocks executing binaries from /data/data/ directly (noexec), EXCEPT inside
the app's nativeLibraryDir (/data/app/.../lib/arm64/). Any ELF binary named lib*.so
placed in this folder is automatically extracted with +x executable permissions!

1. PumpkinMC (Required for native Rust Minecraft server):
   - Download the latest ARM64 Linux/Android binary of PumpkinMC.
   - Rename it to: libpumpkin.so
   - Place it in this folder: app/src/main/jniLibs/arm64-v8a/libpumpkin.so

2. Optional Public Tunnel (for playing outside Wi-Fi with zero 429 errors):
   - Download playit-linux-aarch64 from playit.gg -> rename to: libplayit.so
   - OR download bore (aarch64) -> rename to: libbore.so
   - Place them in this same folder.

Kavesh-MC automatically detects these binaries at runtime, launches them inside a
ForegroundService with WakeLock, and displays both your real Local Wi-Fi IP and
your Public Tunnel IP right on the server card and console!
