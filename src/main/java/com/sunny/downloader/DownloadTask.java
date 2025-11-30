package com.sunny.downloader;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

public class DownloadTask implements Callable<ChunkResult> {

    private final String fileUrl;
    private final String savePath;
    private final long startByte;
    private final long endByte;

    private final int taskId;

    private volatile boolean isPaused = false;
    private volatile boolean isStopped = false;

    private final DownloadManager downloadManager;
    private final int downloadId;

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

    public DownloadTask(DownloadManager downloadManager,
                        int downloadId,
                        String fileUrl,
                        String saveFile,
                        long startByte,
                        long endByte,
                        int taskId) {
        this.downloadManager = downloadManager;
        this.downloadId = downloadId;
        this.fileUrl = fileUrl;
        this.savePath = saveFile;
        this.startByte = startByte;
        this.endByte = endByte;
        this.taskId = taskId;
    }

    @Override
    public ChunkResult call() {
        HttpURLConnection conn = null;
        InputStream inputStream = null;
        RandomAccessFile localFile = null;

        long totalRead = 0;

        try {
            localFile = new RandomAccessFile(savePath, "rw");
            URL url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            String byteRange = "bytes=" + startByte + "-" + endByte;
            conn.setRequestProperty("Range", byteRange);
            applyCommon(conn, fileUrl);

            int responseCode = conn.getResponseCode();
            System.out.println("Response code for thread " + taskId + ": " + responseCode);
            if(responseCode != HttpURLConnection.HTTP_PARTIAL){
                throw new Exception("Server does not support multi-threading");
            }

            inputStream = conn.getInputStream();
            localFile.seek(startByte);

            byte[] buffer = new byte[8192];
            int bytesRead;

            long expectedBytes = endByte - startByte + 1;

            while(totalRead < expectedBytes && !isStopped){
                synchronized (this) {
                    while(isPaused && !isStopped){
                        wait();
                    }
                }

                if (isStopped) break;

                bytesRead = inputStream.read(buffer);
                if(bytesRead == -1){
                    break;
                }

                long remaining = expectedBytes - totalRead;
                int toWrite = (int) Math.min(bytesRead, remaining);

                localFile.write(buffer, 0, toWrite);

                totalRead += toWrite;

                downloadManager.onChunkProgress(downloadId, toWrite);

                if (toWrite < bytesRead) break;
            }

        } catch (Exception e) {
            System.err.println("Error in download thread " + taskId + ": " + e.getMessage());
            return new ChunkResult(taskId, totalRead, 0, e);
        } finally {
            try {
                if(inputStream != null) inputStream.close();
                if(conn != null) conn.disconnect();
                if(localFile != null) localFile.close();
            }catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return new ChunkResult(taskId, totalRead, 0, null);
    }


    public void pauseDownload(){
        synchronized (this) {
            isPaused = true;
        }
    }

    public void resumeDownload(){
        synchronized (this) {
            isPaused = false;
            notifyAll();
        }
    }

    public void stopDownload(){
        synchronized (this) {
            isStopped = true;
            isPaused = false;
            notifyAll();
        }
    }
}
