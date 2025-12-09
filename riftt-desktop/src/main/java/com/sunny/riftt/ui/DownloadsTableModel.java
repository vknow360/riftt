package com.sunny.riftt.ui;

import com.sunny.riftt.model.Download;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class DownloadsTableModel extends AbstractTableModel {

    private final List<Download> downloadList = new ArrayList<>();
    // 0: Filename, 1: Progress, 2: Size, 3: Status
    private final String[] columnNames = { "Filename", "Progress", "Size", "Status" };

    @Override
    public int getRowCount() {
        return downloadList.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Download download = downloadList.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return download.getFilename();
            case 1:
                return getProgressValue(download);
            case 2:
                return formatSize(download.getFileSize());
            case 3:
                return download.getStatus();
            default:
                return null;
        }
    }

    // Support for JProgressBar renderer
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 1) {
            return Double.class; // Progress
        }
        return String.class;
    }

    public void addDownload(Download download) {
        downloadList.add(download);
        fireTableRowsInserted(downloadList.size() - 1, downloadList.size() - 1);
    }

    public void removeDownload(int id) {
        for (int i = 0; i < downloadList.size(); i++) {
            if (downloadList.get(i).getId() == id) {
                downloadList.remove(i);
                fireTableRowsDeleted(i, i);
                return;
            }
        }
    }

    public void clearAll() {
        int size = downloadList.size();
        if (size > 0) {
            downloadList.clear();
            fireTableDataChanged();
        }
    }

    public void updateDownload(Download download) {
        for (int i = 0; i < downloadList.size(); i++) {
            if (downloadList.get(i).getId() == download.getId()) {
                downloadList.set(i, download);
                fireTableRowsUpdated(i, i);
                return;
            }
        }
    }

    public void updateProgress(int id, long downloadedSize, long totalSize) {
        for (int i = 0; i < downloadList.size(); i++) {
            if (downloadList.get(i).getId() == id) {
                downloadList.get(i).setDownloadedSize(downloadedSize);
                downloadList.get(i).setFileSize(totalSize);

                // Update Progress(1), Size(2)
                fireTableCellUpdated(i, 1);
                fireTableCellUpdated(i, 2);
                return;
            }
        }
    }

    public void updateStatus(int id, com.sunny.riftt.model.DownloadStatus status) {
        for (int i = 0; i < downloadList.size(); i++) {
            if (downloadList.get(i).getId() == id) {
                downloadList.get(i).setStatus(status);
                // Update Status column (3)
                fireTableCellUpdated(i, 3);
                return;
            }
        }
    }

    public Download getDownloadAt(int row) {
        return downloadList.get(row);
    }

    public void setDownloads(List<Download> downloads) {
        this.downloadList.clear();
        this.downloadList.addAll(downloads);
        fireTableDataChanged();
    }

    private double getProgressValue(Download d) {
        if (d.getFileSize() <= 0)
            return 0;
        return (double) d.getDownloadedSize() / d.getFileSize() * 100;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
