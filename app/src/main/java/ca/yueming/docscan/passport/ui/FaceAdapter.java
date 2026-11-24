package ca.yueming.docscan.passport.ui;

import android.graphics.PointF;
import android.graphics.Rect;
import ca.yueming.docscan.passport.HeadEstimator;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceLandmark;
import java.util.ArrayList;
import java.util.List;

/** Converts an ML Kit Face into the plain HeadEstimator.Face used by the tested math. */
final class FaceAdapter {
    private FaceAdapter() {}

    static HeadEstimator.Face toFace(Face f) {
        Rect b = f.getBoundingBox();
        List<double[]> contour = null;
        FaceContour fc = f.getContour(FaceContour.FACE);
        if (fc != null && !fc.getPoints().isEmpty()) {
            contour = new ArrayList<>();
            for (PointF p : fc.getPoints()) contour.add(new double[] {p.x, p.y});
        }
        return new HeadEstimator.Face(b.left, b.top, b.right, b.bottom,
                point(f.getLandmark(FaceLandmark.LEFT_EYE)), point(f.getLandmark(FaceLandmark.RIGHT_EYE)), contour);
    }

    private static double[] point(FaceLandmark l) {
        return l == null ? null : new double[] {l.getPosition().x, l.getPosition().y};
    }
}
