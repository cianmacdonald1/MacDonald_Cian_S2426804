package org.me.gcu.macdonald_cian_s2426804.viewmodel;

/** Centre point and radius for the "search by distance" feature. */
public class NearbyQuery {

    /** Fixed fallback location: Glasgow Caledonian University campus. */
    public static final double GCU_LAT = 55.8667;
    public static final double GCU_LON = -4.2502;

    public final double latitude;
    public final double longitude;
    public final String placeLabel;
    public final int radiusKm;
    public final boolean fromDevice;

    public NearbyQuery(double latitude, double longitude, String placeLabel, int radiusKm, boolean fromDevice) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.placeLabel = placeLabel;
        this.radiusKm = radiusKm;
        this.fromDevice = fromDevice;
    }

    public static NearbyQuery gcu(int radiusKm) {
        return new NearbyQuery(GCU_LAT, GCU_LON, "GCU campus", radiusKm, false);
    }

    public NearbyQuery withRadius(int km) {
        return new NearbyQuery(latitude, longitude, placeLabel, km, fromDevice);
    }
}
