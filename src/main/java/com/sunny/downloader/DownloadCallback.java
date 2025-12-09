package com.sunny.downloader;

public interface DownloadCallback {
    void onStart(int id);

    void onPause(int id);

    void onResume(int id);

    void onProgress(int id, long downloaded, long total, double progress);

    void onDownloadCompleted(int id);

    void onDownloadFailed(int id, String message);

    void onDownloadCancelled(int id);
}
