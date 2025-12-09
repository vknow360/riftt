package com.sunny.downloader;

import com.sunny.core.IChunkRepository;
import com.sunny.core.IDownloadRepository;
import com.sunny.core.ILogger;
import com.sunny.core.ISettingsProvider;
import com.sunny.model.Download;
import com.sunny.model.DownloadChunk;
import com.sunny.model.DownloadStatus;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadManager {

    private final IDownloadRepository downloadRepo;
    private final IChunkRepository chunkRepo;
    private final ISettingsProvider settings;
    private final ILogger logger;

    private final ExecutorService executorService;
    private final Map<Integer, List<Future<ChunkResult>>> activeFutures;
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
        int threadsPerDownload = settings.getThreadsPerDownload(); // Initial sizing guidance

        // We might want a dynamic pool, but fixed is fine for now
        // Assuming worst case scenario for pool size
        this.executorService = Executors.newFixedThreadPool(maxConcurrent * Math.max(threadsPerDownload, 16));
        this.activeFutures = new ConcurrentHashMap<>();
        this.activeTasks = new ConcurrentHashMap<>();
    }

    public int addDownload(Download download, DownloadCallback callback) {
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
        try {
            Download download = downloadRepo.getDownloadById(id);
            if (download == null) {
                throw new Exception("Download not found");
            }

            if (activeFutures.containsKey(id)) {
                throw new Exception("Download already started");
            }

            // Phase 2: Chunk Persistence
            List<DownloadChunk> chunks = chunkRepo.getChunksForDownload(id);

            // If new download (or legacy without chunks), init chunks
            if (chunks.isEmpty()) {
                download.setDownloadedSize(0L);
                download.setStatus(DownloadStatus.DOWNLOADING);

                FileDownloader fileDownloader = new FileDownloader();
                long fileSize = fileDownloader.getFileSize(download.getUrl());
                download.setFileSize(fileSize);

                downloadRepo.updateDownload(download); // Save size

                // Initialize Chunks
                boolean supportsRange = fileDownloader.supportsRangeRequests(download.getUrl());
                if (!supportsRange || fileSize < 2 * 1024 * 1024) {
                    // Single chunk
                    DownloadChunk chunk = new DownloadChunk(id, 0, fileSize - 1);
                    chunks.add(chunk);
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
                chunkRepo.createChunks(chunks);
                // Reload to get IDs
                chunks = chunkRepo.getChunksForDownload(id);

                DownloadCallback callback = callbacks.get(id);
                if (callback != null) {
                    callback.onStart(id);
                    callback.onProgress(id, 0, fileSize, 0);
                }
            } else {
                // RESUME: Calculate initial progress from DB state
                long totalDownloaded = 0;
                for (DownloadChunk c : chunks) {
                    totalDownloaded += (c.getCurrentOffset() - c.getStartByte());
                }
                // Update in-memory accumulator to 0 (It's a delta buffer!)
                downloadProgress.put(id, new AtomicLong(0));

                download.setDownloadedSize(totalDownloaded);
                download.setStatus(DownloadStatus.DOWNLOADING);
                downloadRepo.updateDownload(download);

                DownloadCallback callback = callbacks.get(id);
                if (callback != null) {
                    callback.onResume(id);
                    double prog = (totalDownloaded * 100.0) / download.getFileSize();
                    callback.onProgress(id, totalDownloaded, download.getFileSize(), prog);
                }
            }

            List<DownloadTask> tasks = new ArrayList<>();
            List<Future<ChunkResult>> futures = new ArrayList<>();

            // If not tracking yet
            downloadProgress.computeIfAbsent(id, k -> new AtomicLong(download.getDownloadedSize()));
            lastReportedTime.put(id, System.currentTimeMillis());

            // Create tasks ONLY for incomplete chunks
            for (DownloadChunk chunk : chunks) {
                if (chunk.getCurrentOffset() > chunk.getEndByte()) {
                    continue; // Chunk completed
                }

                DownloadTask task = new DownloadTask(
                        this,
                        id,
                        download.getUrl(),
                        download.getDownloadPath(),
                        chunk,
                        chunkRepo, // Use Interface
                        logger);
                tasks.add(task);
                Future<ChunkResult> future = executorService.submit(task);
                futures.add(future);
            }

            if (futures.isEmpty()) {
                // Already done?
                monitorDownload(id, futures); // Will detect "allSuccess"
            } else {
                activeFutures.put(id, futures);
                activeTasks.put(id, tasks);
                monitorDownload(id, futures);
            }

        } catch (Exception e) {
            DownloadCallback cb = callbacks.get(id);
            if (cb != null)
                cb.onDownloadFailed(id, e.getMessage());
            logger.error("Start download failed", e);
        }
    }

    public void pauseDownload(int id) throws Exception {
        Download download = downloadRepo.getDownloadById(id);
        if (download.getStatus() == DownloadStatus.PAUSED)
            return;

        List<DownloadTask> tasks = activeTasks.get(id);
        if (tasks != null) {
            for (DownloadTask task : tasks) {
                task.pauseDownload();
            }
        }

        download.setStatus(DownloadStatus.PAUSED);
        downloadRepo.updateDownload(download);

        DownloadCallback callback = callbacks.get(id);
        if (callback != null) {
            callback.onPause(id);
        }
    }

    public void removeDownload(int id) {
        try {
            // Stop logic
            List<DownloadTask> tasks = activeTasks.get(id);
            if (tasks != null) {
                for (DownloadTask task : tasks) {
                    task.stopDownload();
                }
            }
            List<Future<ChunkResult>> futures = activeFutures.get(id);
            if (futures != null) {
                for (Future<ChunkResult> f : futures) {
                    f.cancel(true);
                }
            }

            activeFutures.remove(id);
            activeTasks.remove(id);
            downloadProgress.remove(id);
            callbacks.remove(id);

            // DAO Delete
            downloadRepo.deleteDownload(id);

        } catch (Exception e) {
            logger.error("Remove download failed", e);
        }
    }

    public void removeAllDownloads() {
        try {
            // Stop all
            List<Integer> ids = new ArrayList<>(activeTasks.keySet());
            for (Integer id : ids) {
                removeDownload(id);
            }
            // Fallback clear
            downloadRepo.clearAllDownloads();

        } catch (Exception e) {
            logger.error("Remove all downloads failed", e);
        }
    }

    public void resumeDownload(int id) {
        try {
            Download download = downloadRepo.getDownloadById(id);
            if (download != null && download.getStatus() == DownloadStatus.COMPLETED) {
                // Ignore
                return;
            }

            if (download.getStatus() == DownloadStatus.DOWNLOADING)
                return; // Already downloading

            List<DownloadTask> tasks = activeTasks.get(id);

            if (tasks != null && !tasks.isEmpty()) {
                for (DownloadTask task : tasks) {
                    task.resumeDownload();
                }
            } else {
                if (!activeFutures.containsKey(id)) {
                    startDownload(id);
                    return;
                }
            }

            download.setStatus(DownloadStatus.DOWNLOADING);
            downloadRepo.updateDownload(download);

            DownloadCallback callback = callbacks.get(id);
            if (callback != null) {
                callback.onResume(id);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void cancelDownload(int id) {
        try {
            downloadRepo.updateDownloadStatus(id, DownloadStatus.CANCELED);

            List<Future<ChunkResult>> futures = activeFutures.get(id);
            if (futures != null) {
                for (Future<ChunkResult> future : futures) {
                    future.cancel(true);
                }
                activeFutures.remove(id);
            }

            List<DownloadTask> tasks = activeTasks.get(id);
            if (tasks != null) {
                for (DownloadTask task : tasks) {
                    task.stopDownload();
                }
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
            throw new RuntimeException(e);
        }
    }

    private void monitorDownload(int downloadId, List<Future<ChunkResult>> futures) {
        new Thread(() -> {
            try {
                boolean allSuccess = true;
                String failMessage = "";

                for (Future<ChunkResult> future : futures) {
                    try {
                        ChunkResult result = future.get();
                        if (result.getError() != null) {
                            allSuccess = false;
                            failMessage = result.getError().getMessage();
                            logger.error("Chunk failed: " + failMessage);
                            break;
                        }
                    } catch (CancellationException e) {
                        allSuccess = false;
                        failMessage = "Cancelled";
                        break;
                    } catch (InterruptedException | ExecutionException e) {
                        allSuccess = false;
                        failMessage = "Interrupted";
                        break;
                    }
                }
                flushProgressIfAny(downloadId);

                Download download = downloadRepo.getDownloadById(downloadId);
                if (download.getStatus() == DownloadStatus.CANCELED) {
                    return;
                }

                long downloaded = download.getDownloadedSize();
                long totalSize = download.getFileSize();

                File file = new File(download.getDownloadPath());
                boolean sizeMatched = (file.exists() && file.length() == totalSize);

                boolean finalSuccess = allSuccess && sizeMatched;

                if (!sizeMatched) {
                    logger.error("Size mismatch! Disk=" + (file.exists() ? file.length() : -1) + " total=" + totalSize);
                    if (allSuccess)
                        failMessage = "Size Mismatch";
                }

                if (!finalSuccess) {
                    List<DownloadTask> tasks = activeTasks.get(downloadId);
                    if (tasks != null) {
                        for (DownloadTask task : tasks) {
                            task.stopDownload();
                        }
                    }
                }

                if (finalSuccess) {
                    download.setStatus(DownloadStatus.COMPLETED);
                    download.setDownloadedSize(totalSize);
                } else {
                    download.setStatus(DownloadStatus.FAILED);
                }
                download.setEndTime(new Timestamp(System.currentTimeMillis()));
                downloadRepo.updateDownload(download);

                activeFutures.remove(downloadId);
                activeTasks.remove(downloadId);
                lastReportedTime.remove(downloadId);
                downloadProgress.remove(downloadId);

                DownloadCallback cb = callbacks.get(downloadId);
                if (cb != null) {
                    if (finalSuccess) {
                        cb.onProgress(downloadId, totalSize, totalSize, 100.0);
                        cb.onDownloadCompleted(downloadId);
                    } else {
                        cb.onDownloadFailed(downloadId, failMessage);
                    }
                }

            } catch (Exception e) {
                logger.error("Monitor thread error", e);
            }
        }).start();
    }

    public double getDownloadProgress(int downloadId) throws Exception {
        Download download = downloadRepo.getDownloadById(downloadId);
        if (download == null) {
            throw new Exception("Download not found");
        }

        if (download.getStatus() == DownloadStatus.CANCELED || download.getStatus() == DownloadStatus.FAILED) {
            return 0.0;
        }

        if (download.getStatus() == DownloadStatus.COMPLETED) {
            return 100.0;
        }

        if (download.getFileSize() == 0)
            return 0.0;

        long dbProgress = download.getDownloadedSize();
        double totalProgress = Math.min(dbProgress, download.getFileSize());
        return (totalProgress * 100.0) / download.getFileSize();
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

                    // Notify Callback
                    DownloadCallback cb = callbacks.get(downloadId);
                    if (cb != null) {
                        Download d = downloadRepo.getDownloadById(downloadId);
                        double prog = (d.getDownloadedSize() * 100.0) / d.getFileSize();
                        cb.onProgress(downloadId, d.getDownloadedSize(), d.getFileSize(), Math.ceil(prog));
                    }
                    lastReportedTime.put(downloadId, now);

                } catch (Exception e) {
                    acc.addAndGet(toFlush); // Restore
                    logger.error("DB Update failed for ID " + downloadId + ", restored " + toFlush + " bytes.", e);
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

    public DownloadStatus getDownloadStatus(int downloadId) throws Exception {
        Download d = downloadRepo.getDownloadById(downloadId);
        if (d == null) {
            throw new Exception("Download not found");
        }
        return d.getStatus();
    }

    public List<Download> getAllDownloads() {
        return downloadRepo.getAllDownloads();
    }

    public List<DownloadChunk> getChunks(int downloadId) {
        return chunkRepo.getChunksForDownload(downloadId);
    }
}
