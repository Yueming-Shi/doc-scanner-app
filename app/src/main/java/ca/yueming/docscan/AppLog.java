package ca.yueming.docscan;

import android.util.Log;

/** Tiny logging wrapper so every log line shares the "DocScan" tag prefix. */
public final class AppLog {
    private AppLog() {}

    public static void i(String area, String msg) { Log.i("DocScan/" + area, msg); }

    public static void w(String area, String msg) { Log.w("DocScan/" + area, msg); }

    public static void e(String area, String msg, Throwable t) { Log.e("DocScan/" + area, msg, t); }
}
