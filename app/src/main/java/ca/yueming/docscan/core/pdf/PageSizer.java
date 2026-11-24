package ca.yueming.docscan.core.pdf;

/** Works out PDF page size (in points, 1/72 in) and where an image goes on it. */
public final class PageSizer {
    public enum Paper { FIT_IMAGE, A4, LETTER }

    public static final int A4_W = 595, A4_H = 842;
    public static final int LETTER_W = 612, LETTER_H = 792;
    /** Margin on A4/Letter pages: 0.5 in. */
    public static final float MARGIN_PT = 36f;
    /** Fit-to-image pages assume the scan was this many pixels per inch. */
    public static final float FIT_ASSUMED_DPI = 200f;

    /** Page box plus image placement, all in PDF points. */
    public static final class Layout {
        public final int pageW, pageH;
        public final float left, top, width, height;

        Layout(int pageW, int pageH, float left, float top, float width, float height) {
            this.pageW = pageW; this.pageH = pageH;
            this.left = left; this.top = top; this.width = width; this.height = height;
        }
    }

    private PageSizer() {}

    public static Layout layout(Paper paper, int imgW, int imgH) {
        if (imgW <= 0 || imgH <= 0) throw new IllegalArgumentException("bad image size");
        if (paper == Paper.FIT_IMAGE) {
            int w = Math.max(1, Math.round(imgW * 72f / FIT_ASSUMED_DPI));
            int h = Math.max(1, Math.round(imgH * 72f / FIT_ASSUMED_DPI));
            return new Layout(w, h, 0, 0, w, h);
        }
        int pw = paper == Paper.A4 ? A4_W : LETTER_W;
        int ph = paper == Paper.A4 ? A4_H : LETTER_H;
        if (imgW > imgH) { int t = pw; pw = ph; ph = t; } // landscape scan -> landscape page
        float boxW = pw - 2 * MARGIN_PT, boxH = ph - 2 * MARGIN_PT;
        float scale = Math.min(boxW / imgW, boxH / imgH);
        float w = imgW * scale, h = imgH * scale;
        return new Layout(pw, ph, (pw - w) / 2f, (ph - h) / 2f, w, h);
    }
}
