package ca.yueming.docscan.passport;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

public class HeadEstimatorTest {
    private static final double E = 1e-9;

    @Test public void eyesAndContourUseRatio() {
        HeadEstimator.Face f = new HeadEstimator.Face(100, 150, 300, 400,
                new double[] {160, 250}, new double[] {240, 250},
                Arrays.asList(new double[] {200, 160}, new double[] {200, 410}, new double[] {120, 300}));
        HeadEstimator.Head h = HeadEstimator.estimate(f);
        assertEquals(410, h.chinY, E);              // lowest contour point wins over box bottom
        assertEquals(250 - 160 * HeadEstimator.CROWN_TO_EYE_RATIO, h.crownY, E);
        assertEquals(200, h.centerX, E);            // eye midpoint
        assertEquals(320, h.height(), E);
    }

    @Test public void eyesWithoutContourUseBoxBottom() {
        HeadEstimator.Face f = new HeadEstimator.Face(0, 100, 200, 300,
                new double[] {60, 180}, new double[] {140, 184}, null);
        HeadEstimator.Head h = HeadEstimator.estimate(f);
        assertEquals(300, h.chinY, E);
        assertEquals(182 - 118, h.crownY, E);
        assertEquals(100, h.centerX, E);
    }

    @Test public void boxOnlyFallback() {
        HeadEstimator.Face f = new HeadEstimator.Face(10, 100, 210, 300, null, null, null);
        HeadEstimator.Head h = HeadEstimator.estimate(f);
        assertEquals(50, h.crownY, E);
        assertEquals(300, h.chinY, E);
        assertEquals(110, h.centerX, E);
    }

    @Test(expected = IllegalArgumentException.class) public void degenerateRejected() {
        HeadEstimator.estimate(new HeadEstimator.Face(0, 0, 10, 10,
                new double[] {0, 50}, new double[] {10, 50}, Arrays.asList(new double[] {5, 40})));
    }
}
