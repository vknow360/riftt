package com.sunny.riftt;

import com.sunny.riftt.core.IChunkRepository;
import com.sunny.riftt.core.IDownloadRepository;
import com.sunny.riftt.core.ILogger;
import com.sunny.riftt.core.ISettingsProvider;
import com.sunny.riftt.desktop.DesktopLogger;
import com.sunny.riftt.desktop.DesktopSettingsProvider;
import com.sunny.riftt.desktop.repository.JdbcChunkRepository;
import com.sunny.riftt.desktop.repository.JdbcDownloadRepository;
import com.sunny.riftt.downloader.DownloadManager;
import com.sunny.riftt.ui.MainFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        // Use FlatLaf if available, otherwise System L&F
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // ignored.printStackTrace();
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