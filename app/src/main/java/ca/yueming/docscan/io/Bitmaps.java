package ca.yueming.docscan.io;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import ca.yueming.docscan.core.filter.PageFilter;

/** Bitmap loading (downsampled) and applying a PageFilter. */
public final class Bitmaps {
    /** Long side cap for exported pages: about 200 DPI on Letter/A4, keeps filter memory bounded. */
    public static final int MAX_SIDE = 2400;

    private Bitmaps() {}

    public static Bitmap load(String path, int maxSide) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        int sample = 1;
        while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= maxSide) sample *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        o.inMutable = true;
        Bitmap b = BitmapFactory.decodeFile(path, o);
        if (b == null) throw new IllegalStateException("cannot decode " + path);
        int side = Math.max(b.getWidth(), b.getHeight());
        if (side > maxSide) {
            float s = (float) maxSide / side;
            Bitmap scaled = Bitmap.createScaledBitmap(b, Math.round(b.getWidth() * s), Math.round(b.getHeight() * s), true);
            b.recycle();
            b = scaled.isMutable() ? scaled : scaled.copy(Bitmap.Config.ARGB_8888, true);
        }
        return b;
    }

    public static Bitmap filtered(String path, PageFilter filter) {
        Bitmap b = load(path, MAX_SIDE);
        if (filter == PageFilter.ORIGINAL) return b;
        int w = b.getWidth(), h = b.getHeight();
        int[] px = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        px = filter.apply(px, w, h);
        b.setPixels(px, 0, w, 0, 0, w, h);
        return b;
    }
}
