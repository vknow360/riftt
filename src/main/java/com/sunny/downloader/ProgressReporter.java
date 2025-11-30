package com.sunny.downloader;

public interface ProgressReporter {
    void reportProgress(int downloadId, long deltaBytes);
}
