package org.me.gcu.macdonald_cian_s2426804.model;

import java.util.Locale;

/**
 * What kind of situation this is. Worked out from the situationRecord xsi:type,
 * its sub-type element (e.g. roadMaintenanceType) and the comment text.
 * Each category gets its own icon and colour in the UI.
 */
public enum SituationCategory {
    ROAD_CLOSED("Road closed"),
    LANE_RESTRICTION("Lane restriction"),
    QUEUING_TRAFFIC("Queuing traffic"),
    BROKEN_DOWN_VEHICLE("Broken down vehicle"),
    ACCIDENT("Accident"),
    OBSTRUCTION("Obstruction / hazard"),
    WEATHER("Weather / road conditions"),
    ROADWORKS("Roadworks"),
    ROADWORKS_CLOSURE("Roadworks – closure"),
    ROADWORKS_LANE_CLOSURE("Roadworks – lane closure"),
    ROADWORKS_TRAFFIC_LIGHTS("Roadworks – temporary lights"),
    OTHER("Other event");

    private final String label;

    SituationCategory(String label) { this.label = label; }

    public String getLabel() { return label; }

    public boolean isRoadworks() {
        return this == ROADWORKS || this == ROADWORKS_CLOSURE
                || this == ROADWORKS_LANE_CLOSURE || this == ROADWORKS_TRAFFIC_LIGHTS;
    }

    /** Decides the category for a parsed situation. */
    public static SituationCategory classify(String recordType, String subType, String comment, FeedType feed) {
        String type = recordType == null ? "" : recordType;
        String sub = subType == null ? "" : subType;
        String text = comment == null ? "" : comment.toLowerCase(Locale.UK);

        if (type.equals("MaintenanceWorks") || type.equals("ConstructionWorks") || feed == FeedType.ROADWORKS) {
            if (text.contains("ttl") || text.contains("traffic light") || text.contains("signals")) {
                return ROADWORKS_TRAFFIC_LIGHTS;
            }
            if (text.contains("lane closure") || text.contains("lane closed") || text.contains("lane restriction")) {
                return ROADWORKS_LANE_CLOSURE;
            }
            if (text.contains("closure") || text.contains("closed")) return ROADWORKS_CLOSURE;
            return ROADWORKS;
        }
        if (type.equals("RoadOrCarriagewayOrLaneManagement")) {
            if (sub.equals("roadClosed") || sub.equals("carriagewayClosures")) return ROAD_CLOSED;
            return LANE_RESTRICTION;
        }
        if (type.equals("AbnormalTraffic")) return QUEUING_TRAFFIC;
        if (type.equals("VehicleObstruction")) {
            return sub.equals("vehicleOnFire") ? ACCIDENT : BROKEN_DOWN_VEHICLE;
        }
        if (type.equals("Accident")) return ACCIDENT;
        if (type.endsWith("Obstruction")) return OBSTRUCTION;
        if (type.contains("Weather") || type.contains("Environment") || type.contains("RoadConditions")) {
            return WEATHER;
        }
        return OTHER;
    }
}
