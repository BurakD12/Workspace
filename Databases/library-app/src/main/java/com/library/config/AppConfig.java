package com.library.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties props;
    static {
        props = new Properties();
        try (InputStream is = ClassLoader.getSystemResourceAsStream("application.properties")) {
            if (is == null) {
                throw new RuntimeException("application.properties not found on classpath");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    private AppConfig(){}

    public static String getUrl() {
        return props.getProperty("db.url");
    }

    public static String getUser() {
        return props.getProperty("db.user");
    }

    public static String getPassword() {
        return props.getProperty("db.password");
    }

    public static int getPoolSize() {
        return Integer.parseInt(props.getProperty("db.pool.size"));
    }
}
