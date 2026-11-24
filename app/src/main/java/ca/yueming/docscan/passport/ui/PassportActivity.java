package ca.yueming.docscan.passport.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import ca.yueming.docscan.AppLog;
import ca.yueming.docscan.R;
import ca.yueming.docscan.core.pdf.FileNamer;
import ca.yueming.docscan.io.Saver;
import ca.yueming.docscan.passport.HeadEstimator;
import ca.yueming.docscan.passport.PassportChecks;
import ca.yueming.docscan.passport.PassportCropper;
import ca.yueming.docscan.passport.PassportSpec;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Camera with guide overlay -> face detection -> 50x70 mm crop + checks -> save photo, 4x6 sheet JPEG and PDF. */
public class PassportActivity extends AppCompatActivity {
    private static final String TAG = "Passport";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private PreviewView preview;
    private ImageCapture capture;
    private boolean front = true;
    private Bitmap photo;
    private FaceDetector detector;

    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera(); else toast("Camera permission denied; you can still pick a photo.");
            });

    private final ActivityResultLauncher<String> picker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if (uri != null) io.execute(() -> processUri(uri)); });

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_passport);
        preview = findViewById(R.id.preview);
        detector = FaceDetection.getClient(new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build());
        findViewById(R.id.btn_switch).setOnClickListener(v -> { front = !front; startCamera(); });
        findViewById(R.id.btn_capture).setOnClickListener(v -> takePhoto());
        findViewById(R.id.btn_pick).setOnClickListener(v -> picker.launch("image/*"));
        findViewById(R.id.btn_retake).setOnClickListener(v -> showCamera(true));
        findViewById(R.id.btn_save).setOnClickListener(v -> save());
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permission.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onDestroy() {
        io.shutdown();
        detector.close();
        super.onDestroy();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> f = ProcessCameraProvider.getInstance(this);
        f.addListener(() -> {
            try {
                ProcessCameraProvider provider = f.get();
                Preview p = new Preview.Builder().build();
                p.setSurfaceProvider(preview.getSurfaceProvider());
                capture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build();
                CameraSelector sel = front ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
                if (!provider.hasCamera(sel)) {
                    sel = front ? CameraSelector.DEFAULT_BACK_CAMERA : CameraSelector.DEFAULT_FRONT_CAMERA;
                    front = !front;
                }
                provider.unbindAll();
                provider.bindToLifecycle(this, sel, p, capture);
                AppLog.i(TAG, "camera started, front=" + front);
            } catch (Exception e) {
                AppLog.e(TAG, "camera failed", e);
                toast("Camera unavailable: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (capture == null) { toast("Camera not ready"); return; }
        File dir = new File(getCacheDir(), "captures");
        dir.mkdirs();
        File file = new File(dir, "capture.jpg");
        capture.takePicture(new ImageCapture.OutputFileOptions.Builder(file).build(), io,
                new ImageCapture.OnImageSavedCallback() {
                    @Override public void onImageSaved(ImageCapture.OutputFileResults r) {
                        AppLog.i(TAG, "captured " + file.length() + " bytes");
                        processUri(Uri.fromFile(file));
                    }

                    @Override public void onError(ImageCaptureException e) {
                        AppLog.e(TAG, "capture failed", e);
                        main.post(() -> toast("Capture failed: " + e.getMessage()));
                    }
                });
    }

    /** Runs on the io thread. */
    private void processUri(Uri uri) {
        try {
            Bitmap src = decodeUpright(uri);
            List<Face> faces = com.google.android.gms.tasks.Tasks.await(detector.process(InputImage.fromBitmap(src, 0)));
            AppLog.i(TAG, "source " + src.getWidth() + "x" + src.getHeight() + ", faces=" + faces.size());
            PassportChecks.Input in = new PassportChecks.Input();
            in.faceCount = faces.size();
            Bitmap out = null;
            if (faces.size() == 1) {
                Face f = faces.get(0);
                HeadEstimator.Head head = HeadEstimator.estimate(FaceAdapter.toFace(f));
                in.crop = PassportCropper.crop(head, src.getWidth(), src.getHeight());
                in.eulerX = f.getHeadEulerAngleX(); in.eulerY = f.getHeadEulerAngleY(); in.eulerZ = f.getHeadEulerAngleZ();
                in.leftEyeOpen = f.getLeftEyeOpenProbability();
                in.rightEyeOpen = f.getRightEyeOpenProbability();
                in.smiling = f.getSmilingProbability();
                out = PassportRenderer.photo(src, in.crop);
                int[] px = new int[out.getWidth() * out.getHeight()];
                out.getPixels(px, 0, out.getWidth(), 0, 0, out.getWidth(), out.getHeight());
                double[] bg = PassportChecks.backgroundStats(px, out.getWidth(), out.getHeight());
                in.backgroundMean = bg[0]; in.backgroundStd = bg[1];
                AppLog.i(TAG, String.format("head crown=%.0f chin=%.0f scale=%.3f pad=%b",
                        head.crownY, head.chinY, in.crop.scale, in.crop.needsPadding));
            }
            List<PassportChecks.Result> results = PassportChecks.run(in);
            for (PassportChecks.Result r : results) AppLog.i(TAG, "check " + r);
            Bitmap shown = out;
            main.post(() -> showResult(shown, results));
        } catch (Exception e) {
            AppLog.e(TAG, "processing failed", e);
            main.post(() -> toast("Could not process photo: " + e.getMessage()));
        }
    }

    private Bitmap decodeUpright(Uri uri) throws Exception {
        int rotation;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            rotation = new ExifInterface(in).getRotationDegrees();
        }
        Bitmap b;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            b = BitmapFactory.decodeStream(in);
        }
        if (b == null) throw new IllegalStateException("not an image");
        if (rotation == 0) return b;
        Matrix m = new Matrix();
        m.postRotate(rotation);
        return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
    }

    private void showResult(Bitmap out, List<PassportChecks.Result> results) {
        photo = out;
        StringBuilder sb = new StringBuilder("Overall: ").append(PassportChecks.overall(results)).append('\n');
        for (PassportChecks.Result r : results) sb.append(r).append('\n');
        ((TextView) findViewById(R.id.result_checks)).setText(sb);
        ((ImageView) findViewById(R.id.result_image)).setImageBitmap(out);
        findViewById(R.id.btn_save).setEnabled(out != null);
        showCamera(false);
    }

    private void showCamera(boolean camera) {
        findViewById(R.id.camera_group).setVisibility(camera ? View.VISIBLE : View.GONE);
        findViewById(R.id.result_group).setVisibility(camera ? View.GONE : View.VISIBLE);
    }

    private void save() {
        Bitmap p = photo;
        if (p == null) return;
        toast("Saving…");
        io.execute(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                String base = FileNamer.pdf("Passport", now).replace(".pdf", "");
                File single = Saver.exportFile(this, base + "_50x70mm.jpg");
                writeJpeg(p, single);
                Bitmap sheet = PassportRenderer.sheet(p);
                File sheetJpg = Saver.exportFile(this, base + "_4x6_sheet.jpg");
                writeJpeg(sheet, sheetJpg);
                File sheetPdf = Saver.exportFile(this, base + "_4x6_sheet.pdf");
                try (OutputStream os = new FileOutputStream(sheetPdf)) { PassportRenderer.sheetPdf(sheet, os); }
                sheet.recycle();
                Saver.saveToDownloads(this, single, "image/jpeg");
                Saver.saveToDownloads(this, sheetJpg, "image/jpeg");
                String where = Saver.saveToDownloads(this, sheetPdf, "application/pdf");
                List<File> files = new ArrayList<>();
                files.add(single); files.add(sheetJpg); files.add(sheetPdf);
                main.post(() -> new AlertDialog.Builder(this).setTitle("Saved")
                        .setMessage("Saved 50×70 mm photo, 4×6 sheet JPEG and PDF to " + where.replaceAll("/[^/]*$", "/")
                                + "\n\n" + getString(R.string.passport_disclaimer))
                        .setPositiveButton("Share", (d, w) -> Saver.share(this, files, "*/*"))
                        .setNegativeButton("OK", null).show());
            } catch (Exception e) {
                AppLog.e(TAG, "save failed", e);
                main.post(() -> toast("Save failed: " + e.getMessage()));
            }
        });
    }

    /** JPEG quality 95 with 600 DPI resolution tags so it prints at 50x70 mm. */
    private static void writeJpeg(Bitmap b, File f) throws Exception {
        try (OutputStream os = new FileOutputStream(f)) {
            if (!b.compress(Bitmap.CompressFormat.JPEG, 95, os)) throw new IllegalStateException("compress failed");
        }
        ExifInterface exif = new ExifInterface(f.getAbsolutePath());
        exif.setAttribute(ExifInterface.TAG_X_RESOLUTION, PassportSpec.DPI + "/1");
        exif.setAttribute(ExifInterface.TAG_Y_RESOLUTION, PassportSpec.DPI + "/1");
        exif.setAttribute(ExifInterface.TAG_RESOLUTION_UNIT, "2");
        exif.saveAttributes();
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
