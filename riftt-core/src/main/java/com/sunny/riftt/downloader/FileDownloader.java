package com.sunny.riftt.downloader;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class FileDownloader {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public static void applyCommon(HttpURLConnection conn, String fileUrl) {
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        conn.setRequestProperty("Connection", "keep-alive");
        conn.setRequestProperty("Accept-Encoding", "identity");

        // Modern browser headers
        conn.setRequestProperty("Sec-Fetch-Dest", "empty");
        conn.setRequestProperty("Sec-Fetch-Mode", "cors");
        conn.setRequestProperty("Sec-Fetch-Site", "same-origin");

    }

    private boolean isOk(int code) {
        return code >= 200 && code < 300;
    }

    private boolean isPartial(int code) {
        return code == HttpURLConnection.HTTP_PARTIAL;
    }

    /**
     * Helper method to establish a connection while preserving cookies across
     * redirects.
     * This prevents HTTP 403 errors on servers that issue cookies during the
     * redirect chain.
     */

    public static HttpURLConnection safeOpenConnection(String urlStr, String method, String rangeHeader)
            throws Exception {
        int redirectCount = 0;
        Map<String, String> cookieMap = new HashMap<>();

        while (redirectCount < 5) {

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            applyCommon(conn, urlStr);

            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod(method);

            // Send cookies
            if (!cookieMap.isEmpty()) {
                String cookieHeader = cookieMap.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("; "));
                conn.setRequestProperty("Cookie", cookieHeader);
            }

            if (rangeHeader != null)
                conn.setRequestProperty("Range", rangeHeader);

            int status = conn.getResponseCode();

            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {

                // --- Read all Set-Cookie headers ---
                List<String> setCookies = conn.getHeaderFields().get("Set-Cookie");
                if (setCookies != null) {
                    for (String sc : setCookies) {
                        String[] parts = sc.split(";", 2);
                        String[] kv = parts[0].split("=", 2);
                        if (kv.length == 2)
                            cookieMap.put(kv[0].trim(), kv[1].trim());
                    }
                }

                // Redirect URL
                String newUrl = conn.getHeaderField("Location");
                if (newUrl == null)
                    throw new Exception("Redirect with no Location header");

                urlStr = new URL(url, newUrl).toString();

                // RFC: 302/303 should become GET
                if (status == 302 || status == 303)
                    method = "GET";

                conn.disconnect();
                redirectCount++;
                continue;
            }

            return conn;
        }

        throw new Exception("Too many redirects");

    }

    /**
     * Check if the server supports range requests (multi-threading)
     * Try HEAD first, then fallback to GET with Range: bytes=0-0.
     */
    public boolean supportsRangeRequests(String fileUrl) {
        HttpURLConnection conn = null;
        try {
            conn = safeOpenConnection(fileUrl, "HEAD", null);

            int responseCode = conn.getResponseCode();
            System.out.println("Response code for HEAD request: " + responseCode);
            if (isOk(responseCode)) {
                String acceptRanges = conn.getHeaderField("Accept-Ranges");
                if (acceptRanges != null && acceptRanges.equalsIgnoreCase("bytes")) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("HEAD range probe failed: " + e.getMessage());
        } finally {
            if (conn != null)
                conn.disconnect();
        }

        try {
            conn = safeOpenConnection(fileUrl, "GET", "bytes=0-0");

            int responseCode = conn.getResponseCode();
            System.out.println("Response code for GET range probe: " + responseCode);
            if (isPartial(responseCode)) {
                return true;
            }
            // Some servers return 200 but still advertise support in headers
            String acceptRanges = conn.getHeaderField("Accept-Ranges");
            String contentRange = conn.getHeaderField("Content-Range");
            return (acceptRanges != null && acceptRanges.equalsIgnoreCase("bytes")) || (contentRange != null);
        } catch (Exception e) {
            System.err.println("GET range probe failed: " + e.getMessage());
            return false;
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }

    /**
     * Get file size using HEAD or GET
     */
    public long getFileSize(String fileUrl) {
        HttpURLConnection conn = null;

        try {
            conn = safeOpenConnection(fileUrl, "HEAD", null);

            int responseCode = conn.getResponseCode();
            if (isOk(responseCode)) {
                long fileSize = conn.getContentLengthLong();
                if (fileSize > 0) {
                    return fileSize;
                }
            }
        } catch (Exception e) {
            System.err.println("HEAD getFileSize failed: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            conn = safeOpenConnection(fileUrl, "GET", "bytes=0-0");

            int responseCode = conn.getResponseCode();
            if (isPartial(responseCode)) {
                String contentRange = conn.getHeaderField("Content-Range"); // e.g.,
                                                                            // "bytes
                                                                            // 0-0/123456"
                if (contentRange != null && contentRange.contains("/")) {
                    String totalStr = contentRange.substring(contentRange.lastIndexOf('/') + 1).trim();
                    try {
                        long total = Long.parseLong(totalStr);
                        if (total > 0)
                            return total;
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else if (isOk(responseCode)) {
                long fileSize = conn.getContentLengthLong();
                if (fileSize > 0)
                    return fileSize;
            }
        } catch (Exception e) {
            System.err.println("GET range getFileSize failed: " + e.getMessage());
        } finally {
            if (conn != null)
                conn.disconnect();
        }

        return -1;
    }
}