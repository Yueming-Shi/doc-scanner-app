package ca.yueming.docscan.passport;

import static org.junit.Assert.*;

import org.junit.Test;

public class SheetLayoutTest {
    @Test public void sheetIs6x4InchesAt600Dpi() {
        assertEquals(3600, SheetLayout.SHEET_W);
        assertEquals(2400, SheetLayout.SHEET_H);
        assertEquals(432, SheetLayout.PDF_W_PT);
        assertEquals(288, SheetLayout.PDF_H_PT);
    }

    @Test public void twoCopiesFitWithMarginsAndDoNotOverlap() {
        int[][] p = SheetLayout.positions();
        assertEquals(2, p.length);
        int m = SheetLayout.MARK_PX;
        for (int[] xy : p) {
            assertTrue(xy[0] - m >= 0 && xy[1] - m >= 0);
            assertTrue(xy[0] + PassportSpec.OUT_W + m <= SheetLayout.SHEET_W);
            assertTrue(xy[1] + PassportSpec.OUT_H + m <= SheetLayout.SHEET_H);
        }
        assertEquals(SheetLayout.GAP_PX, p[1][0] - (p[0][0] + PassportSpec.OUT_W));
        // centred horizontally
        assertEquals(p[0][0], SheetLayout.SHEET_W - (p[1][0] + PassportSpec.OUT_W), 1);
    }
}
