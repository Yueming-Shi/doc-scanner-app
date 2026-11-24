package ca.yueming.docscan.passport;

import static org.junit.Assert.*;

import org.junit.Test;

public class PassportCropperTest {
    @Test public void headScaledTo34mmAndCentred() {
        HeadEstimator.Head head = new HeadEstimator.Head(1500, 800, 1600); // 800 px tall head in a 3000x4000 photo
        PassportCropper.Crop c = PassportCropper.crop(head, 3000, 4000);
        assertEquals(34.0, c.faceHeightMm, 1e-6);
        assertEquals(0.0, c.centerOffsetMm, 1e-6);
        double pxPerMm = 1654 / 70.0;
        assertEquals(34 * pxPerMm / 800, c.scale, 1e-9);
        assertEquals(1181 / c.scale, c.width, 1e-6);
        assertEquals(1654 / c.scale, c.height, 1e-6);
        // aspect of crop = 50:70
        assertEquals(1181.0 / 1654.0, c.width / c.height, 1e-9);
        // crown lands 14 mm below the top after scaling
        assertEquals(14.0, (head.crownY - c.top) * c.scale / pxPerMm, 1e-6);
        assertFalse(c.needsPadding);
    }

    @Test public void headNearEdgeNeedsPadding() {
        PassportCropper.Crop c = PassportCropper.crop(new HeadEstimator.Head(100, 50, 450), 1000, 1000);
        assertTrue(c.needsPadding);
        assertTrue(c.left < 0);
    }
}
