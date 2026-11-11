package com.ahmed2027.app.core;

import static org.junit.Assert.*;

import com.ahmed2027.app.growth.*;
import com.ahmed2027.app.learn.YouTubeId;

import org.junit.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class GrowthTest {
    private static final LocalDate T = LocalDate.of(2026, 11, 11);
    private static Set<LocalDate> days(int... back) { Set<LocalDate> s = new HashSet<>(); for (int b : back) s.add(T.minusDays(b)); return s; }

    // ---- Meta
    @Test public void metaRoundTrip() {
        Meta m = Meta.parse("a=1;b=hello");
        assertEquals("1", m.get("a")); assertEquals("hello", m.get("b")); assertNull(m.get("c")); assertEquals("x", m.get("c", "x"));
        m.set("c", "z").setFlag("fav", true).set("a", (String) null);
        assertEquals("b=hello;c=z;fav=1", m.toString());
        assertTrue(Meta.parse(m.toString()).flag("fav"));
        assertEquals(7, Meta.parse("n=7").getLong("n", 0)); assertEquals(5, Meta.parse("n=x").getLong("n", 5));
        assertEquals("a,b-c", Meta.parse("").set("k", "a;b=c").get("k")); // separators cannot corrupt the format
        assertEquals("", Meta.parse(null).toString());
    }

    // ---- habits (restart gently)
    @Test public void runCountsConsecutiveDaysEvenIfTodayNotDoneYet() {
        assertEquals(3, HabitMath.run(days(0, 1, 2), T));
        assertEquals(2, HabitMath.run(days(1, 2), T));      // today pending: yesterday's run still shown
        assertEquals(0, HabitMath.run(days(2, 3), T));      // gap: no scary "broken" number, just 0
        assertEquals(0, HabitMath.run(days(), T));
    }
    @Test public void comebackAfterAtLeastTwoDaysAway() {
        assertTrue(HabitMath.comeback(days(0, 5, 6), T));
        assertFalse(HabitMath.comeback(days(0, 1), T));      // no gap
        assertFalse(HabitMath.comeback(days(0), T));         // very first completion is not a comeback
        assertFalse(HabitMath.comeback(days(5), T));         // not done today
        assertFalse(HabitMath.comeback(days(0, 2, 5), T));   // only one day missed
    }
    @Test public void weekDoneCountsLastSevenDays() { assertEquals(3, HabitMath.weekDone(days(0, 6, 7, 3), T)); }

    // ---- growth score
    @Test public void scoreIsMeanOfAvailableComponentsOnly() {
        GrowthScore.Input in = new GrowthScore.Input();
        assertNull(GrowthScore.score(in));
        in.prayer = 80; in.habits = 40;
        assertEquals(Integer.valueOf(60), GrowthScore.score(in));
        in.learning = 0;                       // a real zero counts
        assertEquals(Integer.valueOf(40), GrowthScore.score(in));
    }
    @Test public void componentMaths() {
        assertEquals(100, GrowthScore.pct(500, 150)); assertEquals(0, GrowthScore.pct(-3, 150)); assertEquals(50, GrowthScore.pct(75, 150));
        assertEquals(100, GrowthScore.prayerPct(35, 0)); assertEquals(70, GrowthScore.prayerPct(0, 35)); assertEquals(0, GrowthScore.prayerPct(0, 0));
        assertNull(GrowthScore.habitsPct(5, 0)); assertEquals(Integer.valueOf(50), GrowthScore.habitsPct(7, 2));
    }
    @Test public void trendNeedsBothWeeks() {
        assertEquals(Integer.valueOf(-10), GrowthScore.trend(50, 60)); assertNull(GrowthScore.trend(null, 60)); assertNull(GrowthScore.trend(50, null));
    }

    // ---- Leitner
    @Test public void leitnerBoxesMoveUpAndResetOnMiss() {
        assertEquals(2, Leitner.nextBox(1, true)); assertEquals(5, Leitner.nextBox(5, true)); assertEquals(1, Leitner.nextBox(4, false));
        assertEquals(1000 + 1, Leitner.dueDay(1000, 1)); assertEquals(1000 + 16, Leitner.dueDay(1000, 5));
        assertTrue(Leitner.isDue(999, 1000)); assertTrue(Leitner.isDue(1000, 1000)); assertFalse(Leitner.isDue(1001, 1000));
    }

    // ---- reading
    @Test public void readingPlan() {
        assertEquals(8, ReadingPlan.daysToFinish(300, 60, 30)); assertEquals(0, ReadingPlan.daysToFinish(300, 300, 30));
        assertEquals(-1, ReadingPlan.daysToFinish(300, 0, 0)); assertEquals(20, ReadingPlan.percent(300, 60)); assertEquals(100, ReadingPlan.percent(10, 99));
        assertEquals(0, ReadingPlan.remaining(10, 99));
    }

    // ---- fitness
    @Test public void progressionNeverExceedsTenPercent() {
        assertEquals(15, Progression.suggest(0)); assertEquals(33, Progression.suggest(30)); assertEquals(11, Progression.suggest(10));
        assertEquals(33, Progression.limit(30, 100)); assertEquals(20, Progression.limit(30, 20)); assertEquals(15, Progression.limit(0, 60));
        for (int w = 1; w < 500; w++) assertTrue(Progression.suggest(w) <= Math.ceil(w * 1.10) + 1e-9);
    }
    @Test public void restAfterThreeActiveDaysAndRotationOtherwise() {
        assertEquals(FitnessPlan.REST, FitnessPlan.suggest(new boolean[]{true, true, true}, 100, 5));
        int a = FitnessPlan.suggest(new boolean[]{true, true, false}, 100, 5), b = FitnessPlan.suggest(new boolean[]{true, true, false}, 101, 5);
        assertNotEquals(a, b); assertTrue(a >= 0 && a < 5);
        assertEquals(FitnessPlan.REST, FitnessPlan.suggest(null, 1, 0));
    }

    // ---- YouTube
    @Test public void youTubeIdParsing() {
        String id = "dQw4w9WgXcQ";
        assertEquals(id, YouTubeId.parse(id));
        assertEquals(id, YouTubeId.parse("https://www.youtube.com/watch?v=" + id + "&t=10s"));
        assertEquals(id, YouTubeId.parse("youtube.com/watch?feature=share&v=" + id));
        assertEquals(id, YouTubeId.parse("https://youtu.be/" + id + "?si=abc"));
        assertEquals(id, YouTubeId.parse("https://m.youtube.com/watch?v=" + id));
        assertEquals(id, YouTubeId.parse("https://www.youtube.com/embed/" + id));
        assertEquals(id, YouTubeId.parse("https://www.youtube-nocookie.com/embed/" + id + "?rel=0"));
    }
    @Test public void youTubeRejectsShortsAndOtherSites() {
        assertNull(YouTubeId.parse("https://www.youtube.com/shorts/dQw4w9WgXcQ"));
        assertNull(YouTubeId.parse("https://evil.example.com/watch?v=dQw4w9WgXcQ"));
        assertNull(YouTubeId.parse("https://www.youtube.com/@BBCLearningEnglish"));
        assertNull(YouTubeId.parse("https://www.youtube.com/watch?v=short"));
        assertNull(YouTubeId.parse(null)); assertNull(YouTubeId.parse(""));
        assertNull(YouTubeId.parse("https://youtu.be.evil.com/dQw4w9WgXcQ"));
    }
    @Test public void embedUsesPrivacyHostAndRelZeroAndOnlyThatHostIsAllowed() {
        String u = YouTubeId.embedUrl("dQw4w9WgXcQ");
        assertTrue(u.startsWith("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?")); assertTrue(u.contains("rel=0"));
        assertTrue(YouTubeId.embedHtml("dQw4w9WgXcQ").contains(u));
        assertTrue(YouTubeId.isAllowedHost("www.youtube-nocookie.com")); assertFalse(YouTubeId.isAllowedHost("www.youtube.com"));
        assertFalse(YouTubeId.isAllowedHost(null));
    }
}
