# Doc Scanner + Canadian Passport Photo

Java Android app (minSdk 26, target 35). See REQUIREMENTS.md for the plain-language spec and the IRCC source.

## Build
```
export JAVA_HOME=/home/box/.local/share/java/jdk-21.0.13+9 ANDROID_HOME=/home/box/Android/Sdk
./gradlew test assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk` (copied to `apk/`).

## Layout
- `core/session` multi-page session (reorder/delete) · `core/filter` Original/Grayscale/B&W adaptive threshold
- `core/pdf` PDF page sizing, exporter interfaces, file names
- `passport` spec constants, crown/chin estimate, crop math, checks, 4x6 sheet layout (all pure Java, unit-tested)
- `scan`, `passport/ui`, `io` Android screens, ML Kit glue, PdfDocument/JPEG/PNG exporters, MediaStore save + share
- Logs: `adb logcat | grep DocScan`

## Libraries
ML Kit Document Scanner (Play services) 16.0.0 · ML Kit Face Detection (bundled) 16.1.7 · CameraX 1.4.1 ·
AndroidX AppCompat 1.7.0, RecyclerView 1.3.2, ExifInterface 1.3.7 · JUnit 4.13.2.

## Disclaimer
The passport mode is a preview/convenience tool. IRCC says photos must be taken by a commercial photographer or
studio, professionally printed (home prints are refused), not altered, with the studio's name, address and date on
the back. Final acceptance is up to the photographer and IRCC. Chin-to-crown is estimated from face landmarks.

No CamScanner code or assets were used; only its publicly known feature set inspired the scope.
