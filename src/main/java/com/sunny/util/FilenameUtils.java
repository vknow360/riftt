package com.sunny.util;

import com.sunny.downloader.FileDownloader;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class FilenameUtils {

    /**
     * Resolves the filename from the URL, checking Content-Disposition header
     * first.
     * Use this in a background thread as it performs network operations.
     */
    public static String resolveFilename(String urlStr) {
        String filename = null;
        HttpURLConnection conn = null;
        try {
            // 1. Try HEAD request first to get headers without downloading body
            conn = FileDownloader.safeOpenConnection(urlStr, "HEAD", null);
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                String disposition = conn.getHeaderField("Content-Disposition");
                if (disposition != null && !disposition.isEmpty()) {
                    filename = extractFilenameFromDisposition(disposition);
                }

                // If we followed redirects, the URL might have changed to something with a
                // better path
                if (filename == null) {
                    String finalUrl = conn.getURL().toString();
                    if (!finalUrl.equals(urlStr)) {
                        filename = extractFilenameFromUrl(finalUrl);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to resolve filename via network: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // 2. Fallback to extracting from the original URL string
        if (filename == null || filename.trim().isEmpty()) {
            filename = extractFilenameFromUrl(urlStr);
        }

        // 3. Final sanitization and fallback
        return sanitizeFilename(filename);
    }

    private static String extractFilenameFromDisposition(String disposition) {
        // Content-Disposition: attachment; filename="filename.jpg"
        // Content-Disposition: attachment; filename*=UTF-8''filename.jpg

        // Simple parsing for now
        String filename = null;
        String[] parts = disposition.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.toLowerCase().startsWith("filename=")) {
                filename = part.substring(9);
                if (filename.startsWith("\"") && filename.endsWith("\"")) {
                    filename = filename.substring(1, filename.length() - 1);
                }
                break;
            }
            // Note: filename* handling is more complex (encoding), skipping for basic MVP
            // unless needed
        }
        return filename;
    }

    private static String extractFilenameFromUrl(String url) {
        try {
            // Remove query parameters
            int queryIdx = url.indexOf('?');
            if (queryIdx != -1) {
                url = url.substring(0, queryIdx);
            }
            int fragmentIdx = url.indexOf('#');
            if (fragmentIdx != -1) {
                url = url.substring(0, fragmentIdx);
            }
            // Get last segment
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash != -1) {
                return URLDecoder.decode(url.substring(lastSlash + 1), StandardCharsets.UTF_8.name());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "downloaded_file";
        }

        // Replace invalid characters for Windows/Linux filesystems
        // Windows: < > : " / \ | ? *
        String sanitized = filename.replaceAll("[<>:\"/\\\\|?*]", "_");

        // Trim length if needed (windows limit 255 but path adds to it, let's just be
        // safe-ish)
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }

        return sanitized.trim();
    }
}
