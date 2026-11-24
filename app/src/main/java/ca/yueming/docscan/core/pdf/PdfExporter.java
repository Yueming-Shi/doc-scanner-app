package ca.yueming.docscan.core.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/** Writes pages (opaque refs, e.g. file paths) into one PDF stream. Implemented on Android with PdfDocument. */
public interface PdfExporter {
    /** @return number of pages written */
    int export(List<String> pages, PageSizer.Paper paper, OutputStream out) throws IOException;
}
