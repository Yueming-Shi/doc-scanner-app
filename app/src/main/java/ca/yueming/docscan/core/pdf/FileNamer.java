package ca.yueming.docscan.core.pdf;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Builds export file names like "Scan_20261123_231500.pdf" or "Scan_20261123_231500_p02.jpg". */
public final class FileNamer {
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private FileNamer() {}

    public static String pdf(String prefix, LocalDateTime t) { return prefix + "_" + F.format(t) + ".pdf"; }

    public static String page(String prefix, LocalDateTime t, int pageIndex, int pageCount, String ext) {
        int digits = Math.max(2, String.valueOf(pageCount).length());
        return String.format("%s_%s_p%0" + digits + "d.%s", prefix, F.format(t), pageIndex + 1, ext);
    }
}
