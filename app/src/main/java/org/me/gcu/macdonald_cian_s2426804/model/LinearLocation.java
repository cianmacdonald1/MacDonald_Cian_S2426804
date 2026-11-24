package org.me.gcu.macdonald_cian_s2426804.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The groupOfLocations element. Most situations are a stretch of road between a
 * "from" point and a "to" point, so the map needs to draw a line, not a single pin.
 */
public class LinearLocation {

    private String locationGroupType;  // xsi:type of groupOfLocations, e.g. "Linear"
    private String direction;          // tpegDirection, e.g. "southBound"
    private String linearType;         // tpegLinearLocationType, e.g. "segment"
    private PointLocation from;
    private PointLocation to;
    private PointLocation singlePoint; // used if the feed ever sends a point location

    /** All points that have coordinates, in from -> to order. */
    public List<PointLocation> getPoints() {
        List<PointLocation> points = new ArrayList<>();
        if (from != null && from.hasCoordinates()) points.add(from);
        if (to != null && to.hasCoordinates()) points.add(to);
        if (singlePoint != null && singlePoint.hasCoordinates()) points.add(singlePoint);
        return points;
    }

    /** Mid point of the stretch, used for the map marker and distance search. */
    public double[] getCentre() {
        List<PointLocation> pts = getPoints();
        if (pts.isEmpty()) return null;
        double lat = 0, lon = 0;
        for (PointLocation p : pts) { lat += p.getLatitude(); lon += p.getLongitude(); }
        return new double[]{lat / pts.size(), lon / pts.size()};
    }

    /** Length of the from -> to stretch in km (0 for a single point). */
    public double getLengthKm() {
        if (from == null || to == null || !from.hasCoordinates() || !to.hasCoordinates()) return 0;
        return GeoUtil.distanceKm(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude());
    }

    public String getDirectionLabel() { return TextUtil.humanise(direction); }

    // ---- getters / setters ----
    public String getLocationGroupType() { return locationGroupType; }
    public void setLocationGroupType(String t) { this.locationGroupType = t; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getLinearType() { return linearType; }
    public void setLinearType(String linearType) { this.linearType = linearType; }
    public PointLocation getFrom() { return from; }
    public void setFrom(PointLocation from) { this.from = from; }
    public PointLocation getTo() { return to; }
    public void setTo(PointLocation to) { this.to = to; }
    public PointLocation getSinglePoint() { return singlePoint; }
    public void setSinglePoint(PointLocation singlePoint) { this.singlePoint = singlePoint; }
}
