//
// Name                 Cian MacDonald
// Student ID           S2426804
// Programme of Study   BSc (Hons) Software Engineering
//
package org.me.gcu.macdonald_cian_s2426804.viewmodel;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.macdonald_cian_s2426804.data.NetworkMonitor;
import org.me.gcu.macdonald_cian_s2426804.data.RefreshStatus;
import org.me.gcu.macdonald_cian_s2426804.data.TrafficRepository;
import org.me.gcu.macdonald_cian_s2426804.data.TrafficSnapshot;
import org.me.gcu.macdonald_cian_s2426804.model.FeedType;
import org.me.gcu.macdonald_cian_s2426804.model.Situation;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ViewModel shared by MainActivity and all of its fragments.
 *
 * It holds all UI state (selected situation, search text, nearby settings, current tab,
 * what the map shows) so that it survives screen rotation: when the device switches
 * between the portrait and landscape layouts the new views simply re-observe this state,
 * so the change is invisible to the user.
 *
 * Fragments never talk to the repository directly - only through this class.
 */
public class TrafficViewModel extends AndroidViewModel {

    public enum RoadworksFilter { ACTIVE_NOW, UPCOMING, ALL }

    public enum SearchScope { ALL, INCIDENTS, ROADWORKS }

    /** Default auto refresh interval. Can be shortened from the menu for demos. */
    public static final int DEFAULT_REFRESH_MINUTES = 60;

    private final TrafficRepository repository;
    private final NetworkMonitor networkMonitor;

    // navigation + selection
    private final MutableLiveData<Tab> currentTab = new MutableLiveData<>(Tab.INCIDENTS);
    private final MediatorLiveData<Situation> selected = new MediatorLiveData<>();
    private final MutableLiveData<Integer> refreshMinutes = new MutableLiveData<>(DEFAULT_REFRESH_MINUTES);

    // lists
    private final MutableLiveData<RoadworksFilter> roadworksFilter = new MutableLiveData<>(RoadworksFilter.ACTIVE_NOW);
    private final MediatorLiveData<List<Situation>> incidents = new MediatorLiveData<>();
    private final MediatorLiveData<List<Situation>> roadworks = new MediatorLiveData<>();

    // search by road
    private final MutableLiveData<String> roadQuery = new MutableLiveData<>("");
    private final MutableLiveData<SearchScope> searchScope = new MutableLiveData<>(SearchScope.ALL);
    private final MediatorLiveData<List<Situation>> roadResults = new MediatorLiveData<>();

    // search by distance
    private final MutableLiveData<NearbyQuery> nearbyQuery = new MutableLiveData<>(NearbyQuery.gcu(25));
    private final MediatorLiveData<List<Situation>> nearbyResults = new MediatorLiveData<>();
    private final Map<String, Double> nearbyDistances = new HashMap<>();

    // map
    private final MutableLiveData<MapContent.Mode> mapMode = new MutableLiveData<>(MapContent.Mode.ALL);
    private MapContent customMapSelection;
    private final MediatorLiveData<MapContent> mapContent = new MediatorLiveData<>();

    public TrafficViewModel(@NonNull Application application) {
        super(application);

        // Read the API keys from the manifest meta-data (values come from local.properties)
        Bundle meta = readMetaData(application);
        String pkg = application.getPackageName();
        String clientId = meta != null ? meta.getString(pkg + ".TS_CLIENT_ID") : null;
        String clientKey = meta != null ? meta.getString(pkg + ".TS_CLIENT_KEY") : null;

        repository = new TrafficRepository(application, clientId, clientKey);
        networkMonitor = new NetworkMonitor(application, repository::refreshNow);
        networkMonitor.start();

        LiveData<TrafficSnapshot> snapshot = repository.getSnapshot();

        incidents.addSource(snapshot, s -> incidents.setValue(buildIncidents()));
        roadworks.addSource(snapshot, s -> roadworks.setValue(buildRoadworks()));
        roadworks.addSource(roadworksFilter, f -> roadworks.setValue(buildRoadworks()));

        roadResults.addSource(snapshot, s -> roadResults.setValue(buildRoadResults()));
        roadResults.addSource(roadQuery, q -> roadResults.setValue(buildRoadResults()));
        roadResults.addSource(searchScope, q -> roadResults.setValue(buildRoadResults()));

        nearbyResults.addSource(snapshot, s -> nearbyResults.setValue(buildNearbyResults()));
        nearbyResults.addSource(nearbyQuery, q -> nearbyResults.setValue(buildNearbyResults()));

        mapContent.addSource(snapshot, s -> mapContent.setValue(buildMapContent()));
        mapContent.addSource(mapMode, m -> mapContent.setValue(buildMapContent()));

        // keep the selected situation pointing at the latest version after a refresh
        selected.addSource(snapshot, this::reselectAfterRefresh);

        // load data now and then every hour
        repository.startAutoRefresh(DEFAULT_REFRESH_MINUTES, true);
    }

