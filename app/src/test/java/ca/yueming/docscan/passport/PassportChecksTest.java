package ca.yueming.docscan.passport;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class PassportChecksTest {
    private PassportChecks.Input good() {
        PassportChecks.Input in = new PassportChecks.Input();
        in.faceCount = 1;
        in.leftEyeOpen = 0.95f; in.rightEyeOpen = 0.9f; in.smiling = 0.05f;
        in.crop = PassportCropper.crop(new HeadEstimator.Head(1500, 800, 1600), 3000, 4000);
        in.backgroundMean = 235; in.backgroundStd = 6;
        return in;
    }

    private PassportChecks.Level level(List<PassportChecks.Result> r, String name) {
        for (PassportChecks.Result x : r) if (x.name.equals(name)) return x.level;
        throw new AssertionError("missing " + name);
    }

    @Test public void goodPhotoPassesEverything() {
        List<PassportChecks.Result> r = PassportChecks.run(good());
        assertEquals(PassportChecks.Level.PASS, PassportChecks.overall(r));
        assertEquals(9, r.size());
    }

    @Test public void noFaceFailsImmediately() {
        PassportChecks.Input in = good();
        in.faceCount = 0;
        List<PassportChecks.Result> r = PassportChecks.run(in);
        assertEquals(1, r.size());
        assertEquals(PassportChecks.Level.FAIL, PassportChecks.overall(r));
    }

    @Test public void tiltClosedEyesSmileDarkBackgroundWarn() {
        PassportChecks.Input in = good();
        in.eulerZ = 12; in.leftEyeOpen = 0.1f; in.smiling = 0.9f; in.backgroundMean = 90;
        List<PassportChecks.Result> r = PassportChecks.run(in);
        assertEquals(PassportChecks.Level.WARN, level(r, "Head straight"));
        assertEquals(PassportChecks.Level.WARN, level(r, "Eyes open"));
        assertEquals(PassportChecks.Level.WARN, level(r, "Neutral expression"));
        assertEquals(PassportChecks.Level.WARN, level(r, "Plain light background"));
        assertEquals(PassportChecks.Level.WARN, PassportChecks.overall(r));
    }

    @Test public void smallSourceHeadWarnsOnResolutionAndPadding() {
        PassportChecks.Input in = good();
        in.crop = PassportCropper.crop(new HeadEstimator.Head(50, 20, 220), 300, 400); // 200 px head
        List<PassportChecks.Result> r = PassportChecks.run(in);
        assertEquals(PassportChecks.Level.WARN, level(r, "Sharpness (resolution)"));
        assertEquals(PassportChecks.Level.WARN, level(r, "Whole head in photo"));
    }

    @Test public void backgroundStatsOfUniformAndNoisy() {
        int w = 120, h = 160;
        int[] px = new int[w * h];
        Arrays.fill(px, 0xFFF0F0F0);
        double[] s = PassportChecks.backgroundStats(px, w, h);
        assertEquals(240, s[0], 1e-9);
        assertEquals(0, s[1], 1e-9);
        for (int i = 0; i < w * 10; i += 2) px[i] = 0xFF000000; // stripes in top-left patch
        s = PassportChecks.backgroundStats(px, w, h);
        assertTrue(s[1] > PassportChecks.MAX_BG_STD);
    }
}
