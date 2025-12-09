package com.sunny.riftt.ui;

import com.sunny.riftt.model.Download;
import com.sunny.riftt.model.DownloadStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class DownloadCard extends JPanel {

    private final int downloadId;
    private final JLabel nameLabel;
    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private final JLabel sizeLabel;

    // Gradient colors (Light Mode)
    private static final Color CARD_BG_START = new Color(255, 255, 255);
    private static final Color CARD_BG_END = new Color(245, 245, 245);
    private static final Color ACCENT_COLOR = new Color(0, 120, 215);

    public DownloadCard(Download download) {
        this.downloadId = download.getId();

        setLayout(new BorderLayout(10, 5)); // Reduce vertical gap
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // --- Header (Name + Status) ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        nameLabel = new JLabel(download.getFilename());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(new Color(50, 50, 50));

        statusLabel = new JLabel(download.getStatus().name());
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground(new Color(100, 100, 100));
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        topPanel.add(nameLabel, BorderLayout.CENTER);
        topPanel.add(statusLabel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // --- Center (Progress Bar Wrapped) ---
        // Wrap in a panel to respect preferred height
        JPanel progressWrapper = new JPanel(new GridBagLayout());
        progressWrapper.setOpaque(false);

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(100, 8)); // Slightly thicker for visibility
        progressBar.setForeground(Color.CYAN);
        progressBar.setBackground(new Color(220, 220, 220)); // Darker gray for contrast
        progressBar.setBorderPainted(false);

        if (download.getFileSize() > 0) {
            int p = (int) ((download.getDownloadedSize() * 100) / download.getFileSize());
            progressBar.setValue(p);
        } else {
            progressBar.setIndeterminate(true);
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        progressWrapper.add(progressBar, gbc);

        add(progressWrapper, BorderLayout.CENTER);

        // --- Footer (Size Info) ---
        sizeLabel = new JLabel(formatSize(download.getDownloadedSize()) + " / " + formatSize(download.getFileSize()));
        sizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sizeLabel.setForeground(new Color(100, 100, 100));
        add(sizeLabel, BorderLayout.SOUTH);

        // Fixed height for list item
        setPreferredSize(new Dimension(0, 80));
        setMaximumSize(new Dimension(9999, 80));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Card Background (Rounded Gradient)
        GradientPaint gp = new GradientPaint(0, 0, CARD_BG_START, 0, getHeight(), CARD_BG_END);
        g2.setPaint(gp);
        g2.fill(new RoundRectangle2D.Double(5, 2, getWidth() - 10, getHeight() - 4, 15, 15));

        // Border
        g2.setColor(new Color(200, 200, 200));
        g2.draw(new RoundRectangle2D.Double(5, 2, getWidth() - 10, getHeight() - 4, 15, 15));

        g2.dispose();
        super.paintComponent(g);
    }

    public void updateProgress(long downloaded, long total) {
        if (total > 0) {
            progressBar.setIndeterminate(false);
            int p = (int) ((downloaded * 100) / total);
            progressBar.setValue(p);
            sizeLabel.setText(formatSize(downloaded) + " / " + formatSize(total));
        } else {
            progressBar.setIndeterminate(true);
            sizeLabel.setText(formatSize(downloaded) + " / Unknown");
        }
    }

    public void updateStatus(DownloadStatus status) {
        statusLabel.setText(status.name());
        if (status == DownloadStatus.FAILED) {
            statusLabel.setForeground(new Color(200, 50, 50));
        } else if (status == DownloadStatus.COMPLETED) {
            statusLabel.setForeground(new Color(0, 150, 0));
        } else {
            statusLabel.setForeground(new Color(100, 100, 100));
        }
    }

    private String formatSize(long bytes) {
        if (bytes == -1)
            return "Unknown";
        if (bytes < 1024)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public int getDownloadId() {
        return downloadId;
    }
}
