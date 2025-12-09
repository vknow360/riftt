package com.sunny.core;

import com.sunny.model.DownloadChunk;
import java.util.List;

public interface IChunkRepository {
    void createChunks(List<DownloadChunk> chunks);
    List<DownloadChunk> getChunksForDownload(int downloadId);
    void updateChunkProgress(int chunkId, long currentOffset, String status);
}
