package com.sunny;

import com.sunny.core.IChunkRepository;
import com.sunny.core.IDownloadRepository;
import com.sunny.core.ILogger;
import com.sunny.core.ISettingsProvider;
import com.sunny.desktop.DesktopLogger;
import com.sunny.desktop.DesktopSettingsProvider;
import com.sunny.desktop.JdbcChunkRepository;
import com.sunny.desktop.JdbcDownloadRepository;
import com.sunny.downloader.DownloadManager;
import com.sunny.ui.MainFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        // Use FlatLaf if available, otherwise System L&F
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            // 1. Dependency Injection Setup
            IDownloadRepository downloadRepo = new JdbcDownloadRepository();
            IChunkRepository chunkRepo = new JdbcChunkRepository();
            ISettingsProvider settings = new DesktopSettingsProvider();
            ILogger logger = new DesktopLogger();

            // 2. Create Core Manager with Dependencies
            DownloadManager manager = new DownloadManager(downloadRepo, chunkRepo, settings, logger);

            // 3. Create UI
            MainFrame frame = new MainFrame(manager);
            frame.showFrame();

            Runtime.getRuntime().addShutdownHook(new Thread(manager::shutDown));
        });
    }
}