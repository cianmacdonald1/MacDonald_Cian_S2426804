package org.me.gcu.macdonald_cian_s2426804.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Watches the device's internet connection. Exposes it as LiveData for the offline
 * banner, and calls a listener when the connection comes back so the app can refresh.
 */
public class NetworkMonitor {

    public interface Listener {
        void onConnectionRestored();
    }

    private final ConnectivityManager connectivity;
    private final MutableLiveData<Boolean> online = new MutableLiveData<>();
    private final Listener listener;
    private volatile boolean lastKnownOnline;

    private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
            update(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
        }

        @Override
        public void onLost(@NonNull Network network) {
            update(false);
        }
    };

    public NetworkMonitor(Context context, Listener listener) {
        this.connectivity = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listener = listener;
        lastKnownOnline = isOnlineNow();
        online.setValue(lastKnownOnline);
    }

    public void start() {
        try {
            connectivity.registerDefaultNetworkCallback(callback);
        } catch (RuntimeException e) {
            // too many callbacks registered or permission missing - banner just won't update live
        }
    }

    public void stop() {
        try {
            connectivity.unregisterNetworkCallback(callback);
        } catch (RuntimeException ignored) {
            // wasn't registered
        }
    }

    public LiveData<Boolean> getOnline() { return online; }

    public boolean isOnlineNow() {
        Network network = connectivity.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = connectivity.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void update(boolean nowOnline) {
        boolean wasOnline = lastKnownOnline;
        lastKnownOnline = nowOnline;
        online.postValue(nowOnline);   // callbacks arrive on a binder thread
        if (nowOnline && !wasOnline && listener != null) listener.onConnectionRestored();
    }
}
