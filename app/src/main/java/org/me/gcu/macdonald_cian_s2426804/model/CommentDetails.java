package org.me.gcu.macdonald_cian_s2426804.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The generalPublicComment text broken down into its parts.
 *
 * Roadworks comments follow a loose "ROAD DIR Place - Place - Works" format, e.g.
 *   "M8 WB Woodside - A804 Phoenix Rd - Closure"
 *   "A702 SB - TTL's - Causewayend"
 * Incident comments are sentences, e.g.
 *   "2 lanes restricted Eastbound"
 *   "The A78 is closed in both directions between X and Y due to emergency roadworks."
 */
public class CommentDetails {

    private static final Pattern DIRECTION_TOKEN = Pattern.compile(
            "(?i)(?<![A-Za-z])(NB|SB|EB|WB|N/B|S/B|E/B|W/B|northbound|southbound|eastbound|westbound|both directions)(?![A-Za-z])");
    private static final Pattern LANES = Pattern.compile("(?i)(\\d+)\\s+lanes?\\s+(restricted|closed)");
    private static final Pattern DUE_TO = Pattern.compile("(?i)\\b(?:due to|to allow for|following)\\s+([^.]+)");
    private static final Pattern BETWEEN = Pattern.compile("(?i)\\bbetween\\s+(.+?)\\s+and\\s+(.+?)(?:\\s+due|\\s+to allow|\\s*-|,|\\.|$)");
    private static final Pattern WORKS_WORDS = Pattern.compile(
            "(?i)closure|ttl|traffic lights|stop and go|mlc|lane|verge|resurfacing|works|mvms|diversion|barrier|inspection|surfacing|patching|drainage|lighting|signs?|convoy|contraflow");

    private final String original;
    private String road;
    private String direction;
    private String place;
    private String worksDescription;
    private String cause;
    private String between;
    private Integer lanesAffected;

    private CommentDetails(String original) { this.original = original; }

    public static CommentDetails parse(String comment) {
        CommentDetails d = new CommentDetails(comment);
        if (TextUtil.isBlank(comment)) return d;
        String text = comment.trim();

        d.road = TextUtil.firstRoad(text);

        Matcher dir = DIRECTION_TOKEN.matcher(text);
        if (dir.find()) d.direction = TextUtil.expandDirection(dir.group(1));

        Matcher lanes = LANES.matcher(text);
        if (lanes.find()) d.lanesAffected = Integer.parseInt(lanes.group(1));

        Matcher due = DUE_TO.matcher(text);
        if (due.find()) d.cause = due.group(1).trim();

        Matcher between = BETWEEN.matcher(text);
        if (between.find()) d.between = between.group(1).trim() + " and " + between.group(2).trim();

        // Roadworks style: split on " - " into place / works parts
        String[] parts = text.split("\\s+-\\s*|\\s*-\\s+");
        if (parts.length >= 2) {
            StringBuilder places = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                // only the first part starts with this situation's road number;
                // later parts can name other roads (e.g. "A804 Phoenix Rd") which must be kept
                String part = stripRoadAndDirection(parts[i].trim(), i == 0 ? d.road : null);
                if (part.isEmpty()) continue;
                if (d.worksDescription == null && WORKS_WORDS.matcher(part).find()) {
                    d.worksDescription = TextUtil.expandAbbreviations(part);
                } else {
                    if (places.length() > 0) places.append(" – ");
                    places.append(part);
                }
            }
            if (places.length() > 0) d.place = places.toString();
        } else if (d.road != null && !text.toLowerCase().startsWith("the ")
                && !Character.isDigit(text.charAt(0)) && text.length() < 80) {
            // short roadworks text without dashes, e.g. "A77 SB Ballantrae - TTLs" handled above,
            // "A725 SB Crossbaskets Lane Closure" handled here
            String rest = stripRoadAndDirection(text, d.road);
            Matcher works = WORKS_WORDS.matcher(rest);
            if (works.find() && works.start() > 0) {
                d.place = rest.substring(0, works.start()).trim();
                d.worksDescription = TextUtil.expandAbbreviations(rest.substring(works.start()).trim());
            } else {
                d.place = rest;
            }
        }
        return d;
    }

    private static String stripRoadAndDirection(String part, String road) {
        String s = part;
        if (road != null) {
            // remove the leading road number in whatever form it was written
            s = s.replaceFirst("^(?i)(the\\s+)?[AMB][0-9]{1,4}(\\([MS]\\)|M(\\(S\\))?)?\\b", "").trim();
        }
        s = DIRECTION_TOKEN.matcher(s).replaceFirst("").trim();
        s = s.replaceAll("^[-–,\\s]+|[-–,\\s]+$", "");
        return s;
    }

    public boolean hasAnyDetail() {
        return road != null || direction != null || place != null || worksDescription != null
                || cause != null || between != null || lanesAffected != null;
    }

    // ---- getters ----
    public String getOriginal() { return original; }
    public String getRoad() { return road; }
    public String getDirection() { return direction; }
    public String getPlace() { return place; }
    public String getWorksDescription() { return worksDescription; }
    public String getCause() { return cause; }
    public String getBetween() { return between; }
    public Integer getLanesAffected() { return lanesAffected; }
}
