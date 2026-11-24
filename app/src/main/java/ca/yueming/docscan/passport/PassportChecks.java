package ca.yueming.docscan.passport;

import ca.yueming.docscan.core.filter.PageFilter;
import java.util.ArrayList;
import java.util.List;

/** Simple pass/warn/fail checks against the IRCC rules we can approximate on the phone. */
public final class PassportChecks {
    public enum Level { PASS, WARN, FAIL }

    public static final class Result {
        public final String name;
        public final Level level;
        public final String detail;

        Result(String name, Level level, String detail) { this.name = name; this.level = level; this.detail = detail; }

        @Override public String toString() { return level + " " + name + ": " + detail; }
    }

    /** Inputs gathered from detection and the crop. Probabilities may be null when the detector gave none. */
    public static final class Input {
        public int faceCount;
        public float eulerX, eulerY, eulerZ;
        public Float leftEyeOpen, rightEyeOpen, smiling;
        public PassportCropper.Crop crop;
        public double backgroundMean, backgroundStd;
    }

    public static final double MAX_ROLL = 5, MAX_YAW = 8, MAX_PITCH = 8;
    public static final double MIN_EYE_OPEN = 0.6, MAX_SMILE = 0.4;
    public static final double MAX_CENTER_OFFSET_MM = 2;
    public static final double MIN_BG_MEAN = 180, MAX_BG_STD = 25;
    public static final double MAX_UPSCALE = 1.5;

    private PassportChecks() {}

    public static List<Result> run(Input in) {
        List<Result> r = new ArrayList<>();
        if (in.faceCount != 1) {
            r.add(new Result("One face", Level.FAIL, in.faceCount == 0 ? "no face found" : in.faceCount + " faces found"));
            return r;
        }
        r.add(new Result("One face", Level.PASS, "1 face"));
        PassportCropper.Crop c = in.crop;
        boolean sizeOk = c.faceHeightMm >= PassportSpec.FACE_MIN_MM && c.faceHeightMm <= PassportSpec.FACE_MAX_MM;
        r.add(new Result("Face height", sizeOk ? Level.PASS : Level.FAIL,
                String.format("%.1f mm chin-to-crown (estimated; allowed 31-36)", c.faceHeightMm)));
        r.add(new Result("Centred", Math.abs(c.centerOffsetMm) <= MAX_CENTER_OFFSET_MM ? Level.PASS : Level.WARN,
                String.format("%.1f mm off centre", c.centerOffsetMm)));
        boolean tiltOk = Math.abs(in.eulerZ) <= MAX_ROLL && Math.abs(in.eulerY) <= MAX_YAW && Math.abs(in.eulerX) <= MAX_PITCH;
        r.add(new Result("Head straight", tiltOk ? Level.PASS : Level.WARN,
                String.format("roll %.0f°, turn %.0f°, nod %.0f°", in.eulerZ, in.eulerY, in.eulerX)));
        if (in.leftEyeOpen == null || in.rightEyeOpen == null) {
            r.add(new Result("Eyes open", Level.WARN, "could not tell"));
        } else {
            boolean open = in.leftEyeOpen >= MIN_EYE_OPEN && in.rightEyeOpen >= MIN_EYE_OPEN;
            r.add(new Result("Eyes open", open ? Level.PASS : Level.WARN,
                    String.format("%.0f%% / %.0f%%", in.leftEyeOpen * 100, in.rightEyeOpen * 100)));
        }
        if (in.smiling == null) {
            r.add(new Result("Neutral expression", Level.WARN, "could not tell"));
        } else {
            r.add(new Result("Neutral expression", in.smiling <= MAX_SMILE ? Level.PASS : Level.WARN,
                    String.format("smile %.0f%%", in.smiling * 100)));
        }
        boolean bgOk = in.backgroundMean >= MIN_BG_MEAN && in.backgroundStd <= MAX_BG_STD;
        r.add(new Result("Plain light background", bgOk ? Level.PASS : Level.WARN,
                String.format("brightness %.0f/255, variation %.0f", in.backgroundMean, in.backgroundStd)));
        r.add(new Result("Sharpness (resolution)", c.scale <= MAX_UPSCALE ? Level.PASS : Level.WARN,
                String.format("upscaled %.2fx", c.scale)));
        r.add(new Result("Whole head in photo", c.needsPadding ? Level.WARN : Level.PASS,
                c.needsPadding ? "edges filled with white; step back or recentre" : "ok"));
        return r;
    }

    public static Level overall(List<Result> results) {
        Level worst = Level.PASS;
        for (Result x : results) if (x.level.ordinal() > worst.ordinal()) worst = x.level;
        return worst;
    }

    /**
     * Background brightness/variation from two square patches in the top corners of the real photo area
     * (beside the head, above the shoulders), ignoring white padding added outside the source image.
     * {@code valid} = {left, top, right, bottom} of the source image inside the output, in output pixels.
     * Returns {mean, std} of luma.
     */
    public static double[] backgroundStats(int[] argb, int w, int h, int[] valid) {
        int l = Math.max(0, valid[0]), t = Math.max(0, valid[1]);
        int r = Math.min(w, valid[2]), b = Math.min(h, valid[3]);
        if (r <= l || b <= t) throw new IllegalArgumentException("empty photo area");
        int s = Math.max(1, Math.min(Math.min(r - l, b - t), w / 12));
        double sum = 0, sq = 0;
        int n = 0;
        int[] xs = {l, r - s};
        for (int x0 : xs) for (int y = t; y < t + s; y++) for (int x = x0; x < x0 + s; x++) {
            int v = PageFilter.luma(argb[y * w + x]);
            sum += v; sq += (double) v * v; n++;
        }
        double mean = sum / n;
        return new double[] {mean, Math.sqrt(Math.max(0, sq / n - mean * mean))};
    }

    /** Where the source image lands inside the output, {left, top, right, bottom} in output pixels. */
    public static int[] validArea(PassportCropper.Crop c, int srcW, int srcH) {
        return new int[] {
            (int) Math.ceil(-c.left * c.scale), (int) Math.ceil(-c.top * c.scale),
            (int) Math.floor((srcW - c.left) * c.scale), (int) Math.floor((srcH - c.top) * c.scale)};
    }
}
