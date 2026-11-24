package ca.yueming.docscan.passport;

/**
 * Computes the source-image crop that becomes the 50x70 mm output: head scaled to FACE_TARGET_MM,
 * centred horizontally, crown CROWN_TOP_MARGIN_MM below the top edge.
 */
public final class PassportCropper {
    /** Crop rectangle in source pixels (may extend outside the image) and output px per source px. */
    public static final class Crop {
        public final double left, top, width, height, scale;
        public final boolean needsPadding;
        public final double faceHeightMm, centerOffsetMm;

        Crop(double left, double top, double width, double height, double scale, boolean needsPadding,
             double faceHeightMm, double centerOffsetMm) {
            this.left = left; this.top = top; this.width = width; this.height = height; this.scale = scale;
            this.needsPadding = needsPadding; this.faceHeightMm = faceHeightMm; this.centerOffsetMm = centerOffsetMm;
        }
    }

    private PassportCropper() {}

    public static Crop crop(HeadEstimator.Head head, int srcW, int srcH) {
        double pxPerMm = PassportSpec.OUT_H / PassportSpec.HEIGHT_MM;
        double scale = PassportSpec.FACE_TARGET_MM * pxPerMm / head.height();
        double w = PassportSpec.OUT_W / scale, h = PassportSpec.OUT_H / scale;
        double left = head.centerX - w / 2;
        double top = head.crownY - PassportSpec.CROWN_TOP_MARGIN_MM * pxPerMm / scale;
        boolean pad = left < 0 || top < 0 || left + w > srcW || top + h > srcH;
        double faceMm = head.height() * scale / pxPerMm;
        double offMm = ((head.centerX - left) * scale - PassportSpec.OUT_W / 2.0) / pxPerMm;
        return new Crop(left, top, w, h, scale, pad, faceMm, offMm);
    }
}
