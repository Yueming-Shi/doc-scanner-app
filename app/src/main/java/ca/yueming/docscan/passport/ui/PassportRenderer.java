package ca.yueming.docscan.passport.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import ca.yueming.docscan.passport.PassportCropper;
import ca.yueming.docscan.passport.PassportSpec;
import ca.yueming.docscan.passport.SheetLayout;
import java.io.IOException;
import java.io.OutputStream;

/** Draws the 1181x1654 photo from the source crop, the 4x6 two-copy sheet, and the sheet PDF. */
final class PassportRenderer {
    private PassportRenderer() {}

    /** Crop/scale only (no retouching); areas outside the source are white. */
    static Bitmap photo(Bitmap src, PassportCropper.Crop crop) {
        Bitmap out = Bitmap.createBitmap(PassportSpec.OUT_W, PassportSpec.OUT_H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        c.drawColor(0xFFFFFFFF);
        Matrix m = new Matrix();
        m.postTranslate((float) -crop.left, (float) -crop.top);
        m.postScale((float) crop.scale, (float) crop.scale);
        c.drawBitmap(src, m, new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
        return out;
    }

    static Bitmap sheet(Bitmap photo) {
        Bitmap s = Bitmap.createBitmap(SheetLayout.SHEET_W, SheetLayout.SHEET_H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(s);
        c.drawColor(0xFFFFFFFF);
        Paint mark = new Paint();
        mark.setColor(0xFF000000);
        mark.setStrokeWidth(3f);
        int m = SheetLayout.MARK_PX, w = PassportSpec.OUT_W, h = PassportSpec.OUT_H;
        for (int[] p : SheetLayout.positions()) {
            c.drawBitmap(photo, p[0], p[1], null);
            int[][] corners = {{p[0], p[1]}, {p[0] + w, p[1]}, {p[0], p[1] + h}, {p[0] + w, p[1] + h}};
            for (int[] k : corners) {
                int dx = k[0] == p[0] ? -1 : 1, dy = k[1] == p[1] ? -1 : 1;
                c.drawLine(k[0], k[1] + dy * 8, k[0], k[1] + dy * m, mark);
                c.drawLine(k[0] + dx * 8, k[1], k[0] + dx * m, k[1], mark);
            }
        }
        return s;
    }

    /** One 6x4 in PDF page with the sheet image filling it (600 DPI effective). */
    static void sheetPdf(Bitmap sheet, OutputStream out) throws IOException {
        PdfDocument doc = new PdfDocument();
        try {
            PdfDocument.Page page = doc.startPage(
                    new PdfDocument.PageInfo.Builder(SheetLayout.PDF_W_PT, SheetLayout.PDF_H_PT, 1).create());
            page.getCanvas().drawBitmap(sheet, null, new Rect(0, 0, SheetLayout.PDF_W_PT, SheetLayout.PDF_H_PT),
                    new Paint(Paint.FILTER_BITMAP_FLAG));
            doc.finishPage(page);
            doc.writeTo(out);
        } finally {
            doc.close();
        }
    }
}
