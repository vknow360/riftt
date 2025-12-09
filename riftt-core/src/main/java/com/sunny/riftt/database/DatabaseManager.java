package com.sunny.riftt.database;

import com.sunny.riftt.utils.FileUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static DatabaseManager instance;

    private static final String DB_NAME = "test.db";

    private static String DB_PATH = "";

    private Connection connection;

    private DatabaseManager() {
        DB_PATH = getDBPath();
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                synchronized (DatabaseManager.class) {
                    if (connection == null || connection.isClosed()) {
                        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void initializeDatabase() {

        String sql = "CREATE TABLE IF NOT EXISTS downloads (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    filename VARCHAR(255),\n" +
                "    url TEXT NOT NULL,\n" +
                "    file_size LONG,\n" +
                "    downloaded_size LONG DEFAULT 0,\n" +
                "    status VARCHAR(20),\n" +
                "    download_path TEXT,\n" +
                "    start_time TIMESTAMP,\n" +
                "    end_time TIMESTAMP,\n" +
                "    thread_count INTEGER DEFAULT 1\n" +
                ");";

        String chunksSql = "CREATE TABLE IF NOT EXISTS download_chunks (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    download_id INTEGER,\n" +
                "    start_byte LONG,\n" +
                "    end_byte LONG,\n" +
                "    current_offset LONG,\n" +
                "    status VARCHAR(20),\n" +
                "    FOREIGN KEY(download_id) REFERENCES downloads(id) ON DELETE CASCADE\n" +
                ");";

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
            stmt.execute(chunksSql);
        } catch (Exception e) {
            System.err.println("[DatabaseManager] DB Init Error: " + e.getMessage());
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
            instance.initializeDatabase();
        }
        return instance;
    }

    private String getDBPath() {
        String appDataPath = FileUtils.getAppDataDirectory();
        return appDataPath + File.separator + DB_NAME;
    }
}
