package ca.yueming.docscan.core.pdf;

import java.io.IOException;
import java.io.OutputStream;

/** Writes one page as an image stream. */
public interface ImageExporter {
    enum Format {
        JPEG("jpg", "image/jpeg"), PNG("png", "image/png");
        public final String ext, mime;
        Format(String ext, String mime) { this.ext = ext; this.mime = mime; }
    }

    void export(String page, Format format, OutputStream out) throws IOException;
}
