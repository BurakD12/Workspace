package com.library.util;

import java.sql.Connection;
import java.sql.SQLException;

import com.library.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class ConnectionPool {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(AppConfig.getUrl());
        config.setUsername(AppConfig.getUser());
        config.setPassword(AppConfig.getPassword());
        config.setMaximumPoolSize(AppConfig.getPoolSize());
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        dataSource = new HikariDataSource(config);
    }

    private ConnectionPool(){}

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
