package com.sunny.downloader;

import com.sunny.database.DatabaseManager;
import com.sunny.database.DownloadDAO;
import com.sunny.exceptions.DatabaseException;
import com.sunny.model.Download;
import com.sunny.model.DownloadStatus;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadManager {

    private final DatabaseManager dbManager;
    private final DownloadDAO downloadDAO;

    private final ExecutorService executorService;

    private final Map<Integer, List<Future<ChunkResult>>> activeFutures;

    private static final int MAX_CONCURRENT_DOWNLOADS = 3;
    private static final int THREADS_PER_DOWNLOAD = 4;

    private final ConcurrentHashMap<Integer, AtomicLong> downloadProgress = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> lastReportedTime = new ConcurrentHashMap<>();




    public DownloadManager(){
        this.dbManager = DatabaseManager.getInstance();
        this.downloadDAO = new DownloadDAO();
        this.executorService = Executors.newFixedThreadPool(
                MAX_CONCURRENT_DOWNLOADS * THREADS_PER_DOWNLOAD
        );
        this.activeFutures = new ConcurrentHashMap<>();
    }


    public int addDownload(Download download){
        download.setStatus(DownloadStatus.PENDING);
        download.setDownloadedSize(0L);
        download.setStartTime(new Timestamp(System.currentTimeMillis()));

        int downloadId = downloadDAO.insertDownload(download);
        download.setId(downloadId);
        return downloadId;
    }

    public void startDownload(int id){
        try {
            Download download = downloadDAO.getDownloadById(id);
            if(download == null){
                throw new Exception("Download not found");
            }

            if(activeFutures.containsKey(id)){
                throw new Exception("Download already started");
            }

            download.setStatus(DownloadStatus.DOWNLOADING);
            downloadDAO.updateDownload(download);

            FileDownloader fileDownloader = new FileDownloader();
            long fileSize = fileDownloader.getFileSize(download.getUrl());
            download.setFileSize(fileSize);
            downloadDAO.updateDownload(download);

            // Check if server supports range requests
            boolean supportsRangeRequests = fileDownloader.supportsRangeRequests(download.getUrl());
        
            if (!supportsRangeRequests || fileSize < 2*1024*1024) {
                System.out.println("Server does not support multi-threading. Falling back to single-threaded download.");
                startSingleThreadedDownload(id, download, fileDownloader);
                return;
            }

            // Multi-threaded download
            long chunkSize = fileSize / THREADS_PER_DOWNLOAD;

            List<DownloadTask> tasks = new ArrayList<>();

            List<Future<ChunkResult>> futures = new ArrayList<>();

            downloadProgress.put(id, new AtomicLong(0));
            lastReportedTime.put(id, System.currentTimeMillis());

            for(int i = 0; i < THREADS_PER_DOWNLOAD; i++){
                long startByte = i * chunkSize;
                long endByte = (i == THREADS_PER_DOWNLOAD - 1)
                        ? fileSize - 1
                        : (i + 1) * chunkSize - 1;

                DownloadTask task = new DownloadTask(
                        this,
                        id,
                        download.getUrl(),
                        download.getDownloadPath(),
                        startByte,
                        endByte,
                        i+1
                );
                Future<ChunkResult> future = executorService.submit(task);
                futures.add(future);
            }

            activeFutures.put(id, futures);

            monitorDownload(id, futures);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void startSingleThreadedDownload(int downloadId, Download download, FileDownloader fileDownloader) {
        // Create a single thread that downloads the entire file
        downloadProgress.put(downloadId, new AtomicLong(0));
        lastReportedTime.put(downloadId, System.currentTimeMillis());
        Thread downloadThread = new Thread(() -> {
            try {
                fileDownloader.downloadFileWithProgress(
                        download.getUrl(), 
                        download.getDownloadPath(), 
                        this, 
                        downloadId
                );
            
                download.setStatus(DownloadStatus.COMPLETED);
                download.setEndTime(new Timestamp(System.currentTimeMillis()));
                downloadDAO.updateDownload(download);
                System.out.println("Single-threaded download completed!");

            } catch (Exception e) {
                System.err.println("Single-threaded download failed: " + e.getMessage());
                e.printStackTrace();
            
                try {
                    download.setStatus(DownloadStatus.FAILED);
                    downloadDAO.updateDownload(download);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    
        downloadThread.setName("SingleDownloadThread-" + downloadId);
        downloadThread.start();
    
        // Store in activeDownloads for tracking (empty list indicates single-threaded)
        //activeDownloads.put(downloadId, new ArrayList<>());
        activeFutures.put(downloadId, new ArrayList<>());
    }

    public void pauseDownload(int id) throws Exception{
//        List<DownloadTask> threads = activeDownloads.get(id);
//
//        if (threads == null) {
//            throw new Exception("Download not active");
//        }
//
//        for (DownloadTask thread : threads) {
//            thread.pauseDownload();
//        }

        Download download = downloadDAO.getDownloadById(id);
        download.setStatus(DownloadStatus.PAUSED);
        downloadDAO.updateDownload(download);
    }

    public void resumeDownload(int id){
//        if (!activeFutures.containsKey(id)) {
//            throw new RuntimeException("Download not active");
//        }
//        List<DownloadTask> threads = activeDownloads.get(id);
//
//        if (threads == null) {
//            startDownload(id);
//            return;
//        }
//
//        for (DownloadTask task : threads) {
//            task.resumeDownload();
//        }

        try{
            Download download = downloadDAO.getDownloadById(id);
            download.setStatus(DownloadStatus.DOWNLOADING);
            downloadDAO.updateDownload(download);
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancelDownload(int id){
        List<Future<ChunkResult>> futures = activeFutures.get(id);

        if (futures != null) {
            for (Future<ChunkResult> future : futures) {
                future.cancel(true);
            }
            activeFutures.remove(id);
        }

        try{

            Download download = downloadDAO.getDownloadById(id);
            File file = new File(download.getDownloadPath());
            if(file.exists()){
                file.delete();
            }

            downloadDAO.updateDownloadStatus(id, DownloadStatus.CANCELED);

        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }

    private void monitorDownload(int downloadId, List<Future<ChunkResult>> futures){
        new Thread(() -> {
            try {
                boolean allSuccess = true;

                for (Future<ChunkResult> future : futures) {
                    try {
                        ChunkResult result = future.get();
                        if (result.getError() != null) {
                            allSuccess = false;
                            System.err.println("Download failed: " + result.getError().getMessage());
                            break;
                        }
                    }catch (CancellationException e){
                        allSuccess = false;
                        System.err.println("Download cancelled");
                        break;
                    }catch (InterruptedException | ExecutionException e){
                        allSuccess = false;
                        System.err.println("Download interrupted");
                        break;
                    }
                }
                flushProgressIfAny(downloadId);

                Download download = downloadDAO.getDownloadById(downloadId);
                long downloaded = download.getDownloadedSize();
                long totalSize = download.getFileSize();

                boolean sizeMatched = (downloaded == totalSize);

                boolean finalSuccess = allSuccess && sizeMatched;

                if (!sizeMatched) {
                    System.err.println("Size mismatch! downloaded=" + downloaded + " total=" + totalSize);
                }


                download.setStatus(finalSuccess ? DownloadStatus.COMPLETED : DownloadStatus.FAILED);
                download.setEndTime(new Timestamp(System.currentTimeMillis()));
                downloadDAO.updateDownload(download);

                activeFutures.remove(downloadId);
                lastReportedTime.remove(downloadId);
                downloadProgress.remove(downloadId);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public double getDownloadProgress(int downloadId) throws Exception{
        Download download = downloadDAO.getDownloadById(downloadId);
        if(download == null){
            throw new Exception("Download not found");
        }

        if(download.getStatus() == DownloadStatus.CANCELED || download.getStatus() == DownloadStatus.FAILED){
            return 0.0;
        }

        if (download.getStatus() == DownloadStatus.COMPLETED) {
            return 100.0;
        }

        if(download.getFileSize() == 0) return 0.0;

        long dbProgress = download.getDownloadedSize();

        double totalProgress = Math.min(dbProgress, download.getFileSize());
        return (totalProgress * 100.0) / download.getFileSize();
    }

    public void shutDown(){
        System.out.println("Shutting down DownloadManager...");

        // Shutdown executor service
        executorService.shutdown();

        try {
            if(!executorService.awaitTermination(60, TimeUnit.SECONDS)){
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        System.out.println("DownloadManager shutdown complete");
    }

//    public void reportProgress(int downloadId, long bytesBuffer) {
//        try {
//            downloadDAO.updateDownloadedSize(downloadId, bytesBuffer);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }


    public void onChunkProgress(int downloadId, long deltaBytes) {
        if (deltaBytes <= 0) return;

        AtomicLong acc = downloadProgress.computeIfAbsent(downloadId, k -> new AtomicLong(0L));
        long newAccum = acc.addAndGet(deltaBytes);

        long now = System.currentTimeMillis();
        long lastReported = lastReportedTime.getOrDefault(downloadId, 0L);

        final long FLUSH_INTERVAL_MS = 150L;

        if (now - lastReported >= FLUSH_INTERVAL_MS) {
            long toFlush = acc.getAndSet(0L);
            if (toFlush > 0) {
                try {
                    downloadDAO.updateDownloadedSize(downloadId, toFlush);
                } catch (Exception e) {
                    // Log but do not rethrow
                    e.printStackTrace();
                }
            }
            lastReportedTime.put(downloadId, now);
        }
    }

    private void flushProgressIfAny(int downloadId) {
        AtomicLong acc = downloadProgress.get(downloadId);
        if (acc == null) return;
        long toFlush = acc.getAndSet(0L);
        if (toFlush > 0) {
            try {
                downloadDAO.updateDownloadedSize(downloadId, toFlush);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    public DownloadStatus getDownloadStatus(int downloadId) throws Exception {
        Download d = downloadDAO.getDownloadById(downloadId);
        if (d == null) {
            throw new Exception("Download not found");
        }
        return d.getStatus();
    }
}
