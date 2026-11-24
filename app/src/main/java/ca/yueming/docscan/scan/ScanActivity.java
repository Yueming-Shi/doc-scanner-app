package ca.yueming.docscan.scan;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ca.yueming.docscan.AppLog;
import ca.yueming.docscan.R;
import ca.yueming.docscan.core.filter.PageFilter;
import ca.yueming.docscan.core.pdf.FileNamer;
import ca.yueming.docscan.core.pdf.ImageExporter;
import ca.yueming.docscan.core.pdf.PageSizer;
import ca.yueming.docscan.core.session.ScanSession;
import ca.yueming.docscan.io.AndroidImageExporter;
import ca.yueming.docscan.io.AndroidPdfExporter;
import ca.yueming.docscan.io.Saver;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Multi-page document session. ML Kit Document Scanner does capture/import, edge detection with draggable corners,
 * and perspective correction; this screen keeps the pages, reorders/deletes them, and exports via our own code.
 */
public class ScanActivity extends AppCompatActivity {
    private static final String TAG = "Scan";
    private static final String KEY_PAGES = "pages";

    private final ScanSession session = new ScanSession();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private PageAdapter adapter;
    private Spinner filterSpinner, paperSpinner;
    private View empty;

    private final ActivityResultLauncher<IntentSenderRequest> scanLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(), r -> {
                if (r.getResultCode() != Activity.RESULT_OK || r.getData() == null) {
                    AppLog.i(TAG, "scanner cancelled, code=" + r.getResultCode());
                    return;
                }
                GmsDocumentScanningResult res = GmsDocumentScanningResult.fromActivityResultIntent(r.getData());
                if (res == null || res.getPages() == null) return;
                List<String> added = new ArrayList<>();
                for (GmsDocumentScanningResult.Page p : res.getPages()) added.add(keep(p.getImageUri()));
                session.addAll(added);
                AppLog.i(TAG, "added " + added.size() + " page(s), total " + session.size());
                refresh();
            });

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_scan);
        if (state != null && state.getStringArrayList(KEY_PAGES) != null) session.addAll(state.getStringArrayList(KEY_PAGES));
        empty = findViewById(R.id.empty);
        RecyclerView list = findViewById(R.id.pages);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PageAdapter(session, this::refresh);
        list.setAdapter(adapter);
        filterSpinner = findViewById(R.id.filter);
        filterSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[] {"Original", "Grayscale", "B&W document"}));
        paperSpinner = findViewById(R.id.paper);
        paperSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[] {"PDF: fit image", "PDF: A4", "PDF: Letter"}));
        findViewById(R.id.btn_add).setOnClickListener(v -> startScanner());
        findViewById(R.id.btn_pdf).setOnClickListener(v -> exportPdf());
        findViewById(R.id.btn_jpeg).setOnClickListener(v -> exportImages(ImageExporter.Format.JPEG));
        findViewById(R.id.btn_png).setOnClickListener(v -> exportImages(ImageExporter.Format.PNG));
        refresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putStringArrayList(KEY_PAGES, new ArrayList<>(session.pages()));
    }

    @Override
    protected void onDestroy() {
        io.shutdown();
        super.onDestroy();
    }

    private void startScanner() {
        GmsDocumentScannerOptions opts = new GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build();
        GmsDocumentScanner scanner = GmsDocumentScanning.getClient(opts);
        scanner.getStartScanIntent(this)
                .addOnSuccessListener(sender -> scanLauncher.launch(new IntentSenderRequest.Builder(sender).build()))
                .addOnFailureListener(e -> {
                    AppLog.e(TAG, "scanner unavailable", e);
                    toast("Scanner unavailable (needs Google Play services): " + e.getMessage());
                });
        AppLog.i(TAG, "scanner requested");
    }

    /** Copies a scanner result (in ML Kit's cache) into our files dir so pages survive the session. */
    private String keep(Uri uri) {
        File dir = new File(getFilesDir(), "pages");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("no pages dir");
        File dst = new File(dir, UUID.randomUUID() + ".jpg");
        try (InputStream in = getContentResolver().openInputStream(uri); OutputStream out = new FileOutputStream(dst)) {
            Saver.copy(in, out);
        } catch (Exception e) {
            throw new IllegalStateException("copy page failed: " + uri, e);
        }
        return dst.getAbsolutePath();
    }

    private void refresh() {
        adapter.notifyDataSetChanged();
        empty.setVisibility(session.isEmpty() ? View.VISIBLE : View.GONE);
        setTitle(getString(R.string.scan_title) + " (" + session.size() + ")");
    }

    private PageFilter filter() { return PageFilter.values()[filterSpinner.getSelectedItemPosition()]; }

    private PageSizer.Paper paper() { return PageSizer.Paper.values()[paperSpinner.getSelectedItemPosition()]; }

    private void exportPdf() {
        if (session.isEmpty()) { toast(getString(R.string.no_pages)); return; }
        List<String> pages = session.pages();
        PageFilter f = filter();
        PageSizer.Paper p = paper();
        toast("Exporting PDF…");
        io.execute(() -> {
            try {
                File out = Saver.exportFile(this, FileNamer.pdf("Scan", LocalDateTime.now()));
                try (OutputStream os = new FileOutputStream(out)) { new AndroidPdfExporter(f).export(pages, p, os); }
                String where = Saver.saveToDownloads(this, out, "application/pdf");
                List<File> files = new ArrayList<>();
                files.add(out);
                main.post(() -> done(where, files, "application/pdf"));
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    private void exportImages(ImageExporter.Format fmt) {
        if (session.isEmpty()) { toast(getString(R.string.no_pages)); return; }
        List<String> pages = session.pages();
        PageFilter f = filter();
        toast("Exporting " + fmt + "…");
        io.execute(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                List<File> files = new ArrayList<>();
                String where = null;
                AndroidImageExporter exporter = new AndroidImageExporter(f);
                for (int i = 0; i < pages.size(); i++) {
                    File out = Saver.exportFile(this, FileNamer.page("Scan", now, i, pages.size(), fmt.ext));
                    try (OutputStream os = new FileOutputStream(out)) { exporter.export(pages.get(i), fmt, os); }
                    where = Saver.saveToDownloads(this, out, fmt.mime);
                    files.add(out);
                }
                AppLog.i(TAG, "exported " + files.size() + " " + fmt + " page(s), filter=" + f);
                String last = where;
                main.post(() -> done(files.size() + " file(s), e.g. " + last, files, fmt.mime));
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    private void done(String where, List<File> files, String mime) {
        new AlertDialog.Builder(this)
                .setTitle("Saved")
                .setMessage("Saved to " + where)
                .setPositiveButton("Share", (d, w) -> Saver.share(this, files, mime))
                .setNegativeButton("OK", null)
                .show();
    }

    private void fail(Exception e) {
        AppLog.e(TAG, "export failed", e);
        main.post(() -> toast("Export failed: " + e.getMessage()));
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
