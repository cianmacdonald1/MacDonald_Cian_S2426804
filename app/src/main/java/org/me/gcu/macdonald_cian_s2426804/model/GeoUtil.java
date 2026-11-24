package org.me.gcu.macdonald_cian_s2426804.model;

/** Distance maths for the "search by distance" feature. */
public final class GeoUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtil() { }

    /** Great-circle distance between two lat/long points (haversine formula). */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** "850 m" or "12.3 km". */
    public static String formatDistance(double km) {
        if (km < 1) return Math.round(km * 1000) + " m";
        if (km < 10) return String.format(java.util.Locale.UK, "%.1f km", km);
        return Math.round(km) + " km";
    }
}
