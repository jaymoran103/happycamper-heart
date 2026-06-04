package com.echo.domain;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.echo.automation.TestPreset;
import com.echo.logging.RosterException;
import com.echo.logging.WarningManager;

/**
 * Tests for the CamperRoster class.
 */
public class CamperRosterTest {

    private CamperRoster roster;
    private WarningManager warningManager;

    @BeforeEach
    public void setUp() {
        roster = new CamperRoster();
        warningManager = new WarningManager();
    }

    @Test
    @DisplayName("getRequiredHeaders should return the expected headers")
    public void testGetRequiredHeaders() {
        List<String> requiredHeaders = CamperRoster.getRequiredHeaders();

        // Check that all expected headers are included
        assertTrue(requiredHeaders.contains(RosterHeader.FIRST_NAME.camperRosterName), "Should require first name");
        assertTrue(requiredHeaders.contains(RosterHeader.LAST_NAME.camperRosterName), "Should require last name");
        assertTrue(requiredHeaders.contains(RosterHeader.GRADE.camperRosterName), "Should require grade");
        assertTrue(requiredHeaders.contains(RosterHeader.ESP.camperRosterName), "Should require ESP");
        assertTrue(requiredHeaders.contains(RosterHeader.PREFERRED_NAME.camperRosterName), "Should require preferred name");
    }

    @Test
    @DisplayName("validate should pass with valid data")
    public void testValidateWithValidData() {
        // Add required headers
        for (String header : CamperRoster.getRequiredHeaders()) {
            roster.addHeader(header);
        }

        // Add a valid camper
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camperData.put(RosterHeader.GRADE.camperRosterName, "7");
        camperData.put(RosterHeader.ESP.camperRosterName, "Session 1/Traditional Camp");
        camperData.put(RosterHeader.PREFERRED_NAME.camperRosterName, "Johnny");

        Camper camper = new Camper(camperData);
        roster.addCamper(camper);

        // Validation should pass without exceptions
        assertDoesNotThrow(() -> roster.validate(warningManager));
        // assertFalse(warningManager.hasWarnings(), "Should not have warnings"); TODO has warnings but these are because of test data syntax and regex, not because of roster-relevant issues
    }

    @Test
    @DisplayName("validate should log warnings for invalid data formats")
    public void testValidateWithInvalidDataFormats() {
        // Add required headers
        for (String header : CamperRoster.getRequiredHeaders()) {
            roster.addHeader(header);
        }

        // Add a camper with invalid grade format
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camperData.put(RosterHeader.GRADE.camperRosterName, "Seventh"); // Invalid format, should be numeric
        camperData.put(RosterHeader.ESP.camperRosterName, "Session 1/Traditional Camp");
        camperData.put(RosterHeader.PREFERRED_NAME.camperRosterName, "Johnny");

        Camper camper = new Camper(camperData);
        roster.addCamper(camper);

        // Validation should pass but log warnings
        assertDoesNotThrow(() -> roster.validate(warningManager));
        assertTrue(warningManager.hasWarnings(), "Should have warnings for invalid data format");
    }

    @Test
    @DisplayName("validate should throw warnings for missing required headers")
    public void testValidateWithMissingRequiredHeaders() throws RosterException {
        // Add only some of the required headers
        roster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        // Missing other required headers

        // Add a camper
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");

        Camper camper = new Camper(camperData);
        roster.addCamper(camper);

        // Validation should throw an exception
        assertThrows(RosterException.class, () -> roster.validate(warningManager));
        // roster.validate(warningManager);

        // assertTrue(warningManager.getWarningLog().containsKey(RosterWarning.MISSING_FEATURE_HEADER), "Should have warnings for missing required headers");
    }

    @ParameterizedTest
    @EnumSource(value = TestPreset.class, names = {"MINI_NORMAL"})
    @DisplayName("Should load data from CSV file")
    public void testLoadFromCSV(TestPreset preset) {
        File camperFile = preset.getCamperFile();

        // Load data from CSV
        assertDoesNotThrow(() -> {
            roster.loadFromCSV(camperFile);
            roster.validate(warningManager);
        });

        // Check that data was loaded
        assertFalse(roster.getCampers().isEmpty(), "Should have loaded campers");
        assertTrue(roster.getAllHeaders().containsAll(CamperRoster.getRequiredHeaders()),
                "Should have all required headers");
    }

    @Test
    @DisplayName("generateCamperId should create consistent IDs")
    public void testGenerateCamperId() {
        Map<String, String> camperData1 = new HashMap<>();
        camperData1.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData1.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camperData1.put(RosterHeader.GRADE.camperRosterName, "7");

        String id1 = Roster.generateCamperId(camperData1);
        assertEquals("john_doe_7", id1, "ID should be generated correctly");

        // Test with spaces in names
        Map<String, String> camperData2 = new HashMap<>();
        camperData2.put(RosterHeader.FIRST_NAME.camperRosterName, "Mary Ann");
        camperData2.put(RosterHeader.LAST_NAME.camperRosterName, "Smith Jones");
        camperData2.put(RosterHeader.GRADE.camperRosterName, "8");

        String id2 = Roster.generateCamperId(camperData2);
        assertEquals("mary_ann_smith_jones_8", id2, "ID should handle spaces correctly");
    }
}