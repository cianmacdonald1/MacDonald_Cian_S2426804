package org.me.gcu.macdonald_cian_s2426804.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * Gets the phone's coarse (network) location using the platform LocationManager,
 * so no extra libraries are needed.
 */
public class LocationHelper {

    public interface Callback {
        /** Called on the main thread with a location, or null if none could be found. */
        void onLocation(Location location);
    }

    private static final long TIMEOUT_MS = 10000;

    public static boolean hasPermission(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")   // checked by hasPermission() before use
    public static void getCoarseLocation(Context context, Callback callback) {
        Handler main = new Handler(Looper.getMainLooper());
        if (!hasPermission(context)) {
            main.post(() -> callback.onLocation(null));
            return;
        }
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            main.post(() -> callback.onLocation(null));
            return;
        }

        // 1. use a recent last-known fix if there is one
        Location best = null;
        for (String provider : lm.getProviders(true)) {
            Location l = lm.getLastKnownLocation(provider);
            if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
        }
        if (best != null && System.currentTimeMillis() - best.getTime() < 10 * 60 * 1000) {
            Location found = best;
            main.post(() -> callback.onLocation(found));
            return;
        }

        // 2. otherwise ask for a fresh one
        String provider = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER
                : lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER : null;
        if (provider == null) {
            Location fallback = best;
            main.post(() -> callback.onLocation(fallback));
            return;
        }

        final Location lastKnown = best;
        final boolean[] done = {false};
        Runnable timeout = () -> {
            if (!done[0]) {
                done[0] = true;
                callback.onLocation(lastKnown);
            }
        };
        main.postDelayed(timeout, TIMEOUT_MS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            lm.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(context), location -> {
                if (done[0]) return;
                done[0] = true;
                main.removeCallbacks(timeout);
                callback.onLocation(location != null ? location : lastKnown);
            });
        } else {
            lm.requestSingleUpdate(provider, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    if (done[0]) return;
                    done[0] = true;
                    main.removeCallbacks(timeout);
                    callback.onLocation(location);
                }
            }, Looper.getMainLooper());
        }
    }
}
