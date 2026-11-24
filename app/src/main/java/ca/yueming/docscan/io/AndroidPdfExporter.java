package ca.yueming.docscan.io;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import ca.yueming.docscan.AppLog;
import ca.yueming.docscan.core.filter.PageFilter;
import ca.yueming.docscan.core.pdf.PageSizer;
import ca.yueming.docscan.core.pdf.PdfExporter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/** PdfExporter on android.graphics.pdf.PdfDocument; one page per image, laid out by PageSizer. */
public final class AndroidPdfExporter implements PdfExporter {
    private final PageFilter filter;

    public AndroidPdfExporter(PageFilter filter) { this.filter = filter; }

    @Override
    public int export(List<String> pages, PageSizer.Paper paper, OutputStream out) throws IOException {
        PdfDocument doc = new PdfDocument();
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        try {
            int n = 0;
            for (String path : pages) {
                Bitmap b = Bitmaps.filtered(path, filter);
                PageSizer.Layout l = PageSizer.layout(paper, b.getWidth(), b.getHeight());
                PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(l.pageW, l.pageH, ++n).create());
                Canvas c = page.getCanvas();
                c.drawColor(0xFFFFFFFF);
                c.drawBitmap(b, null, new RectF(l.left, l.top, l.left + l.width, l.top + l.height), paint);
                doc.finishPage(page);
                b.recycle();
            }
            doc.writeTo(out);
            AppLog.i("Pdf", "wrote " + n + " pages, paper=" + paper + ", filter=" + filter);
            return n;
        } finally {
            doc.close();
        }
    }
}
