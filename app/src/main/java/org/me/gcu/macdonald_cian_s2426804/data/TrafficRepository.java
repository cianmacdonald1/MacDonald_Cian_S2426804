//
// Name                 Cian MacDonald
// Student ID           S2426804
// Programme of Study   BSc (Hons) Software Engineering
//
package org.me.gcu.macdonald_cian_s2426804.data;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.macdonald_cian_s2426804.model.FeedType;
import org.me.gcu.macdonald_cian_s2426804.model.Situation;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The single source of truth for traffic data (the "Model" side of MVVM).
 *
 * Threading: all downloading and parsing runs on one background worker thread
 * (a single-thread ScheduledExecutorService). Results are handed to the UI with
 * LiveData.postValue(), which delivers them on the main thread - this replaces the
 * starter code's runOnUiThread(). Because there is only one worker thread, a manual
 * refresh and an automatic refresh can never run at the same time.
 *
 * Auto update: the same executor runs the refresh on a fixed schedule
 * (every hour by default, shorter for demos).
 */
public class TrafficRepository {

    private static final String TAG = "TrafficRepository";

    private final FeedDownloader downloader;
    private final FeedCache cache;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "traffic-worker");
        t.setDaemon(true);
        return t;
    });

    private final MutableLiveData<TrafficSnapshot> snapshot = new MutableLiveData<>();
    private final MutableLiveData<RefreshStatus> status = new MutableLiveData<>();

    private ScheduledFuture<?> autoRefreshTask;
    private volatile long intervalMinutes;
    private volatile long nextRefreshMillis;
    private volatile long lastSuccessMillis;
    private boolean initialLoadDone;     // only touched on the worker thread

    public TrafficRepository(Context context, String clientId, String clientKey) {
        this.downloader = new FeedDownloader(clientId, clientKey);
        this.cache = new FeedCache(context);
    }

    public LiveData<TrafficSnapshot> getSnapshot() { return snapshot; }

    public LiveData<RefreshStatus> getStatus() { return status; }

    public boolean hasCredentials() { return downloader.hasCredentials(); }

    /**
     * Starts (or restarts) automatic refreshing. Runs straight away and then every
     * {@code minutes} minutes.
     */
    public synchronized void startAutoRefresh(long minutes, boolean refreshNow) {
        if (autoRefreshTask != null) autoRefreshTask.cancel(false);
        intervalMinutes = minutes;
        long initialDelay = refreshNow ? 0 : minutes;
        nextRefreshMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(initialDelay);
        autoRefreshTask = worker.scheduleWithFixedDelay(this::refreshOnWorker, initialDelay, minutes, TimeUnit.MINUTES);
    }

    /** Manual refresh (toolbar button, connection restored). */
    public void refreshNow() {
        worker.execute(this::refreshOnWorker);
    }

    public void shutdown() {
        worker.shutdownNow();
    }

    // ------------------------------------------------------------------ worker thread

    private void refreshOnWorker() {
        nextRefreshMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(intervalMinutes);

        // First run: show the saved copy straight away so the user isn't looking at an empty screen
        if (!initialLoadDone) {
            initialLoadDone = true;
            TrafficSnapshot offline = buildOfflineSnapshot();
            if (offline != null) snapshot.postValue(offline);
        }

        status.postValue(new RefreshStatus(RefreshStatus.State.LOADING, "Updating…", lastSuccessMillis, nextRefreshMillis));

        FeedLoad incidents = loadFeed(FeedType.INCIDENT);
        FeedLoad roadworks = loadFeed(FeedType.ROADWORKS);

        TrafficSnapshot result = new TrafficSnapshot(
                incidents.situations, incidents.source, incidents.published,
                roadworks.situations, roadworks.source, roadworks.published,
                Math.min(incidents.dataTime, roadworks.dataTime));
        snapshot.postValue(result);

        if (incidents.error == null && roadworks.error == null) {
            lastSuccessMillis = System.currentTimeMillis();
            status.postValue(new RefreshStatus(RefreshStatus.State.SUCCESS,
                    "Updated", lastSuccessMillis, nextRefreshMillis));
        } else {
            String error = incidents.error != null ? incidents.error : roadworks.error;
            status.postValue(new RefreshStatus(RefreshStatus.State.FAILED, error, lastSuccessMillis, nextRefreshMillis));
        }
    }

    /** Result of loading one feed from wherever it was available. */
    private static class FeedLoad {
        List<Situation> situations = new ArrayList<>();
        DataSource source = DataSource.SAMPLE;
        OffsetDateTime published;
        long dataTime;
        String error;
    }

    /** Download -> parse -> cache. Falls back to the saved copy, then the bundled sample. */
    private FeedLoad loadFeed(FeedType feed) {
        FeedLoad load = new FeedLoad();
        try {
            String xml = downloader.download(feed);
            TrafficFeedParser.Result parsed = new TrafficFeedParser(feed).parse(xml);
            cache.save(feed, xml);        // only save XML that parsed
            load.situations = parsed.situations;
            load.published = parsed.publicationTime;
            load.source = DataSource.LIVE;
            load.dataTime = System.currentTimeMillis();
            Log.d(TAG, feed + ": " + parsed.situations.size() + " situations downloaded");
            return load;
        } catch (FeedDownloader.DownloadException e) {
            load.error = e.getMessage();
            Log.w(TAG, feed + " download failed: " + e.getMessage());
        } catch (Exception e) {      // XmlPullParserException / IOException while parsing
            load.error = "The " + feed.getLabel().toLowerCase() + " feed couldn't be read";
            Log.e(TAG, "Parsing " + feed + " failed", e);
        }
        fillFromOffline(feed, load);
        return load;
    }

    private void fillFromOffline(FeedType feed, FeedLoad load) {
        FeedCache.Cached cached = cache.load(feed);
        if (cached != null && tryParse(feed, cached.xml, load)) {
            load.source = DataSource.CACHE;
            load.dataTime = cached.savedAtMillis;
            return;
        }
        String sample = cache.loadSample(feed);
        if (sample != null && tryParse(feed, sample, load)) {
            load.source = DataSource.SAMPLE;
            load.dataTime = 0;
        }
    }

    private boolean tryParse(FeedType feed, String xml, FeedLoad into) {
        try {
            TrafficFeedParser.Result parsed = new TrafficFeedParser(feed).parse(xml);
            into.situations = parsed.situations;
            into.published = parsed.publicationTime;
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Couldn't parse offline copy of " + feed, e);
            return false;
        }
    }

    private TrafficSnapshot buildOfflineSnapshot() {
        FeedLoad incidents = new FeedLoad();
        FeedLoad roadworks = new FeedLoad();
        fillFromOffline(FeedType.INCIDENT, incidents);
        fillFromOffline(FeedType.ROADWORKS, roadworks);
        if (incidents.situations.isEmpty() && roadworks.situations.isEmpty()) return null;
        return new TrafficSnapshot(incidents.situations, incidents.source, incidents.published,
                roadworks.situations, roadworks.source, roadworks.published,
                Math.min(incidents.dataTime, roadworks.dataTime));
    }
}