    private static Bundle readMetaData(Application app) {
        try {
            ApplicationInfo info = app.getPackageManager()
                    .getApplicationInfo(app.getPackageName(), PackageManager.GET_META_DATA);
            return info.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    protected void onCleared() {
        networkMonitor.stop();
        repository.shutdown();
        super.onCleared();
    }

    // ================================================================== data + status

    public LiveData<TrafficSnapshot> getSnapshot() { return repository.getSnapshot(); }

    public LiveData<RefreshStatus> getStatus() { return repository.getStatus(); }

    public LiveData<Boolean> getOnline() { return networkMonitor.getOnline(); }

    public boolean hasApiKeys() { return repository.hasCredentials(); }

    public void refresh() { repository.refreshNow(); }

    public LiveData<Integer> getRefreshMinutes() { return refreshMinutes; }

    public void setRefreshMinutes(int minutes) {
        Integer current = refreshMinutes.getValue();
        if (current != null && current == minutes) return;
        refreshMinutes.setValue(minutes);
        repository.startAutoRefresh(minutes, false);
    }

    private TrafficSnapshot currentSnapshot() {
        TrafficSnapshot s = repository.getSnapshot().getValue();
        return s != null ? s : TrafficSnapshot.empty();
    }

    // ================================================================== navigation + selection

    public LiveData<Tab> getCurrentTab() { return currentTab; }

    public void setTab(Tab tab) {
        if (tab != currentTab.getValue()) currentTab.setValue(tab);
    }

    public LiveData<Situation> getSelected() { return selected; }

    public void select(Situation situation) { selected.setValue(situation); }

    public void clearSelection() { selected.setValue(null); }

    private void reselectAfterRefresh(TrafficSnapshot snapshot) {
        Situation old = selected.getValue();
        if (old == null || snapshot == null) return;
        Situation updated = snapshot.findByRecordId(old.getRecordId());
        if (updated != null && updated != old) selected.setValue(updated);
    }

    // ================================================================== incident + roadworks lists

    public LiveData<List<Situation>> getIncidents() { return incidents; }

    public LiveData<List<Situation>> getRoadworks() { return roadworks; }

    public LiveData<RoadworksFilter> getRoadworksFilter() { return roadworksFilter; }

    public void setRoadworksFilter(RoadworksFilter filter) {
        if (filter != roadworksFilter.getValue()) roadworksFilter.setValue(filter);
    }

    private List<Situation> buildIncidents() {
        List<Situation> list = new ArrayList<>(currentSnapshot().getIncidents());
        // newest / most recently updated first
        list.sort(Comparator.comparing(Situation::getVersionTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    private List<Situation> buildRoadworks() {
        OffsetDateTime now = OffsetDateTime.now();
        RoadworksFilter filter = roadworksFilter.getValue();
        List<Situation> list = new ArrayList<>();
        for (Situation s : currentSnapshot().getRoadworks()) {
            if (filter == RoadworksFilter.ACTIVE_NOW && !s.isActiveAt(now)) continue;
            if (filter == RoadworksFilter.UPCOMING && !s.getValidity().isUpcomingAt(now)) continue;
            list.add(s);
        }
        // worst delays first, then the works finishing soonest
        list.sort(Comparator
                .comparing((Situation s) -> s.getDelayLevel().ordinal(), Comparator.reverseOrder())
                .thenComparing(s -> s.getValidity().getOverallStart(), Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    // ================================================================== search by road

    public LiveData<String> getRoadQuery() { return roadQuery; }

    public void setRoadQuery(String query) {
        String q = query == null ? "" : query.trim();
        if (!q.equals(roadQuery.getValue())) roadQuery.setValue(q);
    }

    public LiveData<SearchScope> getSearchScope() { return searchScope; }

    public void setSearchScope(SearchScope scope) {
        if (scope != searchScope.getValue()) searchScope.setValue(scope);
    }

    public LiveData<List<Situation>> getRoadResults() { return roadResults; }

    private List<Situation> buildRoadResults() {
        String q = roadQuery.getValue();
        if (q == null || q.isEmpty()) return Collections.emptyList();
        SearchScope scope = searchScope.getValue();
        List<Situation> pool = new ArrayList<>();
        TrafficSnapshot snap = currentSnapshot();
        if (scope != SearchScope.ROADWORKS) pool.addAll(snap.getIncidents());
        if (scope != SearchScope.INCIDENTS) pool.addAll(snap.getRoadworks());

        List<Situation> results = new ArrayList<>();
        for (Situation s : pool) if (s.matchesRoadQuery(q)) results.add(s);
        // incidents before roadworks, then worst delays first
        results.sort(Comparator
                .comparing((Situation s) -> s.getFeedType().ordinal())
                .thenComparing(s -> s.getDelayLevel().ordinal(), Comparator.reverseOrder()));
        return results;
    }

    /** Road names present in the data, for search suggestions. */
    public List<String> getKnownRoads() { return currentSnapshot().getKnownRoads(); }

    // ================================================================== search by distance

    public LiveData<NearbyQuery> getNearbyQuery() { return nearbyQuery; }

    public LiveData<List<Situation>> getNearbyResults() { return nearbyResults; }

    public void setNearbyCentre(double lat, double lon, String label, boolean fromDevice) {
        NearbyQuery q = nearbyQuery.getValue();
        int radius = q != null ? q.radiusKm : 25;
        nearbyQuery.setValue(new NearbyQuery(lat, lon, label, radius, fromDevice));
    }

    public void setNearbyRadius(int km) {
        NearbyQuery q = nearbyQuery.getValue();
        if (q != null && q.radiusKm != km) nearbyQuery.setValue(q.withRadius(km));
    }

    /** Distance (km) of a situation from the nearby search centre, or null. */
    public Double getNearbyDistance(Situation s) {
        return s == null ? null : nearbyDistances.get(s.getRecordId());
    }

    private List<Situation> buildNearbyResults() {
        NearbyQuery q = nearbyQuery.getValue();
        nearbyDistances.clear();
        if (q == null) return Collections.emptyList();
        List<Situation> results = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (Situation s : currentSnapshot().getAll()) {
            if (!s.hasCoordinates()) continue;
            // ignore roadworks that haven't started yet or have finished
            if (s.getFeedType() == FeedType.ROADWORKS && !s.isActiveAt(now)) continue;
            double d = s.distanceKmFrom(q.latitude, q.longitude);
            if (d <= q.radiusKm) {
                nearbyDistances.put(s.getRecordId(), d);
                results.add(s);
            }
        }
        results.sort(Comparator.comparingDouble(s -> nearbyDistances.get(s.getRecordId())));
        return results;
    }

    // ================================================================== map

    public LiveData<MapContent> getMapContent() { return mapContent; }

    public LiveData<MapContent.Mode> getMapMode() { return mapMode; }

    public void setMapMode(MapContent.Mode mode) {
        if (mode == MapContent.Mode.SELECTION && customMapSelection == null) return;
        mapMode.setValue(mode);
    }

    public boolean hasCustomMapSelection() { return customMapSelection != null; }

    public String getCustomMapTitle() { return customMapSelection != null ? customMapSelection.title : null; }

    /** Show a chosen set of situations on the map (search results, one situation, ...). */
    public void showOnMap(List<Situation> situations, String title, boolean withNearbyCircle) {
        double[] centre = null;
        double radius = 0;
        NearbyQuery q = nearbyQuery.getValue();
        if (withNearbyCircle && q != null) {
            centre = new double[]{q.latitude, q.longitude};
            radius = q.radiusKm;
        }
        customMapSelection = new MapContent(MapContent.Mode.SELECTION, title, new ArrayList<>(situations), centre, radius);
        mapMode.setValue(MapContent.Mode.SELECTION);
        setTab(Tab.MAP);
    }

    private MapContent buildMapContent() {
        MapContent.Mode mode = mapMode.getValue();
        TrafficSnapshot snap = currentSnapshot();
        OffsetDateTime now = OffsetDateTime.now();
        if (mode == MapContent.Mode.SELECTION && customMapSelection != null) {
            // swap in refreshed versions of the same situations
            List<Situation> refreshed = new ArrayList<>();
            for (Situation s : customMapSelection.situations) {
                Situation latest = snap.findByRecordId(s.getRecordId());
                refreshed.add(latest != null ? latest : s);
            }
            return new MapContent(mode, customMapSelection.title, refreshed,
                    customMapSelection.centre, customMapSelection.radiusKm);
        }
        List<Situation> list = new ArrayList<>();
        if (mode != MapContent.Mode.ROADWORKS) list.addAll(snap.getIncidents());
        if (mode != MapContent.Mode.INCIDENTS) {
            for (Situation s : snap.getRoadworks()) if (s.isActiveAt(now)) list.add(s);
        }
        String title;
        if (mode == MapContent.Mode.INCIDENTS) title = "All incidents";
        else if (mode == MapContent.Mode.ROADWORKS) title = "Current roadworks";
        else title = "All incidents and current roadworks";
        return new MapContent(mode == null ? MapContent.Mode.ALL : mode, title, list, null, 0);
    }
}
