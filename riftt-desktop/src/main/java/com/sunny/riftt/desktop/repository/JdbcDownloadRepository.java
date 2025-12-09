package com.sunny.riftt.desktop.repository;

import com.sunny.riftt.core.IDownloadRepository;
import com.sunny.riftt.database.DownloadDAO;
import com.sunny.riftt.model.Download;
import com.sunny.riftt.model.DownloadStatus;
import java.sql.Timestamp;
import java.util.List;

public class JdbcDownloadRepository implements IDownloadRepository {

    private final DownloadDAO dao = new DownloadDAO();

    @Override
    public int insertDownload(Download download) {
        return dao.insertDownload(download);
    }

    @Override
    public void updateDownload(Download download) {
        dao.updateDownload(download);
    }

    @Override
    public void updateDownloadStatus(int downloadId, DownloadStatus status) {
        dao.updateDownloadStatus(downloadId, status);
    }

    @Override
    public void updateEndTime(int downloadId, Timestamp endTime) {
        dao.updateEndTime(downloadId, endTime);
    }

    @Override
    public Download getDownloadById(int id) {
        try {
            return dao.getDownloadById(id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Download> getAllDownloads() {
        return dao.getAllDownloads();
    }

    @Override
    public boolean deleteDownload(int id) {
        return dao.deleteDownload(id);
    }

    @Override
    public void clearAllDownloads() {
        dao.clearAllDownloads();
    }

    @Override
    public void updateDownloadedSize(int downloadId, long bytesToAdd) {
        dao.updateDownloadedSize(downloadId, bytesToAdd);
    }
}
