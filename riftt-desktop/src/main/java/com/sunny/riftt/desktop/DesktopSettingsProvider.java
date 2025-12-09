package com.sunny.riftt.desktop;

import com.sunny.riftt.core.ISettingsProvider;
import com.sunny.riftt.manager.SettingsManager;

public class DesktopSettingsProvider implements ISettingsProvider {

    @Override
    public int getMaxConcurrentDownloads() {
        return SettingsManager.getInstance().getMaxConcurrentDownloads();
    }

    @Override
    public int getThreadsPerDownload() {
        return SettingsManager.getInstance().getThreadsPerDownload();
    }

    @Override
    public String getDefaultDownloadPath() {
        return SettingsManager.getInstance().getDefaultDownloadPath();
    }

    @Override
    public int getConnectionTimeout() {
        return SettingsManager.getInstance().getConnectionTimeout();
    }
}
