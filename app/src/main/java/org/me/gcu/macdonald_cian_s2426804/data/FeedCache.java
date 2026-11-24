package org.me.gcu.macdonald_cian_s2426804.data;

import android.content.Context;

import org.me.gcu.macdonald_cian_s2426804.model.FeedType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Keeps the last successfully downloaded XML for each feed on the device, so the app
 * can still show the most recent data when there's no internet connection.
 * Also loads the bundled sample feeds (assets) for a first run with no keys and no cache.
 */
public class FeedCache {

    /** XML plus when it was saved. */
    public static class Cached {
        public final String xml;
        public final long savedAtMillis;

        Cached(String xml, long savedAtMillis) {
            this.xml = xml;
            this.savedAtMillis = savedAtMillis;
        }
    }

    private final Context context;

    public FeedCache(Context context) {
        this.context = context.getApplicationContext();
    }

    private File fileFor(FeedType feed) {
        return new File(context.getFilesDir(), "feed_" + feed.name().toLowerCase() + ".xml");
    }

    public void save(FeedType feed, String xml) {
        File target = fileFor(feed);
        File temp = new File(target.getPath() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write(xml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            return;   // caching is best effort
        }
        //noinspection ResultOfMethodCallIgnored
        temp.renameTo(target);
    }

    public Cached load(FeedType feed) {
        File f = fileFor(feed);
        if (!f.exists()) return null;
        try (InputStream in = new FileInputStream(f)) {
            return new Cached(readAll(in), f.lastModified());
        } catch (IOException e) {
            return null;
        }
    }

    /** Sample feed shipped in assets (copies of the module's example data). */
    public String loadSample(FeedType feed) {
        String name = feed == FeedType.INCIDENT ? "sample_unplanned.xml" : "sample_roadworks.xml";
        try (InputStream in = context.getAssets().open(name)) {
            return readAll(in);
        } catch (IOException e) {
            return null;
        }
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[16 * 1024];
        int n;
        while ((n = in.read(chunk)) != -1) buffer.write(chunk, 0, n);
        return buffer.toString("UTF-8");
    }
}
