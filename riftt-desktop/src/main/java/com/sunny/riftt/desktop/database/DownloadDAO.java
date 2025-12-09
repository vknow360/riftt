package com.sunny.riftt.desktop.database;

import com.sunny.riftt.exceptions.DatabaseException;
import com.sunny.riftt.model.Download;
import com.sunny.riftt.model.DownloadStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DownloadDAO {

    private final IConnectionProvider connectionProvider;

    public DownloadDAO(IConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public int insertDownload(Download download) {
        String sql = "INSERT INTO downloads (filename, url, file_size, downloaded_size, status, download_path, start_time, end_time, thread_count) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement st = connectionProvider.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setFields(download, st);

            st.executeUpdate();

            try (ResultSet generatedKeys = st.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert download", e);
        }
        return -1;
    }

    public void updateDownload(Download download) {
        String sql = "UPDATE downloads SET filename=?, url=?, file_size=?, downloaded_size=?, status=?, " +
                "download_path=?, start_time=?, end_time=?, thread_count=? WHERE id=?";

        try (PreparedStatement st = connectionProvider.getConnection().prepareStatement(sql)) {

            setFields(download, st);
            st.setInt(10, download.getId());

            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update download", e);
        }
    }

    public void updateDownloadStatus(int downloadId, DownloadStatus status) {
        String sql = "UPDATE downloads SET status = ? WHERE id = ?";
        try (PreparedStatement st = connectionProvider.getConnection().prepareStatement(sql)) {
            st.setString(1, status.name());
            st.setInt(2, downloadId);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update download status", e);
        }
    }

    public void updateEndTime(int downloadId, Timestamp endTime) {
        String sql = "UPDATE downloads SET end_time = ? WHERE id = ?";
        try (PreparedStatement st = connectionProvider.getConnection().prepareStatement(sql)) {
            st.setTimestamp(1, endTime);
            st.setInt(2, downloadId);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update end time", e);
        }
    }

    private void setFields(Download download, PreparedStatement st) throws SQLException {
        st.setString(1, download.getFilename());
        st.setString(2, download.getUrl());
        st.setLong(3, download.getFileSize());
        st.setLong(4, download.getDownloadedSize());
        st.setString(5, download.getStatus().name());
        st.setString(6, download.getDownloadPath());
        st.setTimestamp(7, download.getStartTime());
        st.setTimestamp(8, download.getEndTime());
        st.setInt(9, download.getThreadCount());
    }

    public Download getDownloadById(int id) throws DatabaseException {
        String sql = "SELECT * FROM downloads WHERE id = ?";

        try (PreparedStatement st = connectionProvider.getConnection().prepareStatement(sql)) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDownload(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve download by ID: " + id);
        }
        return null;
    }

    public List<Download> getAllDownloads() {
        List<Download> downloads = new ArrayList<>();
        String sql = "SELECT * FROM downloads";

        try (PreparedStatement st = connectionProvider.getConnection().prepareStatement(sql);
                ResultSet rs = st.executeQuery()) {

            while (rs.next()) {
                downloads.add(mapResultSetToDownload(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve all downloads", e);
        }
        return downloads;
    }

    public List<Download> getDownloadsByStatus(DownloadStatus status) {
        List<Download> downloads = new ArrayList<>();
        String sql = "SELECT * FROM downloads WHERE status = ?";

        try (PreparedStatement st = connectionProvider.getConnection().prepareStatement(sql)) {
            st.setString(1, status.name());
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    downloads.add(mapResultSetToDownload(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve downloads by status: " + status, e);
        }
        return downloads;
    }

    public boolean deleteDownload(int id) {
        String sql = "DELETE FROM downloads WHERE id = ?";

        try (PreparedStatement st = connectionProvider.getConnection().prepareStatement(sql)) {
            st.setInt(1, id);
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete download with ID: " + id, e);
        }
    }

    public void clearAllDownloads() {
        String sql = "DELETE FROM downloads";
        try (Statement st = connectionProvider.getConnection().createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear all downloads", e);
        }
    }

    private Download mapResultSetToDownload(ResultSet rs) throws SQLException {
        Download download = new Download();
        download.setId(rs.getInt("id"));
        download.setFilename(rs.getString("filename"));
        download.setUrl(rs.getString("url"));
        download.setFileSize(rs.getLong("file_size"));
        download.setDownloadedSize(rs.getLong("downloaded_size"));
        download.setStatus(DownloadStatus.valueOf(rs.getString("status")));
        download.setDownloadPath(rs.getString("download_path"));
        download.setStartTime(rs.getTimestamp("start_time"));
        download.setEndTime(rs.getTimestamp("end_time"));
        download.setThreadCount(rs.getInt("thread_count"));
        return download;
    }

    public synchronized void updateDownloadedSize(int downloadId, long bytesToAdd) {
        String sql = "UPDATE downloads SET downloaded_size = downloaded_size + ? WHERE id = ?";
        try (PreparedStatement st = connectionProvider.getConnection().prepareStatement(sql)) {
            st.setLong(1, bytesToAdd);
            st.setInt(2, downloadId);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update downloaded size for download with ID: " + downloadId, e);
        }
    }
}
