package ca.yueming.docscan.core.filter;

/**
 * Enhancement filters on ARGB_8888 pixel arrays (as from Bitmap.getPixels), pure Java so they are JVM-testable.
 */
public enum PageFilter {
    ORIGINAL, GRAYSCALE, BW_DOCUMENT;

    /** Returns a new pixel array; the input is not modified. */
    public int[] apply(int[] argb, int width, int height) {
        if (argb.length != width * height) throw new IllegalArgumentException("pixel count mismatch");
        switch (this) {
            case GRAYSCALE: return grayscale(argb);
            case BW_DOCUMENT: return adaptiveThreshold(argb, width, height);
            default: return argb.clone();
        }
    }

    /** ITU-R BT.601 luma, 0..255. */
    public static int luma(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114 + 500) / 1000;
    }

    static int gray(int alpha, int y) { return (alpha << 24) | (y << 16) | (y << 8) | y; }

    static int[] grayscale(int[] src) {
        int[] out = new int[src.length];
        for (int i = 0; i < src.length; i++) out[i] = gray(src[i] >>> 24, luma(src[i]));
        return out;
    }

    /** Window is about 1/8 of the shorter side; a pixel is black if it is this much darker than its local mean. */
    static final int DARKER_PERCENT = 12;

    /**
     * Bradley-style adaptive threshold using an integral image: robust to shadows and uneven light,
     * which a single global threshold is not.
     */
    static int[] adaptiveThreshold(int[] src, int w, int h) {
        long[] integral = new long[(w + 1) * (h + 1)];
        for (int y = 0; y < h; y++) {
            long row = 0;
            for (int x = 0; x < w; x++) {
                row += luma(src[y * w + x]);
                integral[(y + 1) * (w + 1) + x + 1] = integral[y * (w + 1) + x + 1] + row;
            }
        }
        int half = Math.max(1, Math.min(w, h) / 16);
        int[] out = new int[src.length];
        for (int y = 0; y < h; y++) {
            int y0 = Math.max(0, y - half), y1 = Math.min(h - 1, y + half);
            for (int x = 0; x < w; x++) {
                int x0 = Math.max(0, x - half), x1 = Math.min(w - 1, x + half);
                long count = (long) (x1 - x0 + 1) * (y1 - y0 + 1);
                long sum = integral[(y1 + 1) * (w + 1) + x1 + 1] - integral[y0 * (w + 1) + x1 + 1]
                        - integral[(y1 + 1) * (w + 1) + x0] + integral[y0 * (w + 1) + x0];
                int v = luma(src[y * w + x]);
                boolean black = (long) v * count * 100 < sum * (100 - DARKER_PERCENT);
                out[y * w + x] = gray(0xFF, black ? 0 : 255);
            }
        }
        return out;
    }
}
