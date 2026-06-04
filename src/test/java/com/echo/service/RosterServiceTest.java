package com.echo.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.echo.automation.TestPreset;
import com.echo.domain.ActivityRoster;
import com.echo.domain.Camper;
import com.echo.domain.CamperRoster;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.filter.AssignmentFilter;
import com.echo.filter.FilterManager;
import com.echo.logging.RosterException;

/**
 * Tests for the RosterService class, focusing on end-to-end functionality.
 * These tests verify the core operations of importing, processing, and exporting roster data.
 */
public class RosterServiceTest {

    private final ImportService importService = new ImportService();
    private final ExportService exportService = new ExportService();
    private final RosterService rosterService = new RosterService(importService, exportService);

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @EnumSource(value = TestPreset.class, names = {"MINI_NORMAL"})
    @DisplayName("Imports both camper and activity rosters from CSV files")
    void importRostersTest(TestPreset preset) throws Exception {
        // Load test files from the preset
        File camperFile = preset.getCamperFile();
        File activityFile = preset.getActivityFile();

        // Import the roster data from CSV files
        CamperRoster camperRoster = importService.importCamperRoster(camperFile);
        ActivityRoster activityRoster = importService.importActivityRoster(activityFile);

        // Verify the imported data contains campers
        assertNotNull(camperRoster);
        assertNotNull(activityRoster);
        assertFalse(camperRoster.getCampers().isEmpty());
        assertFalse(activityRoster.getCampers().isEmpty());

        // Verify required headers are present
        assertTrue(camperRoster.getAllHeaders().containsAll(CamperRoster.getRequiredHeaders()));
        assertTrue(activityRoster.getAllHeaders().containsAll(ActivityRoster.getRequiredHeaders()));
    }

    @Test
    @DisplayName("Creates and manipulates an enhanced roster with camper data")
    void createEnhancedRosterTest() {
        // Initialize a new enhanced roster
        EnhancedRoster roster = new EnhancedRoster();

        // Configure roster with essential headers
        roster.addHeader(RosterHeader.FIRST_NAME.standardName);
        roster.addHeader(RosterHeader.LAST_NAME.standardName);
        roster.addHeader(RosterHeader.GRADE.standardName);
        roster.addHeader("Session"); // Using string directly since SESSION is not in RosterHeader enum

        // Create test campers with different data
        Map<String, String> camper1Data = new HashMap<>();
        camper1Data.put(RosterHeader.FIRST_NAME.standardName, "John");
        camper1Data.put(RosterHeader.LAST_NAME.standardName, "Doe");
        camper1Data.put(RosterHeader.GRADE.standardName, "5");
        camper1Data.put("Session", "Session 1");

        Map<String, String> camper2Data = new HashMap<>();
        camper2Data.put(RosterHeader.FIRST_NAME.standardName, "Jane");
        camper2Data.put(RosterHeader.LAST_NAME.standardName, "Smith");
        camper2Data.put(RosterHeader.GRADE.standardName, "6");
        camper2Data.put("Session", "Session 2");

        // Add campers to the roster
        Camper camper1 = new Camper("test-id-1", camper1Data);
        Camper camper2 = new Camper("test-id-2", camper2Data);
        roster.addCamper(camper1);
        roster.addCamper(camper2);

        // Verify roster contains the expected data
        assertNotNull(roster);
        assertEquals(2, roster.getCampers().size());

        // Verify we can retrieve specific camper data
        List<Camper> campers = roster.getCampers();
        boolean foundJohn = false;
        boolean foundJane = false;

        for (Camper camper : campers) {
            String firstName = camper.getValue(RosterHeader.FIRST_NAME.standardName);
            if ("John".equals(firstName)) {
                assertEquals("Doe", camper.getValue(RosterHeader.LAST_NAME.standardName));
                assertEquals("5", camper.getValue(RosterHeader.GRADE.standardName));
                foundJohn = true;
            } else if ("Jane".equals(firstName)) {
                assertEquals("Smith", camper.getValue(RosterHeader.LAST_NAME.standardName));
                assertEquals("6", camper.getValue(RosterHeader.GRADE.standardName));
                foundJane = true;
            }
        }

        assertTrue(foundJohn, "John Doe should be in the roster");
        assertTrue(foundJane, "Jane Smith should be in the roster");
    }

    @Test
    @DisplayName("Exports roster to CSV file with configurable settings")
    void exportRosterTest() throws Exception {
        // Create a roster with test data
        EnhancedRoster roster = new EnhancedRoster();

        // Configure roster headers
        List<String> headers = Arrays.asList(
            RosterHeader.FIRST_NAME.standardName,
            RosterHeader.LAST_NAME.standardName,
            RosterHeader.GRADE.standardName,
            "Session" // Using string directly since SESSION is not in RosterHeader enum
        );

        for (String header : headers) {
            roster.addHeader(header);
        }

        // Add multiple test campers
        for (int i = 1; i <= 3; i++) {
            Map<String, String> camperData = new HashMap<>();
            camperData.put(RosterHeader.FIRST_NAME.standardName, "Test" + i);
            camperData.put(RosterHeader.LAST_NAME.standardName, "User" + i);
            camperData.put(RosterHeader.GRADE.standardName, String.valueOf(i + 5));
            camperData.put("Session", "Session " + (i % 2 + 1));

            Camper camper = new Camper("test-id-" + i, camperData);
            roster.addCamper(camper);
        }

        // Create a temporary file for export
        File outputFile = tempDir.resolve("roster-export-test.csv").toFile();

        // Configure export settings
        ExportSettings settings = new ExportSettings(outputFile);
        settings.setShowAllColumns(true)
                .setShowAllRows(true)
                .setUseEmptyPlaceholder(true);

        // Export the roster to CSV
        rosterService.exportRoster(roster, null, settings);

        // Verify the export file exists and has content
        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);

