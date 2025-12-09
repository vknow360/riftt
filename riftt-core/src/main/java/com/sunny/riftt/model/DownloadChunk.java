package com.sunny.riftt.model;

public class DownloadChunk {
    private int id;
    private int downloadId;
    private long startByte;
    private long endByte;
    private long currentOffset;
    private String status; // PENDING, DOWNLOADING, COMPLETED

    public DownloadChunk() {
    }

    public DownloadChunk(int downloadId, long startByte, long endByte) {
        this.downloadId = downloadId;
        this.startByte = startByte;
        this.endByte = endByte;
        this.currentOffset = startByte;
        this.status = "PENDING";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(int downloadId) {
        this.downloadId = downloadId;
    }

    public long getStartByte() {
        return startByte;
    }

    public void setStartByte(long startByte) {
        this.startByte = startByte;
    }

    public long getEndByte() {
        return endByte;
    }

    public void setEndByte(long endByte) {
        this.endByte = endByte;
    }

    public long getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(long currentOffset) {
        this.currentOffset = currentOffset;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
