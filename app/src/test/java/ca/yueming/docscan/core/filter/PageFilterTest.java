package ca.yueming.docscan.core.filter;

import static org.junit.Assert.*;

import org.junit.Test;

public class PageFilterTest {
    private static int rgb(int r, int g, int b) { return 0xFF000000 | (r << 16) | (g << 8) | b; }

    @Test public void lumaOfPrimaries() {
        assertEquals(255, PageFilter.luma(rgb(255, 255, 255)));
        assertEquals(0, PageFilter.luma(rgb(0, 0, 0)));
        assertEquals(76, PageFilter.luma(rgb(255, 0, 0)));
        assertEquals(150, PageFilter.luma(rgb(0, 255, 0)));
    }

    @Test public void originalIsCopy() {
        int[] px = {rgb(1, 2, 3), rgb(4, 5, 6)};
        int[] out = PageFilter.ORIGINAL.apply(px, 2, 1);
        assertArrayEquals(px, out);
        assertNotSame(px, out);
    }

    @Test public void grayscaleHasEqualChannelsAndKeepsAlpha() {
        int px = 0x80FF0000;
        int out = PageFilter.GRAYSCALE.apply(new int[] {px}, 1, 1)[0];
        assertEquals(0x80, out >>> 24);
        assertEquals((out >> 16) & 0xFF, out & 0xFF);
        assertEquals(76, out & 0xFF);
    }

    /** Dark text on a page whose right half is in shadow must still come out as black text on white. */
    @Test public void bwKeepsTextUnderShadow() {
        int w = 64, h = 64;
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int paper = x < 32 ? 230 : 120; // shadowed right half, darker than any global threshold of ~128
            boolean ink = (y == 20 || y == 40) && x % 4 != 0;
            int v = ink ? paper / 3 : paper;
            px[y * w + x] = rgb(v, v, v);
        }
        int[] out = PageFilter.BW_DOCUMENT.apply(px, w, h);
        assertEquals(0xFFFFFFFF, out[10 * w + 50]); // shadowed paper -> white
        assertEquals(0xFF000000, out[20 * w + 50]); // shadowed ink -> black
        assertEquals(0xFFFFFFFF, out[10 * w + 10]); // lit paper -> white
        assertEquals(0xFF000000, out[40 * w + 10]); // lit ink -> black
    }

    @Test public void uniformPageIsAllWhite() {
        int[] px = new int[16 * 16];
        java.util.Arrays.fill(px, rgb(200, 200, 200));
        for (int v : PageFilter.BW_DOCUMENT.apply(px, 16, 16)) assertEquals(0xFFFFFFFF, v);
    }

    @Test(expected = IllegalArgumentException.class) public void sizeMismatchThrows() {
        PageFilter.GRAYSCALE.apply(new int[3], 2, 2);
    }
}
