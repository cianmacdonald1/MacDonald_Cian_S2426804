package org.me.gcu.macdonald_cian_s2426804.model;

import java.time.Duration;

/** How long a set of roadworks lasts, used for the duration colour code. */
public enum DurationLevel {
    SHORT("Short works (up to 3 days)"),
    MEDIUM("Medium works (up to 4 weeks)"),
    LONG("Long works (over 4 weeks)"),
    UNKNOWN("No end date");

    private final String label;

    DurationLevel(String label) { this.label = label; }

    public String getLabel() { return label; }

    public static DurationLevel fromDuration(Duration d) {
        if (d == null || d.isNegative()) return UNKNOWN;
        if (d.toHours() <= 72) return SHORT;
        if (d.toDays() <= 28) return MEDIUM;
        return LONG;
    }
}
