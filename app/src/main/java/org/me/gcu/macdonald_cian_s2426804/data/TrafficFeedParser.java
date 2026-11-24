//
// Name                 Cian MacDonald
// Student ID           S2426804
// Programme of Study   BSc (Hons) Software Engineering
//
package org.me.gcu.macdonald_cian_s2426804.data;

import org.me.gcu.macdonald_cian_s2426804.model.FeedType;
import org.me.gcu.macdonald_cian_s2426804.model.LinearLocation;
import org.me.gcu.macdonald_cian_s2426804.model.PointLocation;
import org.me.gcu.macdonald_cian_s2426804.model.Situation;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Parses a DATEX II SituationPublication (the String downloaded from Traffic Scotland)
 * with XmlPullParser and turns each &lt;situation&gt; into a {@link Situation} object.
 *
 * The parser keeps a stack of open element names so it knows the context of every
 * piece of text: for example a &lt;value&gt; can be the public comment, the source name,
 * or one of several kinds of name for a "from"/"to" point.
 * Elements it doesn't recognise are skipped, so new fields in the feed won't crash it.
 *
 * This class has no Android dependencies, so it can be unit tested on its own.
 */
public class TrafficFeedParser {

    /** Result of parsing one feed. */
    public static class Result {
        public final OffsetDateTime publicationTime;
        public final List<Situation> situations;
        public final int skippedRecords;

        Result(OffsetDateTime publicationTime, List<Situation> situations, int skippedRecords) {
            this.publicationTime = publicationTime;
            this.situations = situations;
            this.skippedRecords = skippedRecords;
        }
    }

    // element names that hold a "*Type" value but are NOT the situation's sub-type
    private static final String[] NOT_SUBTYPES = {
            "sourceType", "delaysType", "tpegLinearLocationType", "tpegJunctionPointDescriptorType",
            "tpegIlcPointDescriptorType", "tpegOtherPointDescriptorType"
    };

    private final FeedType feedType;

    // parsing state
    private final Deque<String> path = new ArrayDeque<>();
    private Situation current;
    private PointLocation currentPoint;
    private boolean currentPointIsSingle; // true when currentPoint is a stand-alone point location
    private String pendingName;      // value read inside a point's name, waiting for its descriptor type
    private OffsetDateTime publicationTime;
    private int skipped;

    public TrafficFeedParser(FeedType feedType) {
        this.feedType = feedType;
    }

