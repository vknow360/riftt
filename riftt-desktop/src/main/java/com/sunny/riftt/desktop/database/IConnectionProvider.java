package com.sunny.riftt.desktop.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface IConnectionProvider {
    Connection getConnection() throws SQLException;
}
