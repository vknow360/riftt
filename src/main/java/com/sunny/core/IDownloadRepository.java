package com.sunny.core;

import com.sunny.model.Download;
import com.sunny.model.DownloadStatus;
import java.sql.Timestamp;
import java.util.List;

public interface IDownloadRepository {
    int insertDownload(Download download);
    void updateDownload(Download download);
    void updateDownloadStatus(int downloadId, DownloadStatus status);
    void updateEndTime(int downloadId, Timestamp endTime);
    Download getDownloadById(int id);
    List<Download> getAllDownloads();
    boolean deleteDownload(int id);
    void clearAllDownloads();
    void updateDownloadedSize(int downloadId, long bytesToAdd);
}
