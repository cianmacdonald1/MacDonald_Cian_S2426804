package org.me.gcu.macdonald_cian_s2426804.model;

/** The impact element: lanes affected and delays. Any field may be missing. */
public class Impact {

    private Integer numberOfLanesRestricted;
    private Integer numberOfOperationalLanes;
    private String delaysType;       // delays / longDelays / veryLongDelays
    private Integer delayTimeSeconds; // delayTimeValue, if the feed sends one

    public DelayLevel getDelayLevel() { return DelayLevel.fromDelaysType(delaysType); }

    public boolean isEmpty() {
        return numberOfLanesRestricted == null && numberOfOperationalLanes == null
                && delaysType == null && delayTimeSeconds == null;
    }

    /** One line summary such as "Road closed (0 lanes open)" or "2 lanes restricted". */
    public String getLanesSummary() {
        if (numberOfOperationalLanes != null) {
            if (numberOfOperationalLanes == 0) return "No lanes open";
            return numberOfOperationalLanes + (numberOfOperationalLanes == 1 ? " lane open" : " lanes open");
        }
        if (numberOfLanesRestricted != null) {
            if (numberOfLanesRestricted == 0) return "Restricted (lane count not given)";
            return numberOfLanesRestricted + (numberOfLanesRestricted == 1 ? " lane restricted" : " lanes restricted");
        }
        return null;
    }

    // ---- getters / setters ----
    public Integer getNumberOfLanesRestricted() { return numberOfLanesRestricted; }
    public void setNumberOfLanesRestricted(Integer n) { this.numberOfLanesRestricted = n; }
    public Integer getNumberOfOperationalLanes() { return numberOfOperationalLanes; }
    public void setNumberOfOperationalLanes(Integer n) { this.numberOfOperationalLanes = n; }
    public String getDelaysType() { return delaysType; }
    public void setDelaysType(String delaysType) { this.delaysType = delaysType; }
    public Integer getDelayTimeSeconds() { return delayTimeSeconds; }
    public void setDelayTimeSeconds(Integer s) { this.delayTimeSeconds = s; }
}
