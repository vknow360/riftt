package com.sunny.riftt.core;

public interface ILogger {
    void log(String message);

    void error(String message);

    void error(String string, Exception e);
}
