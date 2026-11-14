package com.universeclock;

import java.util.Locale;

/**
 * Перевод между игровыми тиками и «настенными» часами ЧЧ:ММ:СС.
 *
 * В Minecraft сутки = 24000 тиков, а тик 0 соответствует 06:00:00 утра
 * (тик 6000 — полдень, 12000 — закат, 18000 — полночь).
 * Один тик = 3,6 секунды суточных часов, поэтому при установке времени
 * значение округляется до ближайшего тика.
 */
public final class TimeUtil {
    public static final int TICKS_PER_DAY = 24000;
    public static final int SECONDS_PER_DAY = 86400;

    /** Тик 0 = 06:00:00. */
    private static final int DAY_START_OFFSET = 6 * 3600;

    private TimeUtil() {
    }

    /** Тики суток (любое число) -> секунды суток 0..86399 по 24-часовому циферблату. */
    public static int ticksToClockSeconds(long dayTime) {
        int ticks = (int) Math.floorMod(dayTime, (long) TICKS_PER_DAY);
        int sinceDawn = (ticks * 36 + 5) / 10; // round(ticks * 3.6)
        return Math.floorMod(sinceDawn + DAY_START_OFFSET, SECONDS_PER_DAY);
    }

    /** Секунды суток по циферблату -> ближайший тик суток 0..23999. */
    public static int clockSecondsToTicks(int clockSeconds) {
        int sinceDawn = Math.floorMod(clockSeconds - DAY_START_OFFSET, SECONDS_PER_DAY);
        int ticks = (sinceDawn * 10 + 18) / 36; // round(sinceDawn / 3.6)
        return Math.floorMod(ticks, TICKS_PER_DAY);
    }

    public static String format(int clockSeconds) {
        int s = Math.floorMod(clockSeconds, SECONDS_PER_DAY);
        return String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }
}
