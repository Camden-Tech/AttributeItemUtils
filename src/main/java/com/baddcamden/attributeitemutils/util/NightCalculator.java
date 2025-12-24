package com.baddcamden.attributeitemutils.util;

/**
 * Utility helpers for translating world time into night counts while keeping the value within
 * a predictable and safe range for chance calculations.
 */
public final class NightCalculator {

    private static final long TICKS_PER_NIGHT = 24000L;
    private static final long MAX_TRACKED_NIGHTS = Integer.MAX_VALUE;

    private NightCalculator() {
    }

    /**
     * Converts the provided world time to a clamped night count.
     */
    public static long nightsFromWorldTime(long worldTime) {
        long nights = worldTime / TICKS_PER_NIGHT;
        return clampNights(nights);
    }

    /**
     * Restricts the provided night count to a sensible non-negative range to keep probability
     * calculations stable when very large world times are provided by the server or commands.
     */
    public static long clampNights(long nights) {
        if (nights < 0) {
            return 0;
        }
        if (nights > MAX_TRACKED_NIGHTS) {
            return MAX_TRACKED_NIGHTS;
        }
        return nights;
    }
}
