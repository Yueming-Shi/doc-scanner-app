package ca.yueming.docscan.passport;

/**
 * Canadian passport photo spec (IRCC, verified 2026-11-23):
 * https://www.canada.ca/en/immigration-refugees-citizenship/services/canadian-passports/photos.html
 * 50 mm x 70 mm; chin-to-crown 31-36 mm; plain white/light background; neutral expression; centred, not tilted.
 */
public final class PassportSpec {
    public static final double WIDTH_MM = 50, HEIGHT_MM = 70;
    public static final double FACE_MIN_MM = 31, FACE_MAX_MM = 36;
    /** Our target inside the allowed range. */
    public static final double FACE_TARGET_MM = 34;
    /** Our choice: crown sits this far below the top edge, leaving room for shoulders below the chin. */
    public static final double CROWN_TOP_MARGIN_MM = 14;
    public static final int DPI = 600;
    public static final int OUT_W = mmToPx(WIDTH_MM);   // 1181
    public static final int OUT_H = mmToPx(HEIGHT_MM);  // 1654

    private PassportSpec() {}

    public static int mmToPx(double mm) { return (int) Math.round(mm / 25.4 * DPI); }

    public static double pxToMm(double px) { return px * 25.4 / DPI; }
}