        // Verify the exported CSV content
        try (BufferedReader reader = new BufferedReader(new FileReader(outputFile))) {
            // Check header row
            String headerLine = reader.readLine();

            // Verify the file has headers (we don't check exact header names as they might be transformed)
            assertNotNull(headerLine, "Exported CSV should have a header row");
            assertFalse(headerLine.isEmpty(), "Exported CSV header row should not be empty");

            // Count data rows
            int rowCount = 0;
            while (reader.readLine() != null) {
                rowCount++;
            }

            // Verify all campers were exported
            assertEquals(3, rowCount, "Exported CSV should contain 3 data rows");
        }
    }

    @Test
    @DisplayName("Performs complete end-to-end workflow from import to export")
    void completeWorkflowTest() throws Exception {
        // Load test files
        TestPreset preset = TestPreset.MINI_NORMAL;
        File camperFile = preset.getCamperFile();
        File activityFile = preset.getActivityFile();

        // Step 1: Import rosters
        CamperRoster camperRoster = importService.importCamperRoster(camperFile);
        ActivityRoster activityRoster = importService.importActivityRoster(activityFile);

        // Verify imported data
        assertFalse(camperRoster.getCampers().isEmpty(), "Camper roster should contain campers");
        assertFalse(activityRoster.getCampers().isEmpty(), "Activity roster should contain activities");

        // Step 2: Create an enhanced roster from the imported data
        EnhancedRoster enhancedRoster = new EnhancedRoster();

        // Add headers from both rosters
        for (String header : camperRoster.getHeaderMap().keySet()) {
            enhancedRoster.addHeader(header);
        }

        for (String header : activityRoster.getHeaderMap().keySet()) {
            if (!enhancedRoster.hasHeader(header)) {
                enhancedRoster.addHeader(header);
            }
        }

        // Add campers from camper roster
        for (Camper camper : camperRoster.getCampers()) {
            enhancedRoster.addCamper(camper);
        }

        // Verify enhanced roster setup
        assertFalse(enhancedRoster.getCampers().isEmpty(), "Enhanced roster should contain campers");
        assertTrue(enhancedRoster.getAllHeaders().size() >=
                  (camperRoster.getAllHeaders().size() + activityRoster.getAllHeaders().size() -
                   camperRoster.getAllHeaders().stream().filter(h -> activityRoster.getAllHeaders().contains(h)).count()),
                  "Enhanced roster should contain headers from both source rosters");

        // Step 3: Apply filtering
        FilterManager filterManager = new FilterManager();
        filterManager.addFilter(new AssignmentFilter()); // Use a concrete filter implementation

        // Step 4: Export the processed roster
        File outputFile = tempDir.resolve("complete-workflow-test.csv").toFile();
        ExportSettings settings = new ExportSettings(outputFile);
        settings.setShowAllColumns(true).setShowAllRows(true);

        rosterService.exportRoster(enhancedRoster, filterManager, settings);

        // Verify export results
        assertTrue(outputFile.exists(), "Export file should exist");
        assertTrue(outputFile.length() > 0, "Export file should contain data");

        // Verify the exported file contains the expected number of rows
        long lineCount = Files.lines(outputFile.toPath()).count();
        assertEquals(enhancedRoster.getCampers().size() + 1, lineCount,
                    "Exported file should contain header row plus one row per camper");
    }

    @Test
    @DisplayName("Processes roster data with session information")
    void processRosterWithSessionTest() throws Exception {
        // Create a test roster with campers in different sessions
        EnhancedRoster roster = new EnhancedRoster();
        roster.addHeader(RosterHeader.FIRST_NAME.standardName);
        roster.addHeader(RosterHeader.LAST_NAME.standardName);
        roster.addHeader("Session"); // Using string directly since SESSION is not in RosterHeader enum

        // Add campers in different sessions
        String[] sessions = {"Session 1", "Session 2", "Session 1", "Session 3"};
        for (int i = 0; i < sessions.length; i++) {
            Map<String, String> data = new HashMap<>();
            data.put(RosterHeader.FIRST_NAME.standardName, "Camper" + i);
            data.put(RosterHeader.LAST_NAME.standardName, "Test");
            data.put("Session", sessions[i]);

            roster.addCamper(new Camper("id-" + i, data));
        }

        // Count campers in Session 1
        int session1Count = 0;
        for (Camper camper : roster.getCampers()) {
            if ("Session 1".equals(camper.getValue("Session"))) {
                session1Count++;
            }
        }

        // Verify session counts
        assertEquals(2, session1Count, "Two campers should be in Session 1");
        assertEquals(4, roster.getCampers().size(), "Total roster should have 4 campers");

        // Export all data
        File outputFile = tempDir.resolve("session-test.csv").toFile();
        ExportSettings settings = new ExportSettings(outputFile);
        settings.setShowAllColumns(true).setShowAllRows(true);

        rosterService.exportRoster(roster, null, settings);

        // Verify exported file contains all data
        try {
            long lineCount = Files.lines(outputFile.toPath()).count();
            assertEquals(5, lineCount, "Exported file should have header row plus four data rows");
        } catch (Exception e) {
            // Use the factory method instead of constructor
            throw RosterException.fileException("Failed to read export file",
                                              "Check file permissions and try again");
        }
    }
}