package com.sunny;

import com.sunny.downloader.DownloadManager;
import com.sunny.model.Download;
import com.sunny.model.DownloadStatus;

public class Main {

    public static void main(String[] args) {
        DownloadManager manager = null;
        try {
            // Initialize manager
            manager = new DownloadManager();
            System.out.println("Download Manager initialized with In-Memory Coordinator");

            // Create download object
            Download download = new Download();
            download.setFilename( "chetanasaathi.pptx");
            download.setUrl("https://sunnythedeveloper.in/wp-content/uploads/2024/05/chetanasaathi.pptx");
            download.setDownloadPath("C:\\Users\\vknow\\Downloads\\chetanasaathi.pptx");
            download.setThreadCount(4);

            // Add to database
            int downloadId = manager.addDownload(download);
            System.out.println("Download added with ID: " + downloadId);

            // Start download
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
        }
    }
}