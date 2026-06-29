package com.echo.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigLocationTest {
    private static final Path HOME = Path.of("/home/user");

    @Test void macUsesApplicationSupport() {
        Path dir = ConfigLocation.resolveDir("Mac OS X", Map.of(), HOME);
        assertEquals(Path.of("/home/user/Library/Application Support/HappyCamper"), dir);
    }

    @Test void windowsUsesAppData() {
        Path dir = ConfigLocation.resolveDir("Windows 11",
            Map.of("APPDATA", "C:\\Users\\u\\AppData\\Roaming"), HOME);
        assertEquals(Path.of("C:\\Users\\u\\AppData\\Roaming", "HappyCamper"), dir);
    }

    @Test void windowsFallsBackToHomeWhenAppDataMissing() {
        Path dir = ConfigLocation.resolveDir("Windows 10", Map.of(), HOME);
        assertEquals(HOME.resolve("HappyCamper"), dir);
    }

    @Test void linuxUsesXdgWhenSet() {
        Path dir = ConfigLocation.resolveDir("Linux",
            Map.of("XDG_CONFIG_HOME", "/home/user/.config"), HOME);
        assertEquals(Path.of("/home/user/.config", "happycamper"), dir);
    }

    @Test void linuxFallsBackToDotConfig() {
        Path dir = ConfigLocation.resolveDir("Linux", Map.of(), HOME);
        assertEquals(HOME.resolve(".config").resolve("happycamper"), dir);
    }
}
