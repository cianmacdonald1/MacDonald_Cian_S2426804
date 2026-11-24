//
// Name                 Cian MacDonald
// Student ID           S2426804
// Programme of Study   BSc (Hons) Software Engineering
//
package org.me.gcu.macdonald_cian_s2426804.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One end of a situation's location (the "from" or "to" point in the DATEX II
 * tpegLinearLocation), or the single point of a point location.
 * Holds the coordinates plus every naming element the feed gives for the point.
 */
public class PointLocation {

    private String pointType;          // xsi:type, e.g. TpegJunction / TpegNonJunctionPoint
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    private String junctionName;       // e.g. "J19"
    private final List<String> roadNumbers = new ArrayList<>(); // ilc names, e.g. "A74(M)", "B957"
    private String intersectionName;   // otherName, e.g. "A74(M) J18 Ecclefechan"
    private String linkName;           // name/linkName, e.g. "M8"
    private String pointName;          // name/nonLinkedPointName, e.g. "A78 Skelmorlie"

    public boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    /** Best human readable name for this point. */
    public String getDisplayName() {
        if (intersectionName != null) return intersectionName;
        if (pointName != null) return pointName;
        StringBuilder sb = new StringBuilder();
        if (!roadNumbers.isEmpty()) sb.append(roadNumbers.get(0));
        else if (linkName != null) sb.append(linkName);
        if (junctionName != null) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(junctionName);
        }
        return sb.length() > 0 ? sb.toString() : "Unnamed point";
    }

    /** "Junction" / "Point on road" from the xsi:type. */
    public String getPointTypeLabel() {
        if (pointType == null) return "Point";
        if (pointType.contains("NonJunction")) return "Point on road";
        if (pointType.contains("Junction")) return "Junction";
        return TextUtil.humanise(pointType.replace("Tpeg", ""));
    }

    // ---- getters / setters ----
    public String getPointType() { return pointType; }
    public void setPointType(String pointType) { this.pointType = pointType; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getJunctionName() { return junctionName; }
    public void setJunctionName(String junctionName) { this.junctionName = junctionName; }
    public List<String> getRoadNumbers() { return roadNumbers; }
    public void addRoadNumber(String road) { if (road != null && !roadNumbers.contains(road)) roadNumbers.add(road); }
    public String getIntersectionName() { return intersectionName; }
    public void setIntersectionName(String intersectionName) { this.intersectionName = intersectionName; }
    public String getLinkName() { return linkName; }
    public void setLinkName(String linkName) { this.linkName = linkName; }
    public String getPointName() { return pointName; }
    public void setPointName(String pointName) { this.pointName = pointName; }
}
