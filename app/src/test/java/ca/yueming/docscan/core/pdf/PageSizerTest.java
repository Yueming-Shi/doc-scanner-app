package ca.yueming.docscan.core.pdf;

import static org.junit.Assert.*;

import org.junit.Test;

public class PageSizerTest {
    private static final float E = 0.01f;

    @Test public void a4PortraitFitsInsideMarginsKeepingAspect() {
        PageSizer.Layout l = PageSizer.layout(PageSizer.Paper.A4, 1000, 2000);
        assertEquals(595, l.pageW);
        assertEquals(842, l.pageH);
        assertEquals(842 - 72, l.height, E);       // height-limited
        assertEquals(l.height / 2, l.width, E);    // aspect preserved
        assertEquals((595 - l.width) / 2, l.left, E);
        assertEquals(36, l.top, E);
    }

    @Test public void letterWideImageIsWidthLimited() {
        PageSizer.Layout l = PageSizer.layout(PageSizer.Paper.LETTER, 1000, 100);
        assertEquals(792, l.pageW); // landscape
        assertEquals(612, l.pageH);
        assertEquals(792 - 72, l.width, E);
        assertEquals(72, l.height, E);
        assertEquals((612 - 72) / 2f, l.top, E);
    }

    @Test public void squareImageOnA4() {
        PageSizer.Layout l = PageSizer.layout(PageSizer.Paper.A4, 500, 500);
        assertEquals(595 - 72, l.width, E);
        assertEquals(l.width, l.height, E);
    }

    @Test public void fitImageUsesAssumedDpi() {
        PageSizer.Layout l = PageSizer.layout(PageSizer.Paper.FIT_IMAGE, 1700, 2200); // 8.5x11 in at 200 dpi
        assertEquals(612, l.pageW);
        assertEquals(792, l.pageH);
        assertEquals(0, l.left, E);
        assertEquals(612, l.width, E);
    }

    @Test(expected = IllegalArgumentException.class) public void zeroSizeRejected() {
        PageSizer.layout(PageSizer.Paper.A4, 0, 10);
    }
}
