package org.me.gcu.macdonald_cian_s2426804.model;

import java.time.Duration;
import java.time.OffsetDateTime;

/** The validity element: when a situation starts and (for roadworks) ends. */
public class Validity {

    private String validityStatus;
    private OffsetDateTime overallStart;
    private OffsetDateTime overallEnd;

    /** Length of the works, or null when there's no end time (most incidents). */
    public Duration getDuration() {
        if (overallStart == null || overallEnd == null) return null;
        return Duration.between(overallStart, overallEnd);
    }

    public DurationLevel getDurationLevel() {
        return DurationLevel.fromDuration(getDuration());
    }

    /** Is the situation in force at the given time? */
    public boolean isActiveAt(OffsetDateTime now) {
        boolean started = overallStart == null || !overallStart.isAfter(now);
        boolean notEnded = overallEnd == null || overallEnd.isAfter(now);
        return started && notEnded;
    }

    public boolean isUpcomingAt(OffsetDateTime now) {
        return overallStart != null && overallStart.isAfter(now);
    }

    /** "3 days", "5 hours", "2 years 4 months" style text. */
    public static String formatDuration(Duration d) {
        if (d == null) return "Ongoing";
        long minutes = d.toMinutes();
        if (minutes < 60) return minutes + " min";
        long hours = d.toHours();
        if (hours < 48) return hours + (hours == 1 ? " hour" : " hours");
        long days = d.toDays();
        if (days < 60) return days + " days";
        long months = days / 30;
        if (months < 24) return months + " months";
        return (days / 365) + " years";
    }

    // ---- getters / setters ----
    public String getValidityStatus() { return validityStatus; }
    public void setValidityStatus(String s) { this.validityStatus = s; }
    public OffsetDateTime getOverallStart() { return overallStart; }
    public void setOverallStart(OffsetDateTime overallStart) { this.overallStart = overallStart; }
    public OffsetDateTime getOverallEnd() { return overallEnd; }
    public void setOverallEnd(OffsetDateTime overallEnd) { this.overallEnd = overallEnd; }
}
