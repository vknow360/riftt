package com.sunny.riftt.downloader;

import com.sunny.riftt.core.IChunkRepository;
import com.sunny.riftt.core.IDownloadRepository;
import com.sunny.riftt.core.ILogger;
import com.sunny.riftt.core.ISettingsProvider;
import com.sunny.riftt.model.Download;
import com.sunny.riftt.model.DownloadChunk;
import com.sunny.riftt.model.DownloadStatus;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class DownloadManager {

    private final IDownloadRepository downloadRepo;
    private final IChunkRepository chunkRepo;
    private final ISettingsProvider settings;
    private final ILogger logger;

    private final ExecutorService executorService;
    private final Map<Integer, CompletableFuture<Void>> activeDownloads;
    private final Map<Integer, List<DownloadTask>> activeTasks;
    private final ConcurrentHashMap<Integer, AtomicLong> downloadProgress = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> lastReportedTime = new ConcurrentHashMap<>();

    private final Map<Integer, DownloadCallback> callbacks = new ConcurrentHashMap<>();

    public DownloadManager(IDownloadRepository downloadRepo,
            IChunkRepository chunkRepo,
            ISettingsProvider settings,
            ILogger logger) {
        this.downloadRepo = downloadRepo;
        this.chunkRepo = chunkRepo;
        this.settings = settings;
        this.logger = logger;

        int maxConcurrent = settings.getMaxConcurrentDownloads();
        int threadsPerDownload = settings.getThreadsPerDownload();
        // Fixed thread pool shared across all downloads
        this.executorService = Executors.newFixedThreadPool(maxConcurrent * Math.max(threadsPerDownload, 16));
        this.activeDownloads = new ConcurrentHashMap<>();
        this.activeTasks = new ConcurrentHashMap<>();

        logger.log("DownloadManager initialized with " + maxConcurrent + " threads");
    }

    public int addDownload(Download download, DownloadCallback callback) {
        logger.log("addDownload called for URL: " + download.getUrl());
        download.setStatus(DownloadStatus.PENDING);
        download.setDownloadedSize(0L);
        download.setStartTime(new Timestamp(System.currentTimeMillis()));

        int downloadId = downloadRepo.insertDownload(download);
        download.setId(downloadId);

        if (callback != null) {
            callbacks.put(downloadId, callback);
        }

        return downloadId;
    }

    public void registerCallback(int id, DownloadCallback callback) {
        if (callback != null) {
            callbacks.put(id, callback);
        }
    }

    public void startDownload(int id) {
        logger.log("startDownload called for ID: " + id);
        try {
            Download download = downloadRepo.getDownloadById(id);
            if (download == null) {
                throw new Exception("Download not found");
            }

            if (activeDownloads.containsKey(id)) {
                logger.log("Download " + id + " is already active.");
                return;
            }

            // Phase 2: Chunk Persistence
            List<DownloadChunk> chunks = chunkRepo.getChunksForDownload(id);

            // If new download (or legacy without chunks), init chunks
            if (chunks.isEmpty()) {
                logger.log("Initializing chunks for ID: " + id);
                initializeNewDownload(download, chunks, id);
            } else {
                logger.log("Resuming existing chunks for ID: " + id);
                resumeExistingDownload(download, chunks, id);
            }

            List<DownloadTask> tasks = new ArrayList<>();
            List<CompletableFuture<ChunkResult>> chunkFutures = new ArrayList<>();

            downloadProgress.computeIfAbsent(id, k -> new AtomicLong(download.getDownloadedSize()));
            lastReportedTime.put(id, System.currentTimeMillis());

            // Create tasks ONLY for incomplete chunks
            for (DownloadChunk chunk : chunks) {
                // Fix: Check for endByte != -1 before comparing offset
                if (chunk.getEndByte() != -1 && chunk.getCurrentOffset() > chunk.getEndByte()) {
                    continue; // Chunk completed
                }

                DownloadTask task = new DownloadTask(
                        this,
                        id,
                        download.getUrl(),
                        download.getDownloadPath(),
                        chunk,
                        chunkRepo,
                        logger);
                tasks.add(task);

                // Submit using CompletableFuture
                CompletableFuture<ChunkResult> future = CompletableFuture.supplyAsync(task::call, executorService);
                chunkFutures.add(future);
            }

            if (chunkFutures.isEmpty()) {
                logger.log("No chunk futures created for ID: " + id + ". Checking completion immediately.");
                // Already done logic
                handleDownloadCompletion(id, true, null);
            } else {
                activeTasks.put(id, tasks);

                // Combine all futures
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                        chunkFutures.toArray(new CompletableFuture[0]));

                activeDownloads.put(id, allFutures);

                // Attach completion handler (Non-Blocking Monitor)
                allFutures.whenComplete((v, ex) -> {
                    // Collect results
                    boolean allSuccess = true;
                    String failMessage = null;

                    if (ex != null) {
                        allSuccess = false;
                        failMessage = (ex instanceof CompletionException) ? ex.getCause().getMessage()
                                : ex.getMessage();
                    } else {
                        // Check individual results
                        for (CompletableFuture<ChunkResult> cf : chunkFutures) {
                            try {
                                ChunkResult res = cf.join(); // Safe to join here as they are done
                                if (res.getError() != null) {
                                    allSuccess = false;
                                    failMessage = res.getError().getMessage();
                                    break;
                                }
                            } catch (Exception e) {
                                allSuccess = false;
                                failMessage = "Task execution failed";
                                break;
                            }
                        }
                    }

                    if (failMessage == null)
                        failMessage = "Unknown error";
                    handleDownloadCompletion(id, allSuccess, allSuccess ? null : failMessage);
                });
            }

        } catch (Exception e) {
            DownloadCallback cb = callbacks.get(id);
            if (cb != null)
                cb.onDownloadFailed(id, e.getMessage());
            logger.error("Start download failed for ID " + id, e);
        }
    }

    private void initializeNewDownload(Download download, List<DownloadChunk> chunks, int id) throws Exception {
        download.setDownloadedSize(0L);
        download.setStatus(DownloadStatus.DOWNLOADING);

        FileDownloader fileDownloader = new FileDownloader();
        long fileSize = fileDownloader.getFileSize(download.getUrl());
        download.setFileSize(fileSize);

        downloadRepo.updateDownload(download);

        // Unknown size support
        if (fileSize == -1) {
            // Single chunk, unknown end
            chunks.add(new DownloadChunk(id, 0, -1));
        } else {
            boolean supportsRange = fileDownloader.supportsRangeRequests(download.getUrl());
            if (!supportsRange || fileSize < 2 * 1024 * 1024) {
                // Single chunk
                chunks.add(new DownloadChunk(id, 0, fileSize - 1));
            } else {
                // Multi chunk
                int threads = settings.getThreadsPerDownload();
                long chunkSize = fileSize / threads;
                for (int i = 0; i < threads; i++) {
                    long start = i * chunkSize;
                    long end = (i == threads - 1) ? fileSize - 1 : (i + 1) * chunkSize - 1;
                    chunks.add(new DownloadChunk(id, start, end));
                }
            }
        }
        chunkRepo.createChunks(chunks);
        chunks.clear(); // Clear in-memory chunks before reloading from DB to avoid duplicates
        chunks.addAll(chunkRepo.getChunksForDownload(id)); // Refill with IDs

        logger.log("Loaded " + chunks.size() + " chunks from DB for ID: " + id);

        if (chunks.isEmpty()) {
            logger.error("CRITICAL: Chunks list is empty after creation for ID: " + id);
        }

        DownloadCallback callback = callbacks.get(id);
        if (callback != null) {
            callback.onStart(id);
            callback.onProgress(id, 0, fileSize, 0);
        }
    }

    private void resumeExistingDownload(Download download, List<DownloadChunk> chunks, int id) {
        long totalDownloaded = 0;
        for (DownloadChunk c : chunks) {
            // Safe guard against negative calculation if current=0
            long current = c.getCurrentOffset();
            long start = c.getStartByte();
            if (current > start) {
                totalDownloaded += (current - start);
            }
        }
        downloadProgress.put(id, new AtomicLong(0));
        download.setDownloadedSize(totalDownloaded);
        download.setStatus(DownloadStatus.DOWNLOADING);
        downloadRepo.updateDownload(download);

        DownloadCallback callback = callbacks.get(id);
        if (callback != null) {
            callback.onResume(id);
            if (download.getFileSize() > 0) {
                double prog = (totalDownloaded * 100.0) / download.getFileSize();
                callback.onProgress(id, totalDownloaded, download.getFileSize(), Math.min(100, prog));
            } else {
                callback.onProgress(id, totalDownloaded, -1, 0);
            }
        }
    }

    private void handleDownloadCompletion(int downloadId, boolean allSuccess, String failMessage) {
        // Run in executor or specific thread if needed, but here is fine
        try {
            flushProgressIfAny(downloadId);

            Download download = downloadRepo.getDownloadById(downloadId);
            // If already canceled, ignore
            if (download.getStatus() == DownloadStatus.CANCELED)
                return;

            long totalSize = download.getFileSize();
            File file = new File(download.getDownloadPath());

            boolean sizeMatched;
            if (totalSize == -1) {
                sizeMatched = file.exists() && file.length() > 0;
                if (allSuccess && sizeMatched) {
                    totalSize = file.length();
                    download.setFileSize(totalSize);
                }
            } else {
                sizeMatched = (file.exists() && file.length() == totalSize);
            }

            boolean finalSuccess = allSuccess && sizeMatched;

            if (!sizeMatched) {
                long diskSize = file.exists() ? file.length() : -1;
                // Only log mismatch if we were expecting success and size was known/fixed
                if (totalSize != -1 && diskSize != totalSize) {
                    logger.error("Size mismatch for ID " + downloadId + "! Disk=" + diskSize + " total=" + totalSize);
                }
                if (allSuccess)
                    failMessage = "Size Mismatch";
            }

            if (!finalSuccess) {
                // Stop any lingering tasks just in case
                List<DownloadTask> tasks = activeTasks.get(downloadId);
                if (tasks != null) {
                    tasks.forEach(DownloadTask::stopDownload);
                }
                download.setStatus(DownloadStatus.FAILED);
            } else {
                download.setStatus(DownloadStatus.COMPLETED);
                download.setDownloadedSize(totalSize);
            }

            download.setEndTime(new Timestamp(System.currentTimeMillis()));
            downloadRepo.updateDownload(download);

            activeDownloads.remove(downloadId);
            activeTasks.remove(downloadId);
            lastReportedTime.remove(downloadId);
            downloadProgress.remove(downloadId);

            DownloadCallback cb = callbacks.get(downloadId);
            if (cb != null) {
                if (finalSuccess) {
                    cb.onProgress(downloadId, totalSize, totalSize, 100.0);
                    cb.onDownloadCompleted(downloadId);
                } else {
                    cb.onDownloadFailed(downloadId, failMessage != null ? failMessage : "Unknown Error");
                }
            }

        } catch (Exception e) {
            logger.error("Error handling completion for ID " + downloadId, e);
        }
    }

    public void pauseDownload(int id) throws Exception {
        Download download = downloadRepo.getDownloadById(id);
        if (download.getStatus() == DownloadStatus.PAUSED)
            return;

        List<DownloadTask> tasks = activeTasks.get(id);
        if (tasks != null) {
            tasks.forEach(DownloadTask::pauseDownload);
        }

        download.setStatus(DownloadStatus.PAUSED);
        downloadRepo.updateDownload(download);

        // Remove from active downloads so it can be resumed
        activeDownloads.remove(id); // Future will complete effectively
        activeTasks.remove(id);

        DownloadCallback callback = callbacks.get(id);
        if (callback != null) {
            callback.onPause(id);
        }
    }

    public void removeDownload(int id) {
        try {
            cancelDownloadFutures(id);

            List<DownloadTask> tasks = activeTasks.get(id);
            if (tasks != null) {
                tasks.forEach(DownloadTask::stopDownload);
            }

            activeDownloads.remove(id);
            activeTasks.remove(id);
            downloadProgress.remove(id);
            callbacks.remove(id);

            downloadRepo.deleteDownload(id);

        } catch (Exception e) {
            logger.error("Remove download failed for ID " + id, e);
        }
    }

    public void removeAllDownloads() {
        try {
            List<Integer> ids = new ArrayList<>(activeDownloads.keySet());
            for (Integer id : ids) {
                removeDownload(id);
            }
            downloadRepo.clearAllDownloads();
        } catch (Exception e) {
            logger.error("Remove all downloads failed", e);
        }
    }

    public void resumeDownload(int id) {
        try {
            Download download = downloadRepo.getDownloadById(id);
            if (download != null && download.getStatus() == DownloadStatus.COMPLETED)
                return;
            if (activeDownloads.containsKey(id))
                return; // Already running

            startDownload(id); // Re-trigger start logic which handles resume

        } catch (Exception e) {
            logger.error("Resume failed for ID " + id, e);
            throw new RuntimeException(e);
        }
    }

    public void cancelDownload(int id) {
        try {
            downloadRepo.updateDownloadStatus(id, DownloadStatus.CANCELED);
            cancelDownloadFutures(id);

            List<DownloadTask> tasks = activeTasks.get(id);
            if (tasks != null) {
                tasks.forEach(DownloadTask::stopDownload);
                activeTasks.remove(id);
            }

            Download download = downloadRepo.getDownloadById(id);
            if (download != null) {
                File file = new File(download.getDownloadPath());
                if (file.exists()) {
                    file.delete();
                }
            }

            DownloadCallback cb = callbacks.get(id);
            if (cb != null)
                cb.onDownloadCancelled(id);

            callbacks.remove(id);
            downloadProgress.remove(id);
            lastReportedTime.remove(id);

        } catch (Exception e) {
            logger.error("Cancel failed for ID " + id, e);
            throw new RuntimeException(e);
        }
    }

    private void cancelDownloadFutures(int id) {
        CompletableFuture<Void> future = activeDownloads.get(id);
        if (future != null) {
            future.cancel(true);
            activeDownloads.remove(id);
        }
    }

    public void shutDown() {
        logger.log("Shutting down DownloadManager...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        logger.log("DownloadManager shutdown complete");
    }

    public void onChunkProgress(int downloadId, long deltaBytes) {
        if (deltaBytes <= 0)
            return;

        AtomicLong acc = downloadProgress.computeIfAbsent(downloadId, k -> new AtomicLong(0L));
        long newAccum = acc.addAndGet(deltaBytes);

        long now = System.currentTimeMillis();
        long lastReported = lastReportedTime.getOrDefault(downloadId, 0L);
        final long FLUSH_INTERVAL_MS = 150L;

        if (now - lastReported >= FLUSH_INTERVAL_MS) {
            long toFlush = acc.getAndSet(0L);
            if (toFlush > 0) {
                try {
                    downloadRepo.updateDownloadedSize(downloadId, toFlush);
                    DownloadCallback cb = callbacks.get(downloadId);
                    if (cb != null) {
                        Download d = downloadRepo.getDownloadById(downloadId);
                        double prog = (d.getDownloadedSize() * 100.0) / d.getFileSize();
                        cb.onProgress(downloadId, d.getDownloadedSize(), d.getFileSize(),
                                Math.min(100, Math.ceil(prog)));
                    }
                    lastReportedTime.put(downloadId, now);
                } catch (Exception e) {
                    acc.addAndGet(toFlush);
                    logger.error("DB Update failed for ID " + downloadId, e);
                }
            }
        }
    }

    private void flushProgressIfAny(int downloadId) {
        AtomicLong acc = downloadProgress.get(downloadId);
        if (acc == null)
            return;
        long toFlush = acc.getAndSet(0L);
        if (toFlush > 0) {
            try {
                downloadRepo.updateDownloadedSize(downloadId, toFlush);
            } catch (Exception e) {
                acc.addAndGet(toFlush);
                logger.error("Final flush failed for ID " + downloadId, e);
            }
        }
    }

    public List<Download> getAllDownloads() {
        return downloadRepo.getAllDownloads();
    }

    public List<DownloadChunk> getChunks(int downloadId) {
        return chunkRepo.getChunksForDownload(downloadId);
    }
}
