package ca.yueming.docscan.passport;

import static org.junit.Assert.*;

import org.junit.Test;

public class PassportSpecTest {
    @Test public void outputIs600DpiOf50x70mm() {
        assertEquals(1181, PassportSpec.OUT_W);
        assertEquals(1654, PassportSpec.OUT_H);
    }

    @Test public void targetInsideOfficialRange() {
        assertTrue(PassportSpec.FACE_TARGET_MM >= PassportSpec.FACE_MIN_MM);
        assertTrue(PassportSpec.FACE_TARGET_MM <= PassportSpec.FACE_MAX_MM);
        // head plus top margin must leave room below the chin
        assertTrue(PassportSpec.CROWN_TOP_MARGIN_MM + PassportSpec.FACE_TARGET_MM < PassportSpec.HEIGHT_MM - 15);
    }

    @Test public void mmPxRoundTrip() { assertEquals(25.4, PassportSpec.pxToMm(600), 1e-9); }
}
