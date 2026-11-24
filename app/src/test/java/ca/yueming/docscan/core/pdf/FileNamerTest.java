package ca.yueming.docscan.core.pdf;

import static org.junit.Assert.*;

import java.time.LocalDateTime;
import org.junit.Test;

public class FileNamerTest {
    private final LocalDateTime t = LocalDateTime.of(2026, 11, 23, 23, 15, 7);

    @Test public void pdfName() { assertEquals("Scan_20261123_231507.pdf", FileNamer.pdf("Scan", t)); }

    @Test public void pageNamesArePaddedAndOneBased() {
        assertEquals("Scan_20261123_231507_p01.jpg", FileNamer.page("Scan", t, 0, 3, "jpg"));
        assertEquals("Scan_20261123_231507_p012.png", FileNamer.page("Scan", t, 11, 120, "png"));
    }
}
