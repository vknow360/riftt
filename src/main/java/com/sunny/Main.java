package com.sunny;

import com.sunny.downloader.DownloadManager;
import com.sunny.model.Download;
import com.sunny.model.DownloadStatus;

public class Main {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        DownloadManager manager = null;
        try {
            manager = new DownloadManager(3, 4);
            System.out.println("Download Manager initialized with In-Memory Coordinator");

            Download download = new Download();
            download.setFilename("500MB.zip");
            download.setUrl("https://mmatechnical.com/Download/Download-Test-File/(MMA)-500MB.zip");
            download.setDownloadPath("C:\\Users\\vknow\\Downloads\\500MB.zip");
            download.setThreadCount(16);


            int downloadId = manager.addDownload(download);
            System.out.println("Download added with ID: " + downloadId);

            manager.startDownload(downloadId);
            System.out.println("Download started!");

            do {
                Thread.sleep(1000);
                double progress = manager.getDownloadProgress(downloadId);
                System.out.printf("Progress: %.2f%%\n", progress);

            } while (manager.getDownloadStatus(downloadId) != DownloadStatus.COMPLETED && manager.getDownloadStatus(downloadId) != DownloadStatus.FAILED);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(manager != null) {
                manager.shutDown();
            }
            long end = System.currentTimeMillis();
            System.out.println(parseTime(end-start));
        }
    }

    private static String parseTime(long time) {
        long seconds = time / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}