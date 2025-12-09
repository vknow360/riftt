package com.sunny.riftt.downloader;

import com.sunny.riftt.core.IChunkRepository;
import com.sunny.riftt.core.ILogger;
import com.sunny.riftt.model.DownloadChunk;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;

public class DownloadTask implements Callable<ChunkResult> {

    private final String fileUrl;
    private final String savePath;
    private final DownloadChunk chunk;
    private final IChunkRepository chunkRepo; // INTERFACE
    private final DownloadManager downloadManager;
    private final int downloadId;
    private final ILogger logger; // INTERFACE

    private volatile boolean isPaused = false;
    private volatile boolean isStopped = false;

    public DownloadTask(DownloadManager downloadManager,
            int downloadId,
            String fileUrl,
            String saveFile,
            DownloadChunk chunk,
            IChunkRepository chunkRepo, // Inject Interface
            ILogger logger) {
        this.downloadManager = downloadManager;
        this.downloadId = downloadId;
        this.fileUrl = fileUrl;
        this.savePath = saveFile;
        this.chunk = chunk;
        this.chunkRepo = chunkRepo;
        this.logger = logger;
    }

    @Override
    public ChunkResult call() {
        HttpURLConnection conn = null;
        InputStream inputStream = null;
        RandomAccessFile localFile = null;

        long currentOffset = chunk.getCurrentOffset();
        long endByte = chunk.getEndByte();

        if (endByte != -1 && currentOffset > endByte) {
            return new ChunkResult(chunk.getId(), 0, 0, null);
        }

        int retryCount = 0;
        final int MAX_RETRIES = 5;

        try {
            localFile = new RandomAccessFile(savePath, "rw");

            while ((endByte == -1 || currentOffset <= endByte) && !isStopped) {

                synchronized (this) {
                    while (isPaused && !isStopped) {
                        try {
                            chunkRepo.updateChunkProgress(chunk.getId(), currentOffset, "PAUSED");
                        } catch (Exception e) {
                        }

                        logger.log("Chunk " + chunk.getId() + " paused at " + currentOffset);
                        wait();

                        try {
                            chunkRepo.updateChunkProgress(chunk.getId(), currentOffset, "DOWNLOADING");
                        } catch (Exception e) {
                        }
                        logger.log("Chunk " + chunk.getId() + " resumed.");
                    }
                }
                if (isStopped)
                    break;

                try {
                    String byteRange = null;
                    if (endByte != -1 || currentOffset > 0) {
                        byteRange = (endByte == -1)
                                ? "bytes=" + currentOffset + "-"
                                : "bytes=" + currentOffset + "-" + endByte;
                    }

                    logger.log("Chunk " + chunk.getId() + " connecting. Range: "
                            + (byteRange == null ? "Full File" : byteRange));

                    conn = FileDownloader.safeOpenConnection(fileUrl, "GET", byteRange);

                    int responseCode = conn.getResponseCode();
                    logger.log("Chunk " + chunk.getId() + " response code: " + responseCode);

                    if (responseCode >= 400) {
                        throw new Exception("Server returned HTTP " + responseCode);
                    }

                    if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                        if (responseCode == 200 && currentOffset > 0) {
                            throw new Exception("Server does not support partial requests");
                        }
                    }

                    inputStream = conn.getInputStream();
                    localFile.seek(currentOffset);

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long bytesSinceLastSave = 0;
                    final long SAVE_INTERVAL = 64 * 1024;

                    logger.log("Chunk " + chunk.getId() + " starting read loop. Offset: " + currentOffset + ", End: "
                            + endByte);

                    while ((endByte == -1 || currentOffset <= endByte) && !isStopped) {
                        synchronized (this) {
                            while (isPaused && !isStopped) {
                                try {
                                    chunkRepo.updateChunkProgress(chunk.getId(), currentOffset, "PAUSED");
                                } catch (Exception e) {
                                }
                                wait();
                                try {
                                    chunkRepo.updateChunkProgress(chunk.getId(), currentOffset, "DOWNLOADING");
                                } catch (Exception e) {
                                }
                            }
                        }
                        if (isStopped)
                            break;

                        bytesRead = inputStream.read(buffer);
                        if (bytesRead == -1)
                            break;

                        retryCount = 0;
                        int toWrite = bytesRead;

                        if (endByte != -1) {
                            long remaining = endByte - currentOffset + 1;
                            toWrite = (int) Math.min(bytesRead, remaining);
                        }

                        localFile.write(buffer, 0, toWrite);
                        currentOffset += toWrite;

                        downloadManager.onChunkProgress(downloadId, toWrite);

                        bytesSinceLastSave += toWrite;
                        if (bytesSinceLastSave >= SAVE_INTERVAL) {
                            chunkRepo.updateChunkProgress(chunk.getId(), currentOffset, "DOWNLOADING");
                            bytesSinceLastSave = 0;
                        }

                        if (toWrite < bytesRead)
                            break;
                    }

                    if (endByte == -1 || currentOffset > endByte) {
                        chunkRepo.updateChunkProgress(chunk.getId(), currentOffset, "COMPLETED");
                        break;
                    }

                } catch (Exception e) {
                    if (isStopped)
                        break;
                    retryCount++;
                    if (retryCount > MAX_RETRIES)
                        throw e;

                    logger.error("Chunk " + chunk.getId() + " retry " + retryCount + ": " + e.getMessage());

                    closeQuietly(inputStream);
                    disconnectQuietly(conn);

                    Thread.sleep(Math.min(1000L * retryCount, 5000L));
                }
            }
        } catch (Exception e) {
            logger.error("Chunk " + chunk.getId() + " failed: " + e.getMessage());
            return new ChunkResult(chunk.getId(), 0, 0, e);
        } finally {
            closeQuietly(inputStream);
            disconnectQuietly(conn);
            closeQuietly(localFile);
        }
        return new ChunkResult(chunk.getId(), 0, 0, null);
    }

    public void pauseDownload() {
        synchronized (this) {
            isPaused = true;
        }
    }

    public void resumeDownload() {
        synchronized (this) {
            isPaused = false;
            notifyAll();
        }
    }

    public void stopDownload() {
        synchronized (this) {
            isStopped = true;
            isPaused = false;
            notifyAll();
        }
    }

    private void closeQuietly(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void disconnectQuietly(HttpURLConnection c) {
        if (c != null) {
            try {
                c.disconnect();
            } catch (Exception ignored) {
            }
        }
    }
}
