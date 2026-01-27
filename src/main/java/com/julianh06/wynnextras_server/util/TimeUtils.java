package com.julianh06.wynnextras_server.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public class TimeUtils {
    private static final ZoneId CET = ZoneId.of("CET");
    private static final int RESET_HOUR = 19;
    private static final int RESET_MINUTE = 0;

    /**
     * Get current time in CET timezone
     */
    public static ZonedDateTime getCurrentCETTime() {
        return ZonedDateTime.now(CET);
    }

    /**
     * Get week identifier for loot pools (resets Friday 19:00 CET)
     * Returns format: "YYYY-Wxx" (e.g., "2026-W04")
     *
     * The week is defined as: Friday 19:00 CET to next Friday 19:00 CET
     */
    public static String getWeekIdentifier() {
        ZonedDateTime now = getCurrentCETTime();

        // Find the most recent Friday 19:00 (or current if exactly at reset time)
        ZonedDateTime lastReset = now
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
            .withHour(RESET_HOUR)
            .withMinute(RESET_MINUTE)
            .withSecond(0)
            .withNano(0);

        // If we're before Friday 19:00 this week, go back another week
        if (now.isBefore(lastReset)) {
            lastReset = lastReset.minusWeeks(1);
        }

        // Use ISO week number from the Friday that started this period
        int year = lastReset.getYear();
        int weekNumber = lastReset.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());

        return String.format("%d-W%02d", year, weekNumber);
    }

    /**
     * Get day identifier for gambits (resets daily at 19:00 CET)
     * Returns format: "YYYY-MM-DD" (e.g., "2026-01-27")
     *
     * The day is defined as: 19:00 CET to next day 19:00 CET
     */
    public static String getDayIdentifier() {
        ZonedDateTime now = getCurrentCETTime();

        // If it's before 19:00, use yesterday's date
        if (now.getHour() < RESET_HOUR) {
            now = now.minusDays(1);
        }

        return now.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * Get the next reset time for loot pools (next Friday 19:00 CET)
     */
    public static ZonedDateTime getNextLootPoolReset() {
        ZonedDateTime now = getCurrentCETTime();
        ZonedDateTime nextReset = now
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
            .withHour(RESET_HOUR)
            .withMinute(RESET_MINUTE)
            .withSecond(0)
            .withNano(0);

        // If we're past Friday 19:00 this week, go to next Friday
        if (!now.isBefore(nextReset)) {
            nextReset = nextReset.plusWeeks(1);
        }

        return nextReset;
    }

    /**
     * Get the next reset time for gambits (next day 19:00 CET)
     */
    public static ZonedDateTime getNextGambitReset() {
        ZonedDateTime now = getCurrentCETTime();
        ZonedDateTime nextReset = now
            .withHour(RESET_HOUR)
            .withMinute(RESET_MINUTE)
            .withSecond(0)
            .withNano(0);

        // If we're past 19:00 today, go to tomorrow
        if (!now.isBefore(nextReset)) {
            nextReset = nextReset.plusDays(1);
        }

        return nextReset;
    }
}
