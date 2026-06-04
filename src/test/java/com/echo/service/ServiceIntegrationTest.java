package com.echo.service;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.echo.automation.TestPreset;
import com.echo.domain.ActivityRoster;
import com.echo.domain.CamperRoster;
import com.echo.domain.EnhancedRoster;

public class ServiceIntegrationTest {

    private final ImportService importService = new ImportService();
    private final ExportService exportService = new ExportService();
    private final RosterService rosterService = new RosterService(importService, exportService);

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @EnumSource(value = TestPreset.class, names = {"MINI_NORMAL"})
    @DisplayName("Should perform basic import workflow")
    void basicImportWorkflowTest(TestPreset preset) throws Exception {
        // Given
        File camperFile = preset.getCamperFile();
        File activityFile = preset.getActivityFile();

        // When - Import
        CamperRoster camperRoster = importService.importCamperRoster(camperFile);
        ActivityRoster activityRoster = importService.importActivityRoster(activityFile);

        // Then
        assertNotNull(camperRoster);
        assertNotNull(activityRoster);
        assertFalse(camperRoster.getCampers().isEmpty());
        assertFalse(activityRoster.getCampers().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = TestPreset.class, names = {"MINI_NORMAL"})
    @DisplayName("Should perform export workflow")
    void exportWorkflowTest(TestPreset preset) throws Exception {
        // Given
        File camperFile = preset.getCamperFile();
        CamperRoster camperRoster = importService.importCamperRoster(camperFile);
        File outputFile = tempDir.resolve("export_output.csv").toFile();

        // Create an enhanced roster from the camper roster
        EnhancedRoster enhancedRoster = new EnhancedRoster();
        for (String header : camperRoster.getHeaderMap().keySet()) {
            enhancedRoster.addHeader(header);
        }
        for (com.echo.domain.Camper camper : camperRoster.getCampers()) {
            enhancedRoster.addCamper(camper);
        }

        // When - Export
        ExportSettings settings = new ExportSettings(outputFile);
        settings.setShowAllColumns(true).setShowAllRows(true);
        rosterService.exportRoster(enhancedRoster, null, settings);

        // Then
        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
    }

    @Test
    @DisplayName("Should handle error conditions gracefully")
    void errorHandlingTest() {
        // Given - Invalid file
        File invalidCamperFile = new File("non_existent_file.csv");

        // When/Then - Import should fail gracefully
        Exception exception = assertThrows(Exception.class, () -> {
            importService.importCamperRoster(invalidCamperFile);
        });

        assertNotNull(exception);
    }
}