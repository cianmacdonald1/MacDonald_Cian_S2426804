package org.me.gcu.macdonald_cian_s2426804.model;

/** The delaysType value from the impact element, used for the delay colour code. */
public enum DelayLevel {
    NONE("No delays reported"),
    SHORT("Short delays"),
    LONG("Long delays"),
    VERY_LONG("Very long delays");

    private final String label;

    DelayLevel(String label) { this.label = label; }

    public String getLabel() { return label; }

    public static DelayLevel fromDelaysType(String delaysType) {
        if (delaysType == null) return NONE;
        switch (delaysType) {
            case "delays":
            case "delaysOfUncertainDuration":
            case "negligible":
                return SHORT;
            case "longDelays":
                return LONG;
            case "veryLongDelays":
                return VERY_LONG;
            default:
                return NONE;
        }
    }
}
