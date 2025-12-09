package com.sunny.core;

public interface ISettingsProvider {
    int getMaxConcurrentDownloads();
    int getThreadsPerDownload();
    String getDefaultDownloadPath();
    int getConnectionTimeout();
}
