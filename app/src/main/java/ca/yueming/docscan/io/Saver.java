package ca.yueming.docscan.io;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import ca.yueming.docscan.AppLog;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** Exports land in cache/exports first; from there they are copied to Downloads and/or shared. */
public final class Saver {
    private Saver() {}

    public static File exportFile(Context ctx, String name) {
        File dir = new File(ctx.getCacheDir(), "exports");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("no exports dir");
        return new File(dir, name);
    }

    /** Copies to Downloads/DocScanner (MediaStore on Android 10+, app Downloads dir before). Returns a display path. */
    public static String saveToDownloads(Context ctx, File f, String mime) throws IOException {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentResolver cr = ctx.getContentResolver();
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, f.getName());
            v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DocScanner");
            Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) throw new IOException("MediaStore insert failed");
            try (OutputStream out = cr.openOutputStream(uri); InputStream in = new FileInputStream(f)) {
                copy(in, out);
            }
            AppLog.i("Save", "saved " + f.getName() + " -> " + uri);
            return "Downloads/DocScanner/" + f.getName();
        }
        File dir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "DocScanner");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("no downloads dir");
        File dst = new File(dir, f.getName());
        try (OutputStream out = new FileOutputStream(dst); InputStream in = new FileInputStream(f)) {
            copy(in, out);
        }
        AppLog.i("Save", "saved " + dst);
        return dst.getAbsolutePath();
    }

    public static void share(Context ctx, List<File> files, String mime) {
        ArrayList<Uri> uris = new ArrayList<>();
        for (File f : files) uris.add(FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".files", f));
        Intent i;
        if (uris.size() == 1) {
            i = new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.get(0));
        } else {
            i = new Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }
        i.setType(mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ctx.startActivity(Intent.createChooser(i, "Share"));
        AppLog.i("Save", "share " + uris.size() + " file(s)");
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }
}
