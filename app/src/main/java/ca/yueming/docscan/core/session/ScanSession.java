package ca.yueming.docscan.core.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ordered pages of one multi-page scan. Pages are opaque string references (e.g. file URIs). */
public final class ScanSession {
    private final List<String> pages = new ArrayList<>();

    public void addAll(List<String> newPages) {
        for (String p : newPages) {
            if (p == null || p.isEmpty()) throw new IllegalArgumentException("empty page ref");
            pages.add(p);
        }
    }

    /** Moves the page at {@code from} to index {@code to}; other pages shift. */
    public void move(int from, int to) {
        check(from);
        check(to);
        pages.add(to, pages.remove(from));
    }

    /** Moves a page one place up (towards index 0); returns false at the top. */
    public boolean moveUp(int index) {
        check(index);
        if (index == 0) return false;
        move(index, index - 1);
        return true;
    }

    /** Moves a page one place down; returns false at the bottom. */
    public boolean moveDown(int index) {
        check(index);
        if (index == pages.size() - 1) return false;
        move(index, index + 1);
        return true;
    }

    public String remove(int index) {
        check(index);
        return pages.remove(index);
    }

    public void clear() { pages.clear(); }

    public int size() { return pages.size(); }

    public boolean isEmpty() { return pages.isEmpty(); }

    public List<String> pages() { return Collections.unmodifiableList(new ArrayList<>(pages)); }

    private void check(int i) {
        if (i < 0 || i >= pages.size()) throw new IndexOutOfBoundsException("page " + i + " of " + pages.size());
    }
}
