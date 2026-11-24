package org.me.gcu.macdonald_cian_s2426804.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One "situation" from either feed (an incident or a set of roadworks), with the
 * contents of its situationRecord broken down into typed fields and sub-objects.
 */
public class Situation {

    // identity
    private String situationId;
    private String recordId;
    private int recordVersion;
    private FeedType feedType;

    // header
    private String confidentiality;
    private String informationStatus;

    // situationRecord
    private String recordType;        // xsi:type, e.g. AbnormalTraffic
    private String subType;           // e.g. queuingTraffic, roadClosed, roadworks
    private OffsetDateTime creationTime;
    private OffsetDateTime versionTime;
    private String probabilityOfOccurrence;
    private String sourceIdentification;
    private String sourceName;
    private String sourceType;
    private String complianceOption;
    private Boolean urgentRoadworks;
    private String description;       // generalPublicComment

    private final Validity validity = new Validity();
    private final Impact impact = new Impact();
    private final LinearLocation location = new LinearLocation();

    // derived after parsing
    private SituationCategory category = SituationCategory.OTHER;
    private CommentDetails commentDetails;
    private final Set<String> roads = new LinkedHashSet<>();

    /** Called by the parser once the whole situation has been read. */
    public void finishParsing() {
        commentDetails = CommentDetails.parse(description);
        category = SituationCategory.classify(recordType, subType, description, feedType);

        roads.clear();
        roads.addAll(TextUtil.extractRoads(description));
        addRoadsFrom(location.getFrom());
        addRoadsFrom(location.getTo());
        addRoadsFrom(location.getSinglePoint());
    }

    private void addRoadsFrom(PointLocation p) {
        if (p == null) return;
        for (String r : p.getRoadNumbers()) roads.add(TextUtil.normaliseRoad(r));
        if (p.getLinkName() != null) roads.addAll(TextUtil.extractRoads(p.getLinkName()));
    }

    // ---------- behaviour used by the UI / ViewModel ----------

    /** Main road for this situation (first one mentioned). */
    public String getPrimaryRoad() {
        if (commentDetails != null && commentDetails.getRoad() != null) return commentDetails.getRoad();
        return roads.isEmpty() ? null : roads.iterator().next();
    }

    /** Does this situation match a road-name search? */
    public boolean matchesRoadQuery(String query) {
        if (TextUtil.isBlank(query)) return false;
        String q = query.trim();
        String asRoad = TextUtil.normaliseRoad(q);
        // exact road number match (so "M8" doesn't match "M80")
        if (asRoad.matches("[AMB][0-9]{1,4}(\\(M\\))?")) {
            return roads.contains(asRoad);
        }
        // otherwise a place / street name search over the text
        String lower = q.toLowerCase(Locale.UK);
        if (description != null && description.toLowerCase(Locale.UK).contains(lower)) return true;
        for (PointLocation p : location.getPoints()) {
            if (p.getDisplayName().toLowerCase(Locale.UK).contains(lower)) return true;
        }
        return false;
    }

    /** Distance from a point to the nearest part of this situation, in km. */
    public double distanceKmFrom(double lat, double lon) {
        double best = Double.MAX_VALUE;
        for (PointLocation p : location.getPoints()) {
            best = Math.min(best, GeoUtil.distanceKm(lat, lon, p.getLatitude(), p.getLongitude()));
        }
        double[] c = location.getCentre();
        if (c != null) best = Math.min(best, GeoUtil.distanceKm(lat, lon, c[0], c[1]));
        return best;
    }

    public boolean hasCoordinates() { return !location.getPoints().isEmpty(); }

    public DelayLevel getDelayLevel() { return impact.getDelayLevel(); }

    public boolean isActiveAt(OffsetDateTime now) { return validity.isActiveAt(now); }

    /** Short label for list rows, e.g. "Queuing traffic" or "Roadworks – closure". */
    public String getTypeLabel() { return category.getLabel(); }

    public String getRecordTypeLabel() { return TextUtil.humanise(recordType); }

    public String getSubTypeLabel() { return TextUtil.humanise(subType); }

    public List<String> getRoadList() { return Collections.unmodifiableList(new ArrayList<>(roads)); }

    // ---------- getters / setters ----------
    public String getSituationId() { return situationId; }
    public void setSituationId(String situationId) { this.situationId = situationId; }
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public int getRecordVersion() { return recordVersion; }
    public void setRecordVersion(int recordVersion) { this.recordVersion = recordVersion; }
    public FeedType getFeedType() { return feedType; }
    public void setFeedType(FeedType feedType) { this.feedType = feedType; }
    public String getConfidentiality() { return confidentiality; }
    public void setConfidentiality(String confidentiality) { this.confidentiality = confidentiality; }
    public String getInformationStatus() { return informationStatus; }
    public void setInformationStatus(String informationStatus) { this.informationStatus = informationStatus; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }
    public OffsetDateTime getCreationTime() { return creationTime; }
    public void setCreationTime(OffsetDateTime creationTime) { this.creationTime = creationTime; }
    public OffsetDateTime getVersionTime() { return versionTime; }
    public void setVersionTime(OffsetDateTime versionTime) { this.versionTime = versionTime; }
    public String getProbabilityOfOccurrence() { return probabilityOfOccurrence; }
    public void setProbabilityOfOccurrence(String p) { this.probabilityOfOccurrence = p; }
    public String getSourceIdentification() { return sourceIdentification; }
    public void setSourceIdentification(String s) { this.sourceIdentification = s; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getComplianceOption() { return complianceOption; }
    public void setComplianceOption(String complianceOption) { this.complianceOption = complianceOption; }
    public Boolean getUrgentRoadworks() { return urgentRoadworks; }
    public void setUrgentRoadworks(Boolean urgentRoadworks) { this.urgentRoadworks = urgentRoadworks; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Validity getValidity() { return validity; }
    public Impact getImpact() { return impact; }
    public LinearLocation getLocation() { return location; }
    public SituationCategory getCategory() { return category; }
    public CommentDetails getCommentDetails() { return commentDetails; }
}
