package org.me.gcu.macdonald_cian_s2426804.data;

/** Where the data currently on screen came from. */
public enum DataSource {
    LIVE("Live"),
    CACHE("Saved copy"),
    SAMPLE("Sample data");

    private final String label;

    DataSource(String label) { this.label = label; }

    public String getLabel() { return label; }
}
