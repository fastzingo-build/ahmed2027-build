package com.ahmed2027.app.core;

import static org.junit.Assert.*;

import com.ahmed2027.app.launcher.AppClass;
import com.ahmed2027.app.launcher.AppFilter;

import org.junit.Test;

public class AppFilterTest {
    @Test public void allChipHidesHiddenApps() {
        assertTrue(AppFilter.matches("Maps", "com.maps", AppClass.ALLOWED, null, ""));
        assertFalse(AppFilter.matches("Secret", "com.s", AppClass.HIDDEN, null, ""));
    }
    @Test public void hiddenChipShowsOnlyHidden() {
        assertTrue(AppFilter.matches("Secret", "com.s", AppClass.HIDDEN, AppClass.HIDDEN, ""));
        assertFalse(AppFilter.matches("Maps", "com.maps", AppClass.ALLOWED, AppClass.HIDDEN, ""));
    }
    @Test public void classChipFilters() {
        assertTrue(AppFilter.matches("A", "a", AppClass.LIMITED, AppClass.LIMITED, null));
        assertFalse(AppFilter.matches("A", "a", AppClass.LIMITED, AppClass.BLOCKED, null));
    }
    @Test public void searchIsCaseInsensitiveOnLabelAndPackage() {
        assertTrue(AppFilter.matches("Instagram", "com.instagram.android", AppClass.ALLOWED, null, "INSTA"));
        assertTrue(AppFilter.matches("Foo", "com.bar.baz", AppClass.ALLOWED, null, "bar"));
        assertFalse(AppFilter.matches("Foo", "com.bar", AppClass.ALLOWED, null, "zzz"));
    }
    @Test public void searchNeverRevealsHiddenOnAllChip() {
        assertFalse(AppFilter.matches("Secret", "com.s", AppClass.HIDDEN, null, "secret"));
    }
    @Test public void unknownClassIsAllowed() {
        assertTrue(AppFilter.matches("A", "a", "weird", AppClass.ALLOWED, null));
    }
}
