# Doc Scanner + Canadian Passport Photo (Android) — Requirements

Plain-language requirements. Each numbered item maps to one feature commit with unit tests.
Inspiration is only the publicly known feature set of document-scanner apps (CamScanner-style).
No CamScanner code, APK, or assets were decompiled, reverse-engineered, or copied.

## Platform
- Android 7.0+ (minSdk 24), target/compile SDK 35. Java for app code; Gradle Kotlin DSL only for build scripts.
- Needs Google Play services (ML Kit Document Scanner UI is delivered by Play services). Yueming's Samsung S24 has it.

## A. Document scan
1. **Capture**: take a photo with the camera or import from the gallery. (ML Kit Document Scanner provides both.)
2. **Paper edges**: the paper area is found automatically; the user can drag the corners to adjust. (ML Kit.)
3. **Straighten**: the page is perspective-corrected to a flat rectangle. (ML Kit.)
4. **Enhance**: export filter choice of Original, Grayscale, or B&W document (adaptive threshold that keeps text crisp under uneven light). Own pure-Java code (`core.filter`).
5. **Multi-page session**: pages from several scans collect in one session; the user can move pages up/down and delete them. Own pure-Java model (`core.session.ScanSession`).
6. **Export**: one multi-page PDF, or one JPEG/PNG per page. PDF page size: fit-to-image, A4, or Letter, image centred and scaled to fit inside margins without distortion (`core.pdf.PageSizer`). Exporters live behind small interfaces (`PdfExporter`, `ImageExporter`).
7. **Save/share**: save to Downloads (MediaStore on Android 10+, app-specific Downloads folder below 10) and/or open the Android share sheet.

## B. Canadian passport photo
Official spec (verified 2026-11-23 on canada.ca):
- Source: https://www.canada.ca/en/immigration-refugees-citizenship/services/canadian-passports/photos.html
  (also the IRCC photographer sheet https://www.canada.ca/content/dam/ircc/migration/ircc/english/pdf/kits/guides/3998e_tempa.pdf)
- Size **50 mm wide × 70 mm high** (2 in × 2¾ in).
- Face height **31 mm to 36 mm from chin to crown** (natural top of head, not hair).
- Neutral expression: eyes open and clearly visible, looking at the camera, mouth closed, no smile/frown.
- Face and shoulders centred and squared to the camera, not tilted.
- Plain white or light-coloured background; uniform light, no shadows/glare.
- Two identical, unaltered photos, **taken by a commercial photographer or studio**, professionally printed
  on photo paper (**home prints are refused**); studio name/address/date on the back; ≤ 6 months old.

Requirements:
8. **Capture**: live camera preview with front/rear switch and a guide overlay (50×70 frame, head oval, eye line).
9. **Face detection**: ML Kit Face Detection (bundled model) with landmarks, contours, and classification.
10. **Crown/chin estimate** (`passport.HeadEstimator`): chin = lowest point of the face contour (bounding-box bottom fallback).
    Crown = eye line minus `CROWN_TO_EYE_RATIO` (1.0) × eye-to-chin distance — the eyes sit near the middle of the
    head height in adult anthropometry. Fallback without eyes: crown = box top − 0.25 × box height. Documented and unit-tested; it is an estimate.
11. **Auto crop/scale** (`passport.PassportCropper`): scale so chin-to-crown = **34 mm** (target, inside 31–36),
    head centred horizontally, crown 14 mm below the top edge (leaves room for shoulders). Out-of-image areas are filled white and flagged.
12. **Checks** (`passport.PassportChecks`, PASS/WARN/FAIL): exactly one face; face height in mm within 31–36;
    horizontal centring; head tilt (Euler Z roll ≤ 5°, Y yaw ≤ 8°, X pitch ≤ 8°); both eyes open probability ≥ 0.6;
    smiling probability ≤ 0.4; background light and plain (corner samples, mean luma ≥ 180, std ≤ 25); source resolution
    (upscale ≤ 1.5×); padding needed.
13. **Output**: one 50×70 mm photo at 600 DPI = **1181 × 1654 px** JPEG, and a **4×6 in** (2400 × 3600 px at 600 DPI) sheet
    with 2 copies and cut marks, saved as JPEG and as a 4×6 in PDF (`passport.SheetLayout`).
14. **Disclaimer** shown on screen and in the README: this is a convenience/preview tool. IRCC says photos must be taken
    by a commercial photographer and not printed at home, and the photographer's stamp goes on the back; final acceptance is up to the photographer/IRCC.

## Quality
- Small modules; pure-Java logic (geometry/crop math, filters, page sizing, session, sheet layout, checks) has JVM unit tests.
- Logging via `android.util.Log` with tag prefix `DocScan` at each user action and result.
