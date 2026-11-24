package ca.yueming.docscan.passport;

import java.util.List;

/**
 * Estimates chin and crown (natural top of head) from face-detector output. Face detectors do not report the crown,
 * so it is extrapolated:
 * <ul>
 *   <li>chin = lowest point of the face contour, else the bounding-box bottom;</li>
 *   <li>crown = eye line - CROWN_TO_EYE_RATIO x (chin - eye line). In adult anthropometry the eyes sit close to the
 *       vertical middle of the head (crown to chin), so the ratio is 1.0;</li>
 *   <li>without eyes: crown = box top - BOX_CROWN_EXTRA x box height (ML Kit boxes stop around the brow/forehead).</li>
 * </ul>
 * This is an estimate; the on-screen guide lets the user judge it.
 */
public final class HeadEstimator {
    public static final double CROWN_TO_EYE_RATIO = 1.0;
    public static final double BOX_CROWN_EXTRA = 0.25;

    /** Detector output in source-image pixels. Eyes/contour may be null. */
    public static final class Face {
        final double left, top, right, bottom;
        final double[] leftEye, rightEye; // {x, y}
        final List<double[]> contour;

        public Face(double left, double top, double right, double bottom,
                    double[] leftEye, double[] rightEye, List<double[]> contour) {
            this.left = left; this.top = top; this.right = right; this.bottom = bottom;
            this.leftEye = leftEye; this.rightEye = rightEye; this.contour = contour;
        }
    }

    /** Estimated head line positions in source pixels. */
    public static final class Head {
        public final double centerX, crownY, chinY;

        public Head(double centerX, double crownY, double chinY) {
            this.centerX = centerX; this.crownY = crownY; this.chinY = chinY;
        }

        public double height() { return chinY - crownY; }
    }

    private HeadEstimator() {}

    public static Head estimate(Face f) {
        double chin = f.bottom;
        if (f.contour != null && !f.contour.isEmpty()) {
            chin = Double.NEGATIVE_INFINITY;
            for (double[] p : f.contour) chin = Math.max(chin, p[1]);
        }
        boolean eyes = f.leftEye != null && f.rightEye != null;
        double centerX = eyes ? (f.leftEye[0] + f.rightEye[0]) / 2 : (f.left + f.right) / 2;
        double crown;
        if (eyes) {
            double eyeY = (f.leftEye[1] + f.rightEye[1]) / 2;
            crown = eyeY - CROWN_TO_EYE_RATIO * (chin - eyeY);
        } else {
            crown = f.top - BOX_CROWN_EXTRA * (f.bottom - f.top);
        }
        if (chin <= crown) throw new IllegalArgumentException("chin above crown");
        return new Head(centerX, crown, chin);
    }
}
