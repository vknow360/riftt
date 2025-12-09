package com.sunny.riftt.ui;

import com.sunny.riftt.downloader.DownloadCallback;
import com.sunny.riftt.downloader.DownloadManager;
import com.sunny.riftt.model.Download;
import com.sunny.riftt.model.DownloadStatus;
import com.sunny.riftt.util.FilenameUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {

    private final DownloadManager downloadManager;
    private final Map<Integer, DownloadCard> cardMap = new HashMap<>();

    private JPanel listPanel;
    private Integer selectedDownloadId = null;

    // Toolbar Buttons
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
        loadExistingDownloads();
    }

    private void initLookAndFeel() {
        try {
            // System Look and Feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // Global fonts
            UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 12));
            UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 12));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initUI() {
        setTitle("riftt - Modern Downloader");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 245, 245)); // Light background
        setLayout(new BorderLayout());

        // --- Toolbar ---
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(Color.WHITE);
        toolBar.setBorder(new EmptyBorder(10, 10, 10, 10));

        addButton = createStyledButton("Add URL", new Color(0, 120, 215));
        pauseButton = createStyledButton("Pause", new Color(200, 200, 200));
        resumeButton = createStyledButton("Resume", new Color(200, 200, 200));
        cancelButton = createStyledButton("Cancel", new Color(200, 200, 200));
        removeButton = createStyledButton("Remove", new Color(200, 200, 200));
        removeAllButton = createStyledButton("Clear All", new Color(220, 50, 50));

        // Fix text color for light buttons
        pauseButton.setForeground(Color.BLACK);
        resumeButton.setForeground(Color.BLACK);
        cancelButton.setForeground(Color.BLACK);
        removeButton.setForeground(Color.BLACK);

        toolBar.add(addButton);
        toolBar.add(Box.createHorizontalStrut(15));
        toolBar.add(pauseButton);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(resumeButton);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(cancelButton);
        toolBar.add(Box.createHorizontalStrut(15));
        toolBar.add(removeButton);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(removeAllButton);

        toolBar.add(Box.createHorizontalGlue()); // Right align settings
        JButton settingsButton = createStyledButton("Settings", new Color(200, 200, 200));
        settingsButton.setForeground(Color.BLACK);
        settingsButton.addActionListener(e -> new SettingsDialog(this).setVisible(true));
        toolBar.add(settingsButton);

        add(toolBar, BorderLayout.NORTH);

        // --- List View (Cards) ---
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(new Color(245, 245, 245));

        // Filler to push items to top
        listPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(new Color(245, 245, 245));

        add(scrollPane, BorderLayout.CENTER);

        // --- Status Bar ---
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBackground(new Color(235, 235, 235));
        JLabel statusLabel = new JLabel(" Select a download to perform actions.");
        statusLabel.setForeground(Color.GRAY);
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);

        initActionListeners();
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE); // Default white text, overridden for light buttons
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 15, 8, 15));
        return btn;
    }

    private void addDownloadCard(Download download) {
        DownloadCard card = new DownloadCard(download);

        // Click listener for selection
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectDownload(download.getId());
                if (e.getClickCount() == 2) {
                    DownloadDetailsDialog dialog = new DownloadDetailsDialog(MainFrame.this, downloadManager,
                            download.getId(), download.getFilename());
                    dialog.setVisible(true);
                }
            }
        });

        // Insert at top (index 0), keeping glue at bottom
        listPanel.add(card, 0);
        listPanel.revalidate();
        listPanel.repaint();

        cardMap.put(download.getId(), card);
    }

    private void selectDownload(int id) {
        this.selectedDownloadId = id;
        // Visual feedback could be added to cards here (e.g. border highlight)
        // For simplicity, just tracking state
    }

    private void initActionListeners() {
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
                        return FilenameUtils.resolveFilename(url);
                    }

                    @Override
                    protected void done() {
                        try {
                            String filename = get();
                            if (filename == null || filename.isEmpty())
                                filename = "download.file";

                            // Unique Filename Logic
                            File file = new File(path, filename);
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
                                file = new File(path, newName);
                                counter++;
                            }

                            filename = file.getName();
                            String fullPath = file.getAbsolutePath();

                            Download download = new Download();
                            download.setUrl(url);
                            download.setDownloadPath(fullPath);
                            download.setFilename(filename);
                            download.setThreadCount(16);

                            // Manager
                            int id = downloadManager.addDownload(download, createCallback());

                            // UI
                            addDownloadCard(download);

                            // Start
                            downloadManager.startDownload(id);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(MainFrame.this, "Error: " + ex.getMessage());
                        }
                    }
                }.execute();
            }
        });

        pauseButton.addActionListener(e -> {
            if (selectedDownloadId != null) {
                try {
                    downloadManager.pauseDownload(selectedDownloadId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        resumeButton.addActionListener(e -> {
            if (selectedDownloadId != null)
                downloadManager.resumeDownload(selectedDownloadId);
        });

        cancelButton.addActionListener(e -> {
            if (selectedDownloadId != null)
                downloadManager.cancelDownload(selectedDownloadId);
        });

        removeButton.addActionListener(e -> {
            if (selectedDownloadId != null) {
                downloadManager.removeDownload(selectedDownloadId);
                DownloadCard card = cardMap.remove(selectedDownloadId);
                if (card != null) {
                    listPanel.remove(card);
                    listPanel.revalidate();
                    listPanel.repaint();
                }
                selectedDownloadId = null;
            }
        });

        removeAllButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Remove ALL downloads? This cannot be undone.", "Confirm Remove All", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                downloadManager.removeAllDownloads();
                listPanel.removeAll();
                listPanel.add(Box.createVerticalGlue()); // Restore glue
                cardMap.clear();
                selectedDownloadId = null;
                listPanel.revalidate();
                listPanel.repaint();
            }
        });
    }

    private void loadExistingDownloads() {
        List<Download> downloads = downloadManager.getAllDownloads();
        for (Download d : downloads) {
            addDownloadCard(d);
            if (d.getStatus() != DownloadStatus.COMPLETED && d.getStatus() != DownloadStatus.CANCELED) {
                downloadManager.registerCallback(d.getId(), createCallback());
            }
        }
    }

    private DownloadCallback createCallback() {
        return new DownloadCallback() {
            @Override
            public void onStart(int id) {
                SwingUtilities.invokeLater(() -> updateCard(id, c -> c.updateStatus(DownloadStatus.DOWNLOADING)));
            }

            @Override
            public void onPause(int id) {
                SwingUtilities.invokeLater(() -> updateCard(id, c -> c.updateStatus(DownloadStatus.PAUSED)));
            }

            @Override
            public void onResume(int id) {
                SwingUtilities.invokeLater(() -> updateCard(id, c -> c.updateStatus(DownloadStatus.DOWNLOADING)));
            }

            @Override
            public void onProgress(int id, long downloaded, long total, double progress) {
                SwingUtilities.invokeLater(() -> updateCard(id, c -> c.updateProgress(downloaded, total)));
            }

            @Override
            public void onDownloadCompleted(int id) {
                SwingUtilities.invokeLater(() -> updateCard(id, c -> c.updateStatus(DownloadStatus.COMPLETED)));
            }

            @Override
            public void onDownloadFailed(int id, String message) {
                SwingUtilities.invokeLater(() -> updateCard(id, c -> c.updateStatus(DownloadStatus.FAILED)));
            }

            @Override
            public void onDownloadCancelled(int id) {
                SwingUtilities.invokeLater(() -> updateCard(id, c -> c.updateStatus(DownloadStatus.CANCELED)));
            }
        };
    }

    private void updateCard(int id, java.util.function.Consumer<DownloadCard> action) {
        DownloadCard c = cardMap.get(id);
        if (c != null)
            action.accept(c);
    }

    public void showFrame() {
        setVisible(true);
    }
}
