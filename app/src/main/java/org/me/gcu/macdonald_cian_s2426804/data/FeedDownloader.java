package org.me.gcu.macdonald_cian_s2426804.data;

import org.me.gcu.macdonald_cian_s2426804.model.FeedType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Downloads one Traffic Scotland DATEX II feed and returns the raw XML as a String.
 * Based on the starter code's download Task, but as a reusable class that reports
 * failures properly instead of silently returning an empty string.
 * Must be called from a background thread.
 */
public class FeedDownloader {

    private static final int TIMEOUT_MS = 15000;

    /** Thrown when the download fails, with a message suitable for the user. */
    public static class DownloadException extends Exception {
        public DownloadException(String message) { super(message); }
    }

    private final String clientId;
    private final String clientKey;

    public FeedDownloader(String clientId, String clientKey) {
        this.clientId = clientId;
        this.clientKey = clientKey;
    }

    public boolean hasCredentials() {
        return isSet(clientId) && isSet(clientKey);
    }

    private static boolean isSet(String s) {
        return s != null && !s.trim().isEmpty() && !s.equals("KEY_HERE") && !s.startsWith("YOUR_");
    }

    public String download(FeedType feed) throws DownloadException {
        if (!hasCredentials()) {
            throw new DownloadException("No Traffic Scotland API keys set (add them to local.properties)");
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(feed.getUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            // Authorisation header: base64 of "clientId:clientKey" (as in the starter code)
            connection.setRequestProperty("Authorization",
                    Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes(StandardCharsets.UTF_8)));

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new DownloadException("Traffic Scotland rejected the API keys (HTTP " + code + ")");
            }
            if (code != HttpURLConnection.HTTP_OK) {
                throw new DownloadException("Traffic Scotland returned HTTP " + code);
            }

            StringBuilder result = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line).append('\n');
                }
            }
            String xml = result.toString();
            if (!xml.contains("<")) throw new DownloadException("Download was empty");
            // With wrong credentials the server can answer "200 OK" with an HTML page instead of
            // the feed, so check this really is a DATEX II publication before handing it to the parser
            String contentType = connection.getContentType();
            boolean isHtml = contentType != null && contentType.toLowerCase().contains("html");
            if (isHtml || !(xml.contains("d2LogicalModel") || xml.contains("SituationPublication"))) {
                throw new DownloadException("Traffic Scotland didn't send traffic data – check the API keys in local.properties");
            }
            return xml;
        } catch (IOException e) {
            throw new DownloadException("Couldn't reach Traffic Scotland (" + e.getClass().getSimpleName() + ")");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
