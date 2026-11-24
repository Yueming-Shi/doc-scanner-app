package ca.yueming.docscan.io;

import android.graphics.Bitmap;
import ca.yueming.docscan.core.filter.PageFilter;
import ca.yueming.docscan.core.pdf.ImageExporter;
import java.io.IOException;
import java.io.OutputStream;

/** ImageExporter that writes a filtered page as JPEG (quality 92) or PNG. */
public final class AndroidImageExporter implements ImageExporter {
    private final PageFilter filter;

    public AndroidImageExporter(PageFilter filter) { this.filter = filter; }

    @Override
    public void export(String page, Format format, OutputStream out) throws IOException {
        Bitmap b = Bitmaps.filtered(page, filter);
        try {
            boolean ok = b.compress(format == Format.PNG ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 92, out);
            if (!ok) throw new IOException("compress failed");
        } finally {
            b.recycle();
        }
    }
}
