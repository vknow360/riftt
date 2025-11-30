package com.sunny.model;

import java.sql.Timestamp;

public class Download {
    String filename;
    String url;
    long fileSize;
    long downloadedSize;
    DownloadStatus status;
    String downloadPath;
    Timestamp startTime;
    Timestamp endTime;
    int threadCount;

    int id;

    public Download() {
    }

    public Download(String filename, String url, long fileSize, long downloadedSize, DownloadStatus status, String downloadPath, Timestamp startTime, Timestamp endTime, int threadCount) {
        this.filename = filename;
        this.url = url;
        this.fileSize = fileSize;
        this.downloadedSize = downloadedSize;
        this.status = status;
        this.downloadPath = downloadPath;
        this.startTime = startTime;
        this.endTime = endTime;
        this.threadCount = threadCount;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getFileSize() {
        return fileSize;
    }
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    public long getDownloadedSize() {
        return downloadedSize;
    }
    public void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
    }
    public DownloadStatus getStatus() {
        return status;
    }

    public void setStatus(DownloadStatus status) {
        this.status = status;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }
    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }
    public int getThreadCount() {
        return threadCount;
    }
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

}
