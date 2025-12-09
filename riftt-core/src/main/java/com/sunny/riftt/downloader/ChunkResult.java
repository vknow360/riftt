package com.sunny.riftt.downloader;

public final class ChunkResult {
    public final int taskId;
    public final long bytesWritten;
    public final int attempts;

    public Throwable error;

    public ChunkResult(int taskId, long bytesWritten, int attempts, Throwable error){
        this.taskId = taskId;
        this.bytesWritten = bytesWritten;
        this.attempts = attempts;
        this.error = error;
    }

    public int getAttempts() {
        return attempts;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    public int getTaskId() {
        return taskId;
    }

}
