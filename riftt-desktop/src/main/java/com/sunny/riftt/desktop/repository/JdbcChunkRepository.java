package com.sunny.riftt.desktop.repository;

import com.sunny.riftt.core.IChunkRepository;
import com.sunny.riftt.desktop.database.DownloadChunkDAO;
import com.sunny.riftt.model.DownloadChunk;

import java.util.List;

import com.sunny.riftt.desktop.database.DatabaseManager;

public class JdbcChunkRepository implements IChunkRepository {

    private final DownloadChunkDAO dao = new DownloadChunkDAO(DatabaseManager.getInstance());

    @Override
    public void createChunks(List<DownloadChunk> chunks) {
        dao.createChunks(chunks);
    }

    @Override
    public List<DownloadChunk> getChunksForDownload(int downloadId) {
        return dao.getChunksForDownload(downloadId);
    }

    @Override
    public void updateChunkProgress(int chunkId, long currentOffset, String status) {
        dao.updateChunkProgress(chunkId, currentOffset, status);
    }
}
