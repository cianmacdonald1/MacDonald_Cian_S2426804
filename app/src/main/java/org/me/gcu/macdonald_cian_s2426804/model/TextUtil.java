package org.me.gcu.macdonald_cian_s2426804.model;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String helpers used when breaking the feed's text down into parts:
 * turning camelCase codes into readable labels, pulling road numbers out of
 * free text, and expanding the abbreviations Traffic Scotland uses.
 */
public final class TextUtil {

    private TextUtil() { }

    /** Road numbers such as M8, A74(M), A74M(S), A823(M), B957. */
    private static final Pattern ROAD_PATTERN =
            Pattern.compile("\\b([AMB][0-9]{1,4}(?:\\([MS]\\)|M(?:\\(S\\))?)?)(?![0-9A-Za-z])");

    /** "queuingTraffic" -> "Queuing traffic", "southBound" -> "Southbound". */
    public static String humanise(String code) {
        if (code == null || code.isEmpty()) return "";
        switch (code) {
            case "northBound": return "Northbound";
            case "southBound": return "Southbound";
            case "eastBound": return "Eastbound";
            case "westBound": return "Westbound";
            case "bothWays": return "Both directions";
            case "definedByValidityTimeSpec": return "Defined by time period";
            case "noRestriction": return "No restriction";
            case "roadAuthorities": return "Road authorities";
            case "RoadOrCarriagewayOrLaneManagement": return "Road / lane management";
            default: break;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(code.charAt(i - 1))) {
                sb.append(' ');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(i == 0 ? Character.toUpperCase(c) : c);
            }
        }
        return sb.toString();
    }

    /** All road numbers found in a piece of free text, in the order found. */
    public static Set<String> extractRoads(String text) {
        Set<String> roads = new LinkedHashSet<>();
        if (text == null) return roads;
        Matcher m = ROAD_PATTERN.matcher(text);
        while (m.find()) roads.add(normaliseRoad(m.group(1)));
        return roads;
    }

    /** First road number in some text, or null. */
    public static String firstRoad(String text) {
        Set<String> roads = extractRoads(text);
        return roads.isEmpty() ? null : roads.iterator().next();
    }

    /** Puts road numbers in one consistent form so "a74m" and "A74(M)" match. */
    public static String normaliseRoad(String road) {
        if (road == null) return null;
        String r = road.trim().toUpperCase(Locale.UK).replace(" ", "");
        // A74M(S) and A74(M) both describe the A74(M) motorway
        if (r.matches("[AB][0-9]+M(\\(S\\))?")) r = r.substring(0, r.indexOf('M', 1)) + "(M)";
        return r;
    }

    /** Short direction codes used in roadworks descriptions -> full words. */
    public static String expandDirection(String token) {
        if (token == null) return null;
        String t = token.toUpperCase(Locale.UK).replace("/", "").replace(".", "").trim();
        switch (t) {
            case "NB": case "N": case "NORTHBOUND": return "Northbound";
            case "SB": case "S": case "SOUTHBOUND": return "Southbound";
            case "EB": case "E": case "EASTBOUND": return "Eastbound";
            case "WB": case "W": case "WESTBOUND": return "Westbound";
            case "BOTH": case "BOTH DIRECTIONS": case "BOTH WAYS": return "Both directions";
            default: return null;
        }
    }

    /** Expands Traffic Scotland abbreviations for display. */
    public static String expandAbbreviations(String text) {
        if (text == null) return null;
        return text
                .replaceAll("(?i)\\bTTL'?s\\b|\\bTTL\\b", "temporary traffic lights")
                .replaceAll("\\bMVMS\\b", "mobile variable message sign")
                .replaceAll("\\bHS\\b", "hard shoulder")
                .replaceAll("\\bJct\\b", "junction")
                .replaceAll("\\bMLC'?s\\b", "mobile lane closures");
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
