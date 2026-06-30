package com.echo.service.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Resolves the OS-native app-data location for HappyCamper's persisted config. */
public final class ConfigLocation {

    public static final String PRESETS_FILE = "view-presets.json";

    private ConfigLocation() {}

    /** Pure: decide the config directory from inputs, with no filesystem access. */
    public static Path resolveDir(String osName, Map<String, String> env, Path home) {
        String os = osName == null ? "" : osName.toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            return home.resolve("Library").resolve("Application Support").resolve("HappyCamper");
        }
        if (os.contains("win")) {
            String appData = env.get("APPDATA");
            Path base = (appData != null && !appData.isBlank()) ? Path.of(appData) : home;
            return base.resolve("HappyCamper");
        }
        String xdg = env.get("XDG_CONFIG_HOME");
        Path base = (xdg != null && !xdg.isBlank()) ? Path.of(xdg) : home.resolve(".config");
        return base.resolve("happycamper");
    }

    /** Production accessor: resolves, creates the directory, returns the presets file path. */
    public static Path presetsFilePath() {
        Path dir = resolveDir(System.getProperty("os.name"),
                              System.getenv(),
                              Path.of(System.getProperty("user.home")));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("[HappyCamper] could not create config dir " + dir + ": " + e.getMessage());
        }
        return dir.resolve(PRESETS_FILE);
    }
}
