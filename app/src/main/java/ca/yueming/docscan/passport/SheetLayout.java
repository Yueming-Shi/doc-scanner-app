package ca.yueming.docscan.passport;

/**
 * A 6x4 in (landscape 4x6) print sheet at 600 DPI = 3600 x 2400 px holding two 50x70 mm copies side by side,
 * centred, with a gap for cutting. PDF page is 432 x 288 pt.
 */
public final class SheetLayout {
    public static final int SHEET_W = 6 * PassportSpec.DPI, SHEET_H = 4 * PassportSpec.DPI;
    public static final int PDF_W_PT = 6 * 72, PDF_H_PT = 4 * 72;
    public static final int GAP_PX = PassportSpec.mmToPx(6);
    public static final int COPIES = 2;
    /** Length of the corner cut marks. */
    public static final int MARK_PX = PassportSpec.mmToPx(3);

    private SheetLayout() {}

    /** Top-left corners {x, y} of each copy, in sheet pixels. */
    public static int[][] positions() {
        int total = COPIES * PassportSpec.OUT_W + (COPIES - 1) * GAP_PX;
        int x0 = (SHEET_W - total) / 2;
        int y0 = (SHEET_H - PassportSpec.OUT_H) / 2;
        int[][] p = new int[COPIES][];
        for (int i = 0; i < COPIES; i++) p[i] = new int[] {x0 + i * (PassportSpec.OUT_W + GAP_PX), y0};
        return p;
    }
}
