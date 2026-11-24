package ca.yueming.docscan.passport.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import ca.yueming.docscan.passport.HeadEstimator;
import ca.yueming.docscan.passport.PassportSpec;

/** Draws the 50x70 frame, head oval (crown-to-chin = target) and eye line over the camera preview. */
public final class GuideOverlayView extends View {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shade = new Paint();

    public GuideOverlayView(Context c, AttributeSet a) {
        super(c, a);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(4f);
        line.setColor(0xFF00E676);
        shade.setColor(0x66000000);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float fw = w * 0.85f, fh = fw * (float) (PassportSpec.HEIGHT_MM / PassportSpec.WIDTH_MM);
        if (fh > h * 0.8f) { fh = h * 0.8f; fw = fh * (float) (PassportSpec.WIDTH_MM / PassportSpec.HEIGHT_MM); }
        float l = (w - fw) / 2, t = (h - fh) / 2;
        c.drawRect(0, 0, w, t, shade);
        c.drawRect(0, t + fh, w, h, shade);
        c.drawRect(0, t, l, t + fh, shade);
        c.drawRect(l + fw, t, w, t + fh, shade);
        c.drawRect(l, t, l + fw, t + fh, line);
        float mm = fh / (float) PassportSpec.HEIGHT_MM;
        float crown = t + (float) PassportSpec.CROWN_TOP_MARGIN_MM * mm;
        float head = (float) PassportSpec.FACE_TARGET_MM * mm;
        float ovalW = head * 0.72f;
        c.drawOval(new RectF(w / 2 - ovalW / 2, crown, w / 2 + ovalW / 2, crown + head), line);
        float eye = crown + head * (float) (HeadEstimator.CROWN_TO_EYE_RATIO / (1 + HeadEstimator.CROWN_TO_EYE_RATIO));
        c.drawLine(w / 2 - ovalW * 0.6f, eye, w / 2 + ovalW * 0.6f, eye, line);
    }
}
