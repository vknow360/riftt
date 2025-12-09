package com.sunny.ui;

import com.sunny.downloader.DownloadCallback;
import com.sunny.downloader.DownloadManager;
import com.sunny.model.Download;
import com.sunny.model.DownloadStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class MainFrame extends JFrame {

    private JTable downloadTable;
    private DownloadsTableModel tableModel;
    private DownloadManager downloadManager;

    private JButton addButton;
    private JButton pauseButton;
    private JButton resumeButton;
    private JButton cancelButton;
    private JButton removeButton;
    private JButton removeAllButton;

    public MainFrame(DownloadManager manager) {
        this.downloadManager = manager;
        initLookAndFeel();
        initUI();
        initListeners();
        loadExistingDownloads();
    }

    private void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initUI() {
        setTitle("FluxDL - Advanced Download Manager");
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Toolbar ---
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(5, 5, 5, 5));
        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        addButton = createToolbarButton("Add URL", "Add a new download");
        pauseButton = createToolbarButton("Pause", "Pause selected");
        resumeButton = createToolbarButton("Resume", "Resume selected");
        cancelButton = createToolbarButton("Cancel", "Cancel selected");
        removeButton = createToolbarButton("Remove", "Remove selected");
        removeAllButton = createToolbarButton("Remove All", "Clear all downloads");

        toolBar.add(addButton);
        toolBar.addSeparator(new Dimension(10, 0));
        toolBar.add(pauseButton);
        toolBar.add(resumeButton);
        toolBar.add(cancelButton);
        toolBar.addSeparator(new Dimension(10, 0));
        toolBar.add(removeButton);
        toolBar.add(removeAllButton);
        toolBar.addSeparator(new Dimension(10, 0));
        JButton settingsButton = createToolbarButton("Settings", "Open Settings");
        settingsButton.addActionListener(e -> new SettingsDialog(this).setVisible(true));
        toolBar.add(settingsButton);

        add(toolBar, BorderLayout.NORTH);

        // --- Table ---
        tableModel = new DownloadsTableModel();
        downloadTable = new JTable(tableModel);

        // UI Enhancements
        downloadTable.setRowHeight(35);
        downloadTable.setShowGrid(false);
        downloadTable.setIntercellSpacing(new Dimension(0, 5));
        downloadTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        downloadTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        downloadTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));

        // Progress Renderer (Column 1)
        downloadTable.getColumnModel().getColumn(1).setCellRenderer(new ProgressBarRenderer());

        JScrollPane scrollPane = new JScrollPane(downloadTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // --- Status Bar ---
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel statusLabel = new JLabel("Double-click a download to view thread details.");
        statusLabel.setForeground(Color.GRAY);
        statusBar.add(statusLabel);
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        add(statusBar, BorderLayout.SOUTH);
    }

    private JButton createToolbarButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(5, 5, 5, 5));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return btn;
    }

    private void initListeners() {
        // Double-click Config
        downloadTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = downloadTable.getSelectedRow();
                    if (row >= 0) {
                        Download d = tableModel.getDownloadAt(row);
                        openDetailsDialog(d);
                    }
                }
            }
        });

        addButton.addActionListener(e -> {
            AddDownloadDialog dialog = new AddDownloadDialog(this);
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                String url = dialog.getUrl();
                String path = dialog.getSaveDir();
                if (url.isEmpty() || path.isEmpty())
                    return;

                new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        return com.sunny.util.FilenameUtils.resolveFilename(url);
                    }

                    @Override
                    protected void done() {
                        try {
                            String filename = get();
                            if (filename == null || filename.isEmpty()) {
                                filename = "download.file";
                            }

                            // Unique Filename Logic
                            java.io.File file = new java.io.File(path, filename);
                            String nameWithoutExt = filename;
                            String ext = "";
                            int dotIndex = filename.lastIndexOf('.');
                            if (dotIndex > 0) {
                                nameWithoutExt = filename.substring(0, dotIndex);
                                ext = filename.substring(dotIndex);
                            }

                            int counter = 1;
                            while (file.exists()) {
                                String newName = nameWithoutExt + " (" + counter + ")" + ext;
                                file = new java.io.File(path, newName);
                                counter++;
                            }

                            filename = file.getName();
                            String fullPath = file.getAbsolutePath();

                            Download download = new Download();
                            download.setUrl(url);
                            download.setDownloadPath(fullPath);
                            download.setFilename(filename);
                            download.setThreadCount(16);

                            // Add to Manager and Get ID
                            int id = downloadManager.addDownload(download, createCallback());

                            // Add to Table
                            tableModel.addDownload(download);

                            // Start
                            downloadManager.startDownload(id);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(MainFrame.this,
                                    "Error resolving filename: " + ex.getMessage());
                        }
                    }
                }.execute();
            }
        });

        pauseButton.addActionListener(e -> {
            int row = downloadTable.getSelectedRow();
            if (row >= 0) {
                Download d = tableModel.getDownloadAt(row);
                try {
                    downloadManager.pauseDownload(d.getId());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error pausing: " + ex.getMessage());
                }
            }
        });

        resumeButton.addActionListener(e -> {
            int row = downloadTable.getSelectedRow();
            if (row >= 0) {
                Download d = tableModel.getDownloadAt(row);
                downloadManager.resumeDownload(d.getId());
            }
        });

        cancelButton.addActionListener(e -> {
            int row = downloadTable.getSelectedRow();
            if (row >= 0) {
                Download d = tableModel.getDownloadAt(row);
                downloadManager.cancelDownload(d.getId());
            }
        });

        removeButton.addActionListener(e -> {
            int row = downloadTable.getSelectedRow();
            if (row >= 0) {
                Download d = tableModel.getDownloadAt(row);
                downloadManager.removeDownload(d.getId());
                tableModel.removeDownload(d.getId());
            } else {
                JOptionPane.showMessageDialog(this, "Select a download to remove.");
            }
        });

        removeAllButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Remove ALL downloads? This cannot be undone.", "Confirm Remove All", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                downloadManager.removeAllDownloads();
                tableModel.clearAll();
            }
        });
    }

    private void openDetailsDialog(Download d) {
        DownloadDetailsDialog dialog = new DownloadDetailsDialog(this, downloadManager, d.getId(), d.getFilename());
        dialog.setVisible(true);
    }

    private void loadExistingDownloads() {
        List<Download> downloads = downloadManager.getAllDownloads();
        tableModel.setDownloads(downloads);

        for (Download d : downloads) {
            if (d.getStatus() != DownloadStatus.COMPLETED && d.getStatus() != DownloadStatus.CANCELED) {
                downloadManager.registerCallback(d.getId(), createCallback());
            }
        }
    }

    private DownloadCallback createCallback() {
        return new DownloadCallback() {
            @Override
            public void onStart(int id) {
                SwingUtilities.invokeLater(() -> tableModel.updateStatus(id, DownloadStatus.DOWNLOADING));
            }

            @Override
            public void onPause(int id) {
                SwingUtilities.invokeLater(() -> tableModel.updateStatus(id, DownloadStatus.PAUSED));
            }

            @Override
            public void onResume(int id) {
                SwingUtilities.invokeLater(() -> tableModel.updateStatus(id, DownloadStatus.DOWNLOADING));
            }

            @Override
            public void onProgress(int id, long downloaded, long total, double progress) {
                SwingUtilities.invokeLater(() -> tableModel.updateProgress(id, downloaded, total));
            }

            @Override
            public void onDownloadCompleted(int id) {
                SwingUtilities.invokeLater(() -> {
                    tableModel.updateStatus(id, DownloadStatus.COMPLETED);
                });
            }

            @Override
            public void onDownloadFailed(int id, String message) {
                SwingUtilities.invokeLater(() -> tableModel.updateStatus(id, DownloadStatus.FAILED));
            }

            @Override
            public void onDownloadCancelled(int id) {
                SwingUtilities.invokeLater(() -> tableModel.updateStatus(id, DownloadStatus.CANCELED));
            }
        };
    }

    public void showFrame() {
        setVisible(true);
    }
}
