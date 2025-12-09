package com.sunny.riftt.desktop.database;

import com.sunny.riftt.model.DownloadChunk;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DownloadChunkDAO {

    private final IConnectionProvider connectionProvider;

    public DownloadChunkDAO(IConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void createChunks(List<DownloadChunk> chunks) {
        String sql = "INSERT INTO download_chunks (download_id, start_byte, end_byte, current_offset, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement st = connectionProvider.getConnection().prepareStatement(sql)) {
            for (DownloadChunk chunk : chunks) {
                st.setInt(1, chunk.getDownloadId());
                st.setLong(2, chunk.getStartByte());
                st.setLong(3, chunk.getEndByte());
                st.setLong(4, chunk.getCurrentOffset());
                st.setString(5, chunk.getStatus());
                st.addBatch();
            }
            st.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert chunks", e);
        }
    }

    public List<DownloadChunk> getChunksForDownload(int downloadId) {
        List<DownloadChunk> chunks = new ArrayList<>();
        String sql = "SELECT * FROM download_chunks WHERE download_id = ? ORDER BY start_byte ASC";
        try (PreparedStatement st = connectionProvider.getConnection().prepareStatement(sql)) {
            st.setInt(1, downloadId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    DownloadChunk chunk = new DownloadChunk();
                    chunk.setId(rs.getInt("id"));
                    chunk.setDownloadId(rs.getInt("download_id"));
                    chunk.setStartByte(rs.getLong("start_byte"));
                    chunk.setEndByte(rs.getLong("end_byte"));
                    chunk.setCurrentOffset(rs.getLong("current_offset"));
                    chunk.setStatus(rs.getString("status"));
                    chunks.add(chunk);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get chunks", e);
        }
        return chunks;
    }

    public synchronized void updateChunkProgress(int chunkId, long currentOffset, String status) {
        String sql = "UPDATE download_chunks SET current_offset = ?, status = ? WHERE id = ?";
        try (PreparedStatement st = connectionProvider.getConnection().prepareStatement(sql)) {
            st.setLong(1, currentOffset);
            st.setString(2, status);
            st.setInt(3, chunkId);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update chunk progress", e);
        }
    }
}
