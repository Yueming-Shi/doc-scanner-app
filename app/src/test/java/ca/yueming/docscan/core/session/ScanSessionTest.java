package ca.yueming.docscan.core.session;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

public class ScanSessionTest {
    private ScanSession abc() {
        ScanSession s = new ScanSession();
        s.addAll(Arrays.asList("a", "b", "c"));
        return s;
    }

    @Test public void addKeepsOrderAcrossScans() {
        ScanSession s = abc();
        s.addAll(Arrays.asList("d"));
        assertEquals(Arrays.asList("a", "b", "c", "d"), s.pages());
    }

    @Test public void moveForwardAndBack() {
        ScanSession s = abc();
        s.move(0, 2);
        assertEquals(Arrays.asList("b", "c", "a"), s.pages());
        s.move(2, 0);
        assertEquals(Arrays.asList("a", "b", "c"), s.pages());
    }

    @Test public void moveUpDownStopsAtEnds() {
        ScanSession s = abc();
        assertFalse(s.moveUp(0));
        assertFalse(s.moveDown(2));
        assertTrue(s.moveDown(0));
        assertEquals(Arrays.asList("b", "a", "c"), s.pages());
        assertTrue(s.moveUp(2));
        assertEquals(Arrays.asList("b", "c", "a"), s.pages());
    }

    @Test public void removeAndClear() {
        ScanSession s = abc();
        assertEquals("b", s.remove(1));
        assertEquals(Arrays.asList("a", "c"), s.pages());
        s.clear();
        assertTrue(s.isEmpty());
    }

    @Test(expected = IndexOutOfBoundsException.class) public void badIndexThrows() { abc().remove(3); }

    @Test(expected = IllegalArgumentException.class) public void emptyRefRejected() {
        new ScanSession().addAll(Arrays.asList(""));
    }

    @Test(expected = UnsupportedOperationException.class) public void pagesIsReadOnly() {
        abc().pages().add("x");
    }
}
