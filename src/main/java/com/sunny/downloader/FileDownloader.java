package com.sunny.downloader;

import com.sunny.database.DownloadDAO;
import com.sunny.exceptions.DownloadFailedException;
import com.sunny.model.Download;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileDownloader {

    private static final String TAG = "FileDownloader";

    private static final int BUFFER_SIZE = 8192; // Increased buffer size

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private void applyCommon(HttpURLConnection conn, String fileUrl) {
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        conn.setRequestProperty("Connection", "keep-alive");
        conn.setRequestProperty("Accept-Encoding", "identity");
        try {
            URL u = new URL(fileUrl);
            conn.setRequestProperty("Referer", u.getProtocol() + "://" + u.getHost() + "/");
        } catch (Exception ignored) {}
    }

    private boolean isOk(int code) { return code >= 200 && code < 300; }
    private boolean isPartial(int code) { return code == HttpURLConnection.HTTP_PARTIAL; }

    /**
     * Check if the server supports range requests (multi-threading)
     * Try HEAD first, then fallback to GET with Range: bytes=0-0.
     */
    public boolean supportsRangeRequests(String fileUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            applyCommon(conn, fileUrl);
            conn.setRequestMethod("HEAD");

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
            if (conn != null) conn.disconnect();
        }

        try {
            URL url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            applyCommon(conn, fileUrl);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Range", "bytes=0-0");

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
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Download file with progress reporting for single-threaded downloads
     */
    public void downloadFileWithProgress(String fileUrl, String savePath,
                                         DownloadManager manager, int downloadId)
            throws DownloadFailedException {
        HttpURLConnection conn = null;
        FileOutputStream outputStream = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            applyCommon(conn, fileUrl);
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            System.out.println("Response code for GET request: " + responseCode);

            if (!isOk(responseCode)) {
                throw new Exception("Server returned HTTP " + responseCode
                        + " " + conn.getResponseMessage());
            }

            inputStream = conn.getInputStream();
            outputStream = new FileOutputStream(savePath);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long bytesBuffer = 0;
            long reportThreshold = 102400; // Report every 100KB

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                bytesBuffer += bytesRead;

                // Report progress periodically
                if (bytesBuffer >= reportThreshold) {
                    manager.onChunkProgress(downloadId, bytesBuffer);
                    bytesBuffer = 0;
                }
            }

            // Report remaining bytes
            if (bytesBuffer > 0) {
                manager.onChunkProgress(downloadId, bytesBuffer);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new DownloadFailedException("Download Failed: " + e.getMessage());
        } finally {
            try {
                if (outputStream != null) outputStream.close();
                if (inputStream != null) inputStream.close();
                if (conn != null) conn.disconnect();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void downloadFile(String fileUrl, String savePath) throws DownloadFailedException {
        HttpURLConnection conn = null;
        FileOutputStream outputStream = null;
        InputStream inputStream = null;

        try{
            URL url = new URL(fileUrl);

            conn = (HttpURLConnection) url.openConnection();
            applyCommon(conn, fileUrl);
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();

            if(!isOk(responseCode)){
                throw new Exception("Server returned HTTP " + responseCode
                        + " " + conn.getResponseMessage());
            }

            inputStream = conn.getInputStream();
            outputStream = new FileOutputStream(savePath);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while((bytesRead = inputStream.read(buffer)) != -1){
                outputStream.write(buffer, 0, bytesRead);
            }

        } catch (Exception e) {
            throw new DownloadFailedException(e.getMessage());
        } finally {
            try {
                if(outputStream != null) outputStream.close();
                if(inputStream != null) inputStream.close();
                if(conn != null) conn.disconnect();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public long getFileSize(String fileUrl){
        HttpURLConnection conn = null;

        try{
            URL urlObj = new URL(fileUrl);
            conn = (HttpURLConnection) urlObj.openConnection();
            applyCommon(conn, fileUrl);
            conn.setRequestMethod("HEAD");

            int responseCode = conn.getResponseCode();
            if(isOk(responseCode)){
                long fileSize = conn.getContentLengthLong();
                if(fileSize > 0){
                    return fileSize;
                }
            }
        } catch (Exception e) {
            System.err.println("HEAD getFileSize failed: " + e.getMessage());
        } finally {
            if(conn != null){
                conn.disconnect();
            }
        }

        try {
            URL urlObj = new URL(fileUrl);
            conn = (HttpURLConnection) urlObj.openConnection();
            applyCommon(conn, fileUrl);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Range", "bytes=0-0");

            int responseCode = conn.getResponseCode();
            if (isPartial(responseCode)) {
                String contentRange = conn.getHeaderField("Content-Range"); // e.g., "bytes 0-0/123456"
                if (contentRange != null && contentRange.contains("/")) {
                    String totalStr = contentRange.substring(contentRange.lastIndexOf('/') + 1).trim();
                    try {
                        long total = Long.parseLong(totalStr);
                        if (total > 0) return total;
                    } catch (NumberFormatException ignored) {}
                }
            } else if (isOk(responseCode)) {
                long fileSize = conn.getContentLengthLong();
                if (fileSize > 0) return fileSize;
            }
        } catch (Exception e) {
            System.err.println("GET range getFileSize failed: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }

        return -1;
    }

    public void resumeDownload(int id) throws DownloadFailedException {
        HttpURLConnection httpConn = null;
        FileOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            Download download = new DownloadDAO().getDownloadById(id);
            URL url = new URL(download.getUrl());
            httpConn = (HttpURLConnection) url.openConnection();
            applyCommon(httpConn, download.getUrl());
            httpConn.setRequestProperty("Range", "bytes=" + download.getDownloadedSize() + "-");

            int responseCode = httpConn.getResponseCode();

            if(responseCode != HttpURLConnection.HTTP_PARTIAL){
                throw new Exception("Resume not supported");
            }

            inputStream = httpConn.getInputStream();
            outputStream = new FileOutputStream(download.getDownloadPath(), true);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytesRead = download.getDownloadedSize();

            while((bytesRead = inputStream.read(buffer)) != -1){
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
            download.setDownloadedSize(totalBytesRead);
            new DownloadDAO().updateDownload(download);
        } catch (Exception e){
            throw new DownloadFailedException(e.getMessage());
        } finally {
            try {
                if(outputStream != null) outputStream.close();
                if(inputStream != null) inputStream.close();
                if(httpConn != null) httpConn.disconnect();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}