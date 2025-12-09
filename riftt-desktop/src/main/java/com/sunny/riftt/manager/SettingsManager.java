package com.sunny.riftt.manager;

import java.util.prefs.Preferences;

public class SettingsManager {

    private static SettingsManager instance;
    private final Preferences prefs;

    // Keys
    private static final String KEY_MAX_CONCURRENT = "max_concurrent_downloads";
    private static final String KEY_THREADS_PER_DOWNLOAD = "threads_per_download";
    private static final String KEY_DEFAULT_PATH = "default_download_path";
    private static final String KEY_TIMEOUT = "connection_timeout";

    // Defaults
    private static final int DIS_MAX_CONCURRENT = 3;
    private static final int DEF_THREADS = 16;
    private static final String DEF_PATH = System.getProperty("user.home") + java.io.File.separator + "Downloads";
    private static final int DEF_TIMEOUT = 10000;

    private SettingsManager() {
        prefs = Preferences.userNodeForPackage(SettingsManager.class);
    }

    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    public int getMaxConcurrentDownloads() {
        return prefs.getInt(KEY_MAX_CONCURRENT, DIS_MAX_CONCURRENT);
    }

    public void setMaxConcurrentDownloads(int value) {
        prefs.putInt(KEY_MAX_CONCURRENT, value);
    }

    public int getThreadsPerDownload() {
        return prefs.getInt(KEY_THREADS_PER_DOWNLOAD, DEF_THREADS);
    }

    public void setThreadsPerDownload(int value) {
        prefs.putInt(KEY_THREADS_PER_DOWNLOAD, value);
    }

    public String getDefaultDownloadPath() {
        return prefs.get(KEY_DEFAULT_PATH, DEF_PATH);
    }

    public void setDefaultDownloadPath(String path) {
        if (path != null) {
            prefs.put(KEY_DEFAULT_PATH, path);
        }
    }

    public int getConnectionTimeout() {
        return prefs.getInt(KEY_TIMEOUT, DEF_TIMEOUT);
    }

    public void setConnectionTimeout(int value) {
        prefs.putInt(KEY_TIMEOUT, value);
    }
}
