package org.me.gcu.macdonald_cian_s2426804.data;

import org.me.gcu.macdonald_cian_s2426804.model.FeedType;
import org.me.gcu.macdonald_cian_s2426804.model.Situation;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * Everything the app knows at one moment: the parsed situations from both feeds and
 * where each feed's data came from. Immutable, so it can be passed safely from the
 * background thread to the UI thread through LiveData.
 */
public class TrafficSnapshot {

    private final List<Situation> incidents;
    private final List<Situation> roadworks;
    private final DataSource incidentsSource;
    private final DataSource roadworksSource;
    private final OffsetDateTime incidentsPublished;
    private final OffsetDateTime roadworksPublished;
    private final long dataTimeMillis;       // when the data was downloaded (or cached)
    private final List<String> knownRoads;

    public TrafficSnapshot(List<Situation> incidents, DataSource incidentsSource, OffsetDateTime incidentsPublished,
                           List<Situation> roadworks, DataSource roadworksSource, OffsetDateTime roadworksPublished,
                           long dataTimeMillis) {
        this.incidents = Collections.unmodifiableList(new ArrayList<>(incidents));
        this.roadworks = Collections.unmodifiableList(new ArrayList<>(roadworks));
        this.incidentsSource = incidentsSource;
        this.roadworksSource = roadworksSource;
        this.incidentsPublished = incidentsPublished;
        this.roadworksPublished = roadworksPublished;
        this.dataTimeMillis = dataTimeMillis;

        TreeSet<String> roads = new TreeSet<>(TrafficSnapshot::compareRoads);
        for (Situation s : this.incidents) roads.addAll(s.getRoadList());
        for (Situation s : this.roadworks) roads.addAll(s.getRoadList());
        this.knownRoads = Collections.unmodifiableList(new ArrayList<>(roads));
    }

    public static TrafficSnapshot empty() {
        return new TrafficSnapshot(new ArrayList<>(), DataSource.SAMPLE, null,
                new ArrayList<>(), DataSource.SAMPLE, null, 0);
    }

    /** Sort roads M first, then A, then B, then by number: M8, M74, A1, A9, A82, B957. */
    private static int compareRoads(String a, String b) {
        int ca = "MAB".indexOf(a.charAt(0)), cb = "MAB".indexOf(b.charAt(0));
        if (ca != cb) return Integer.compare(ca, cb);
        int na = leadingNumber(a), nb = leadingNumber(b);
        if (na != nb) return Integer.compare(na, nb);
        return a.compareTo(b);
    }

    private static int leadingNumber(String road) {
        int n = 0;
        for (int i = 1; i < road.length() && Character.isDigit(road.charAt(i)); i++) {
            n = n * 10 + (road.charAt(i) - '0');
        }
        return n;
    }

    public List<Situation> getAll() {
        List<Situation> all = new ArrayList<>(incidents);
        all.addAll(roadworks);
        return all;
    }

    public List<Situation> get(FeedType feed) { return feed == FeedType.INCIDENT ? incidents : roadworks; }

    public Situation findByRecordId(String recordId) {
        if (recordId == null) return null;
        for (Situation s : incidents) if (recordId.equals(s.getRecordId())) return s;
        for (Situation s : roadworks) if (recordId.equals(s.getRecordId())) return s;
        return null;
    }

    /** The "worst" source across both feeds, for the status line. */
    public DataSource getOverallSource() {
        if (incidentsSource == DataSource.SAMPLE || roadworksSource == DataSource.SAMPLE) return DataSource.SAMPLE;
        if (incidentsSource == DataSource.CACHE || roadworksSource == DataSource.CACHE) return DataSource.CACHE;
        return DataSource.LIVE;
    }

    public boolean isEmpty() { return incidents.isEmpty() && roadworks.isEmpty(); }

    public List<Situation> getIncidents() { return incidents; }
    public List<Situation> getRoadworks() { return roadworks; }
    public DataSource getIncidentsSource() { return incidentsSource; }
    public DataSource getRoadworksSource() { return roadworksSource; }
    public OffsetDateTime getIncidentsPublished() { return incidentsPublished; }
    public OffsetDateTime getRoadworksPublished() { return roadworksPublished; }
    public long getDataTimeMillis() { return dataTimeMillis; }
    public List<String> getKnownRoads() { return knownRoads; }
}
