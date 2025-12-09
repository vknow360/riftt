package com.sunny.desktop;

import com.sunny.core.IChunkRepository;
import com.sunny.database.DownloadChunkDAO;
import com.sunny.model.DownloadChunk;
import java.util.List;

public class JdbcChunkRepository implements IChunkRepository {
    
    private final DownloadChunkDAO dao = new DownloadChunkDAO();

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