    /** Parses the whole downloaded XML string. */
    public Result parse(String xml) throws XmlPullParserException, IOException {
        List<Situation> situations = new ArrayList<>();
        path.clear();
        current = null;
        currentPoint = null;
        publicationTime = null;
        skipped = 0;

        if (xml == null || xml.trim().isEmpty()) {
            return new Result(null, situations, 0);
        }
        // Clean up any leading garbage characters before the XML declaration (as in the starter code)
        int start = xml.indexOf("<?");
        if (start > 0) xml = xml.substring(start);

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(xml));

        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                handleStartTag(xpp);
            } else if (eventType == XmlPullParser.TEXT) {
                String text = xpp.getText();
                if (text != null) {
                    text = text.trim();
                    if (!text.isEmpty() && !path.isEmpty()) handleText(path.peek(), text);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                handleEndTag(xpp.getName(), situations);
            }
            eventType = xpp.next();
        }
        return new Result(publicationTime, situations, skipped);
    }

    // ------------------------------------------------------------------ start tags

    private void handleStartTag(XmlPullParser xpp) {
        String name = xpp.getName();
        path.push(name);

        switch (name) {
            case "situation":
                current = new Situation();
                current.setFeedType(feedType);
                current.setSituationId(attribute(xpp, "id"));
                break;
            case "situationRecord":
                if (current != null) {
                    current.setRecordType(attribute(xpp, "type"));   // xsi:type, e.g. "AbnormalTraffic"
                    current.setRecordId(attribute(xpp, "id"));
                    current.setRecordVersion(parseInt(attribute(xpp, "version"), 0));
                }
                break;
            case "groupOfLocations":
                if (current != null) current.getLocation().setLocationGroupType(attribute(xpp, "type"));
                break;
            case "from":
            case "to":
                if (current != null) {
                    currentPoint = new PointLocation();
                    currentPoint.setPointType(attribute(xpp, "type"));
                    if (name.equals("from")) current.getLocation().setFrom(currentPoint);
                    else current.getLocation().setTo(currentPoint);
                }
                break;
            case "pointCoordinates":
                // coordinates outside a from/to (a point location) get their own point
                if (current != null && currentPoint == null) {
                    currentPoint = new PointLocation();
                    currentPoint.setPointType("Point");
                    currentPointIsSingle = true;
                    current.getLocation().setSinglePoint(currentPoint);
                }
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ text

    private void handleText(String tag, String text) {
        if (tag.equals("publicationTime")) {
            publicationTime = parseDate(text);
            return;
        }
        if (current == null) return;   // text outside a situation (exchange header etc.)

        switch (tag) {
            // header
            case "confidentiality": current.setConfidentiality(text); break;
            case "informationStatus": current.setInformationStatus(text); break;

            // record metadata
            case "situationRecordCreationTime": current.setCreationTime(parseDate(text)); break;
            case "situationRecordVersionTime": current.setVersionTime(parseDate(text)); break;
            case "probabilityOfOccurrence": current.setProbabilityOfOccurrence(text); break;
            case "sourceIdentification": current.setSourceIdentification(text); break;
            case "sourceType": current.setSourceType(text); break;
            case "complianceOption": current.setComplianceOption(text); break;
            case "urgentRoadworks": current.setUrgentRoadworks(Boolean.parseBoolean(text)); break;

            // validity
            case "validityStatus": current.getValidity().setValidityStatus(text); break;
            case "overallStartTime": current.getValidity().setOverallStart(parseDate(text)); break;
            case "overallEndTime": current.getValidity().setOverallEnd(parseDate(text)); break;

            // impact
            case "numberOfLanesRestricted": current.getImpact().setNumberOfLanesRestricted(parseInt(text, null)); break;
            case "numberOfOperationalLanes": current.getImpact().setNumberOfOperationalLanes(parseInt(text, null)); break;
            case "delaysType": current.getImpact().setDelaysType(text); break;
            case "delayTimeValue": current.getImpact().setDelayTimeSeconds(parseInt(text, null)); break;

            // location
            case "tpegDirection": current.getLocation().setDirection(text); break;
            case "tpegLinearLocationType": current.getLocation().setLinearType(text); break;
            case "latitude": if (currentPoint != null) currentPoint.setLatitude(parseDouble(text)); break;
            case "longitude": if (currentPoint != null) currentPoint.setLongitude(parseDouble(text)); break;
            case "tpegJunctionPointDescriptorType":
            case "tpegIlcPointDescriptorType":
            case "tpegOtherPointDescriptorType":
                applyPointName(text);
                break;

            case "value":
                handleValue(text);
                break;

            default:
                // any other "...Type" element directly inside situationRecord is the record's
                // sub-type (roadMaintenanceType, abnormalTrafficType, vehicleObstructionType, ...)
                if (tag.endsWith("Type") && "situationRecord".equals(parentTag()) && !isExcluded(tag)) {
                    current.setSubType(text);
                }
                break;
        }
    }

    /** A &lt;value&gt; means different things depending on where it is. */
    private void handleValue(String text) {
        if (inside("generalPublicComment")) {
            // a comment may (rarely) be split over several values - join them
            String existing = current.getDescription();
            current.setDescription(existing == null ? text : existing + " " + text);
        } else if (inside("sourceName")) {
            current.setSourceName(text);
        } else if (currentPoint != null && (inside("name") || inside("ilc") || inside("otherName"))) {
            // the descriptor type comes AFTER the value, so hold on to it until then
            pendingName = text;
        }
    }

    /** Store the pending point name now that we know what kind of name it is. */
    private void applyPointName(String descriptorType) {
        if (currentPoint == null || pendingName == null) return;
        switch (descriptorType) {
            case "junctionName": currentPoint.setJunctionName(pendingName); break;
            case "linkName": currentPoint.setLinkName(pendingName); break;
            case "nonLinkedPointName": currentPoint.setPointName(pendingName); break;
            case "intersectionName": currentPoint.setIntersectionName(pendingName); break;
            default:
                if (descriptorType.startsWith("tpegIlcName")) currentPoint.addRoadNumber(pendingName);
                else if (currentPoint.getPointName() == null) currentPoint.setPointName(pendingName);
                break;
        }
        pendingName = null;
    }

    // ------------------------------------------------------------------ end tags

    private void handleEndTag(String name, List<Situation> out) {
        if (!path.isEmpty()) path.pop();
        switch (name) {
            case "from":
            case "to":
                currentPoint = null;
                break;
            case "pointCoordinates":
                if (currentPointIsSingle) {
                    currentPoint = null;
                    currentPointIsSingle = false;
                }
                break;
            case "situation":
                if (current != null) {
                    if (current.getDescription() != null || current.hasCoordinates()) {
                        current.finishParsing();
                        out.add(current);
                    } else {
                        skipped++;   // nothing useful to show
                    }
                }
                current = null;
                currentPoint = null;
                currentPointIsSingle = false;
                pendingName = null;
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Reads an attribute by local name (works for both "type" and "xsi:type"). */
    private static String attribute(XmlPullParser xpp, String localName) {
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            String n = xpp.getAttributeName(i);
            if (n.equals(localName) || n.endsWith(":" + localName)) return xpp.getAttributeValue(i);
        }
        return null;
    }

    private boolean inside(String tag) {
        return path.contains(tag);
    }

    private String parentTag() {
        Iterator<String> it = path.iterator();
        if (!it.hasNext()) return null;
        it.next();                       // the current tag
        return it.hasNext() ? it.next() : null;
    }

    private static boolean isExcluded(String tag) {
        for (String s : NOT_SUBTYPES) if (s.equals(tag)) return true;
        return false;
    }

    static OffsetDateTime parseDate(String text) {
        try {
            return OffsetDateTime.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Integer parseInt(String text, Integer fallback) {
        if (text == null) return fallback;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double parseDouble(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
