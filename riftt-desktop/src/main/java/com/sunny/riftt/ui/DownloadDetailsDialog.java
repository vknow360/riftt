package com.sunny.riftt.ui;

import com.sunny.riftt.downloader.DownloadManager;
import com.sunny.riftt.model.DownloadChunk;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DownloadDetailsDialog extends JDialog {

    private final DownloadManager downloadManager;
    private final int downloadId;
    private final JPanel gridPanel;
    private final Timer refreshTimer;

    public DownloadDetailsDialog(Frame owner, DownloadManager manager, int downloadId, String filename) {
        super(owner, "Details: " + filename, true);
        this.downloadManager = manager;
        this.downloadId = downloadId;

        setLayout(new BorderLayout());
        setSize(600, 400);
        setLocationRelativeTo(owner);

        // Header
        JLabel title = new JLabel("Thread Progress (16 Threads)", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(title, BorderLayout.NORTH);

        // Grid Panel
        gridPanel = new JPanel(new GridLayout(4, 4, 10, 10));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(gridPanel, BorderLayout.CENTER);

        // Initial Layout
        initGrid();

        // Footer
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel footer = new JPanel();
        footer.add(closeButton);
        add(footer, BorderLayout.SOUTH);

        // Refresh Timer (100ms)
        refreshTimer = new Timer(100, e -> updateProgress());
    }

    private void initGrid() {
        // Create placeholders. We'll update them dynamically.
        for (int i = 0; i < 16; i++) {
            JProgressBar pb = new JProgressBar(0, 100);
            pb.setStringPainted(true);
            pb.setString("Connecting...");
            gridPanel.add(pb);
        }
    }

    private void updateProgress() {
        List<DownloadChunk> chunks = downloadManager.getChunks(downloadId);
        Component[] comps = gridPanel.getComponents();

        // If chunks < 16 (e.g. single thread), handle gracefully
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] instanceof JProgressBar) {
                JProgressBar pb = (JProgressBar) comps[i];
                if (i < chunks.size()) {
                    DownloadChunk chunk = chunks.get(i);
                    long total = chunk.getEndByte() - chunk.getStartByte() + 1;
                    long current = chunk.getCurrentOffset() - chunk.getStartByte(); // Approx

                    if (current > total && total != 0) // Cap only if total is known/valid
                        current = total;
                    if (current < 0)
                        current = 0;

                    if (total > 0) {
                        int percent = (int) ((current * 100) / total);
                        pb.setValue(percent);
                        pb.setString(percent + "%");
                    } else {
                        // Unknown size
                        pb.setIndeterminate(true);
                        pb.setString("Downloading...");
                        pb.setValue(0);
                    }
                } else {
                    pb.setValue(0);
                    pb.setString("Inactive");
                }
            }
        }
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            refreshTimer.start();
        } else {
            refreshTimer.stop();
        }
        super.setVisible(b);
    }
}
