package com.sunny.riftt.core;

public interface ISettingsProvider {
    int getMaxConcurrentDownloads();
    int getThreadsPerDownload();
    String getDefaultDownloadPath();
    int getConnectionTimeout();
}
