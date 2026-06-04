package com.echo.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.filter.FilterManager;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ExportServiceTest {

    private final ExportService exportService = new ExportService();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should export roster to CSV file")
    void exportRosterToCSVTest() throws Exception {
        // Given
        EnhancedRoster roster = createTestRoster();
        File outputFile = tempDir.resolve("test_export.csv").toFile();

        // When
        ExportSettings settings = new ExportSettings(outputFile);
        settings.setShowAllColumns(true).setShowAllRows(true);

        // Then - Should not throw exception
        assertDoesNotThrow(() -> {
            exportService.exportRosterToCSV(roster, null, settings);
        });

        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
    }

    @Test
    @DisplayName("Should export roster with specific settings")
    void exportRosterWithSettingsTest() throws Exception {
        // Given
        EnhancedRoster roster = createTestRoster();
        File outputFile = tempDir.resolve("test_export_settings.csv").toFile();

        // When
        ExportSettings settings = new ExportSettings(outputFile);
        settings.setShowAllColumns(false)
               .setShowAllRows(false)
               .setUseEmptyPlaceholder(true);

        // Then - Should not throw exception
        assertDoesNotThrow(() -> {
            exportService.exportRosterToCSV(roster, null, settings);
        });

        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
    }

    /**
     * Helper method to create a test roster
     */
    private EnhancedRoster createTestRoster() {
        EnhancedRoster roster = new EnhancedRoster();

        // Add headers
        roster.addHeader(RosterHeader.FIRST_NAME.standardName);
        roster.addHeader(RosterHeader.LAST_NAME.standardName);
        roster.addHeader(RosterHeader.PREFERENCE_SCORE.standardName);
        roster.addHeader(RosterHeader.PROGRAM.standardName);

        // Add test campers
        for (int i = 0; i < 2; i++) {
            Map<String, String> camperData = new HashMap<>();
            camperData.put(RosterHeader.FIRST_NAME.standardName, "Test" + i);
            camperData.put(RosterHeader.LAST_NAME.standardName, "User" + i);
            camperData.put(RosterHeader.PREFERENCE_SCORE.standardName, "95");
            camperData.put(RosterHeader.PROGRAM.standardName, "Traditional Camp");

            com.echo.domain.Camper camper = new com.echo.domain.Camper("test-id-" + i, camperData);
            roster.addCamper(camper);
        }

        return roster;
    }
}