package org.me.gcu.macdonald_cian_s2426804.model;

/**
 * Which of the two Traffic Scotland feeds a situation came from.
 * The URLs are the DATEX II "Content" publication endpoints from the developer guide.
 */
public enum FeedType {
    INCIDENT("Incident", "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml"),
    ROADWORKS("Roadworks", "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml");

    private final String label;
    private final String url;

    FeedType(String label, String url) {
        this.label = label;
        this.url = url;
    }

    public String getLabel() { return label; }
    public String getUrl() { return url; }
}
