package com.gathin.tvhome;

import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

/**
 * Manages parental PIN verification for adult / restricted apps.
 *
 * Rule: hhddmm (current hour [00-23], day of month [01-31], month [01-12]).
 * E.g., at 10:15 on 01 November -> PIN is "100111".
 *
 * To ensure stealth, PIN input is completely masked on screen (dots only),
 * and dynamic time-based expiration prevents kids from reusing observed pins.
 */
public final class PinManager {

    private PinManager() {}

    /**
     * Checks whether an app is subject to PIN lock.
     */
    public static boolean isProtected(Set<String> protectedApps, String packageName) {
        if (packageName == null || protectedApps == null) return false;
        return protectedApps.contains(packageName);
    }

    /**
     * Formats a 6-digit PIN from a Calendar instance in hhddmm format.
     */
    public static String formatPin(Calendar cal) {
        int hh = cal.get(Calendar.HOUR_OF_DAY);
        int dd = cal.get(Calendar.DAY_OF_MONTH);
        int mm = cal.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-indexed (0=Jan, 11=Dec)
        return String.format(Locale.US, "%02d%02d%02d", hh, dd, mm);
    }

    /**
     * Verifies the entered 6-digit PIN against the current hhddmm.
     * Also allows a 2-minute grace period around hour transitions
     * (e.g. typing started at 10:59 and ended at 11:00).
     */
    public static boolean verifyPin(String entered) {
        if (entered == null || entered.length() != 6) return false;

        Calendar now = Calendar.getInstance();
        String currentPin = formatPin(now);
        if (entered.equals(currentPin)) return true;

        // Grace period check: 2 minutes ago
        Calendar past = (Calendar) now.clone();
        past.add(Calendar.MINUTE, -2);
        if (entered.equals(formatPin(past))) return true;

        return false;
    }
}
