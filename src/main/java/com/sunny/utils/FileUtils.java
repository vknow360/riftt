package com.sunny.utils;

import java.io.File;

public class FileUtils {

    private static final String APP_NAME = "fluxDL";
    private static final String INVALID_FILENAME_CHARS = "[\\\\/:*?\"<>|]";
    private static final int MAX_FILENAME_LENGTH = 255;

    public static boolean isValidFilename(String file){
        if (file == null || file.trim().isEmpty()) {
            return false;
        }
        if (file.length() > MAX_FILENAME_LENGTH) {
            return false;
        }
        return !file.matches(".*" + INVALID_FILENAME_CHARS + ".*");
    }

    public static boolean hasWritePermission(String dir){
        return new File(dir).canWrite();
    }

    /*
        Returns free space in KB
     */
    public static Long getAvailableSpace(String dir) {
        return new File(dir).getFreeSpace()/1024;
    }

    public static void createDirectoryIfNotExists(String dir){
        File file = new File(dir);
        if(!file.exists()){
            file.mkdirs();
        }
    }

    public static String getAppDataDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        String appDataPath;

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData == null || appData.isEmpty()) {
                appData = userHome + File.separator + "AppData" + File.separator + "Roaming";
            }
            appDataPath = appData + File.separator + APP_NAME;
        } else if (os.contains("mac")) {
            appDataPath = userHome + File.separator + "Library" + File.separator + "Application Support" + File.separator + APP_NAME;
        } else {
            appDataPath = userHome + File.separator + "." + APP_NAME.toLowerCase();
        }

        File appDir = new File(appDataPath);
        if (!appDir.exists()) {
            if (!appDir.mkdirs()) {
                throw new RuntimeException("Failed to create app data directory: " + appDataPath);
            }
        }

        return appDataPath;
    }
}
