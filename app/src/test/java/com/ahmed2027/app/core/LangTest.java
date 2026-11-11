package com.ahmed2027.app.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class LangTest {
    @Test public void arabicIsRtl() { assertTrue(Lang.isRtl("ar")); }
    @Test public void englishIsLtr() { assertFalse(Lang.isRtl("en")); }
    @Test public void defaultIsArabicRtl() {
        assertEquals("ar", Lang.DEFAULT);
        assertTrue(Lang.isRtl(null));
        assertEquals("ar", Lang.normalize("fr"));
    }
    @Test public void switchingUpdatesDirection() {
        assertTrue(Lang.isRtl(Lang.AR));
        assertFalse(Lang.isRtl(Lang.EN));
    }
}
