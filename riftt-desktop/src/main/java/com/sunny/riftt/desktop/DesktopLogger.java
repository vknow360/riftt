package com.sunny.riftt.desktop;

import com.sunny.riftt.core.ILogger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DesktopLogger implements ILogger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void log(String message) {
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] [INFO] " + message);
    }

    @Override
    public void error(String message) {
        System.err.println("[" + LocalDateTime.now().format(formatter) + "] [ERROR] " + message);
    }

    @Override
    public void error(String string, Exception e) {
        System.err
                .println("[" + LocalDateTime.now().format(formatter) + "] [ERROR] " + string + " - " + e.getMessage());
    }
}
