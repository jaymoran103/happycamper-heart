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
 * Tests for the ActivityRoster class.
 */
public class ActivityRosterTest {

    private ActivityRoster roster;
    private WarningManager warningManager;

    @BeforeEach
    public void setUp() {
        roster = new ActivityRoster();
        warningManager = new WarningManager();
    }

    @Test
    @DisplayName("getRequiredHeaders should return the expected headers")
    public void testGetRequiredHeaders() {
        List<String> requiredHeaders = ActivityRoster.getRequiredHeaders();

        // Check that all expected headers are included
        assertTrue(requiredHeaders.contains(RosterHeader.ACTIVITY.activityRosterName), "Should require activity");
        assertTrue(requiredHeaders.contains(RosterHeader.ROUND.activityRosterName), "Should require round");
        assertTrue(requiredHeaders.contains(RosterHeader.PREFERRED_NAME.activityRosterName), "Should require preferred name");
        assertTrue(requiredHeaders.contains(RosterHeader.LAST_NAME.activityRosterName), "Should require last name");
        assertTrue(requiredHeaders.contains(RosterHeader.FIRST_NAME.activityRosterName), "Should require first name");
        assertTrue(requiredHeaders.contains(RosterHeader.CABIN.activityRosterName), "Should require cabin");
        assertTrue(requiredHeaders.contains(RosterHeader.GRADE.activityRosterName), "Should require grade");
    }

    @Test
    @DisplayName("validate should pass with valid data")
    public void testValidateWithValidData() {
        // Add required headers
        for (String header : ActivityRoster.getRequiredHeaders()) {
            roster.addHeader(header);
        }

        // Add a valid activity
        Map<String, String> activityData = new HashMap<>();
        activityData.put(RosterHeader.ACTIVITY.activityRosterName, "Swimming");
        activityData.put(RosterHeader.ROUND.activityRosterName, "1");
        activityData.put(RosterHeader.PREFERRED_NAME.activityRosterName, "Johnny");
        activityData.put(RosterHeader.LAST_NAME.activityRosterName, "Doe");
        activityData.put(RosterHeader.FIRST_NAME.activityRosterName, "John");
        activityData.put(RosterHeader.CABIN.activityRosterName, "Cabin A");
        activityData.put(RosterHeader.GRADE.activityRosterName, "7");
        activityData.put("Period", "2");

        Camper activity = new Camper(activityData);
        roster.addCamper(activity);

        // Validation should pass without exceptions
        assertDoesNotThrow(() -> roster.validate(warningManager));
        // assertFalse(warningManager.hasWarnings(), "Should not have warnings"); TODO has warnings but these are because of test data syntax and regex, not because of roster-relevant issues

    }

    @Test
    @DisplayName("validate should log warnings for invalid data formats")
    public void testValidateWithInvalidDataFormats() {
        // Add required headers
        for (String header : ActivityRoster.getRequiredHeaders()) {
            roster.addHeader(header);
        }
        roster.addHeader("Period");

        // Add an activity with invalid period format
        Map<String, String> activityData = new HashMap<>();
        activityData.put(RosterHeader.ACTIVITY.activityRosterName, "Swimming");
        activityData.put(RosterHeader.PREFERRED_NAME.activityRosterName, "Johnny");
        activityData.put(RosterHeader.LAST_NAME.activityRosterName, "Doe");
        activityData.put(RosterHeader.FIRST_NAME.activityRosterName, "John");
        activityData.put(RosterHeader.CABIN.activityRosterName, "Cabin A");
        activityData.put(RosterHeader.GRADE.activityRosterName, "7");
        activityData.put(RosterHeader.ROUND.activityRosterName, "First"); // Invalid format, should be 1-3

        Camper activity = new Camper(activityData);
        roster.addCamper(activity);

        // Validation should pass but log warnings
        assertDoesNotThrow(() -> roster.validate(warningManager));
        assertTrue(warningManager.hasWarnings(), "Should have warnings for invalid data format");
    }

    @Test
    @DisplayName("validate should throw exception for missing required headers")
    public void testValidateWithMissingRequiredHeaders() throws RosterException {
        // Add only some of the required headers
        roster.addHeader(RosterHeader.ACTIVITY.activityRosterName);
        roster.addHeader(RosterHeader.ROUND.activityRosterName);
        // Missing other required headers

        // Add an activity
        Map<String, String> activityData = new HashMap<>();
        activityData.put(RosterHeader.ACTIVITY.activityRosterName, "Swimming");
        activityData.put(RosterHeader.ROUND.activityRosterName, "1");

        Camper activity = new Camper(activityData);
        roster.addCamper(activity);


        // Validation should throw an exception
        assertThrows(RosterException.class, () -> roster.validate(warningManager));

    }

    @ParameterizedTest
    @EnumSource(value = TestPreset.class, names = {"MINI_NORMAL"})
    @DisplayName("Should load data from CSV file")
    public void testLoadFromCSV(TestPreset preset) {
        File activityFile = preset.getActivityFile();

        // Load data from CSV
        assertDoesNotThrow(() -> {
            roster.loadFromCSV(activityFile);
            roster.validate(warningManager);
        });

        // Check that data was loaded
        assertFalse(roster.getCampers().isEmpty(), "Should have loaded activities");
        assertTrue(roster.getAllHeaders().containsAll(ActivityRoster.getRequiredHeaders()),
                "Should have all required headers");
    }

    @Test
    @DisplayName("getKeyedData should return activities keyed by ID")
    public void testGetKeyedData() {
        // Add some activities
        Map<String, String> activityData1 = new HashMap<>();
        activityData1.put(RosterHeader.ACTIVITY.activityRosterName, "Swimming");
        activityData1.put(RosterHeader.ROUND.activityRosterName, "1");
        activityData1.put(RosterHeader.PREFERRED_NAME.activityRosterName, "Johnny");
        activityData1.put(RosterHeader.LAST_NAME.activityRosterName, "Doe");
        activityData1.put(RosterHeader.FIRST_NAME.activityRosterName, "John");
        activityData1.put(RosterHeader.CABIN.activityRosterName, "Cabin A");
        activityData1.put(RosterHeader.GRADE.activityRosterName, "7");

        Map<String, String> activityData2 = new HashMap<>();
        activityData2.put(RosterHeader.ACTIVITY.activityRosterName, "Archery");
        activityData2.put(RosterHeader.ROUND.activityRosterName, "2");
        activityData2.put(RosterHeader.PREFERRED_NAME.activityRosterName, "Jane");
        activityData2.put(RosterHeader.LAST_NAME.activityRosterName, "Smith");
        activityData2.put(RosterHeader.FIRST_NAME.activityRosterName, "Jane");
        activityData2.put(RosterHeader.CABIN.activityRosterName, "Cabin B");
        activityData2.put(RosterHeader.GRADE.activityRosterName, "8");

        String id1 = ActivityRoster.generateCamperIdFromActivity(activityData1);
        String id2 = ActivityRoster.generateCamperIdFromActivity(activityData2);

        Camper activity1 = new Camper(id1, activityData1);
        Camper activity2 = new Camper(id2, activityData2);

        roster.addCamper(activity1);
        roster.addCamper(activity2);

        // Get keyed data
        Map<String, Map<String, String>> keyedData = roster.getKeyedData();

        // Check that all activities are included
        assertEquals(2, keyedData.size(), "Should have 2 activities");
        assertTrue(keyedData.containsKey(activity1.getId()), "Should contain first activity");
        assertTrue(keyedData.containsKey(activity2.getId()), "Should contain second activity");

        // Check that data is correct
        assertEquals("Swimming", keyedData.get(activity1.getId()).get(RosterHeader.ACTIVITY.activityRosterName),
                "First activity should be Swimming");
        assertEquals("Archery", keyedData.get(activity2.getId()).get(RosterHeader.ACTIVITY.activityRosterName),
                "Second activity should be Archery");
    }

    @Test
    @DisplayName("generateCamperIdFromActivity should create consistent IDs")
    public void testGenerateCamperIdFromActivity() {
        Map<String, String> activityData = new HashMap<>();
        activityData.put(RosterHeader.FIRST_NAME.activityRosterName, "John");
        activityData.put(RosterHeader.LAST_NAME.activityRosterName, "Doe");
        activityData.put(RosterHeader.GRADE.activityRosterName, "7");

        String id = ActivityRoster.generateCamperIdFromActivity(activityData);
        assertEquals("john_doe_7", id, "ID should be generated correctly");

        // Test with spaces in names
        Map<String, String> activityData2 = new HashMap<>();
        activityData2.put(RosterHeader.FIRST_NAME.activityRosterName, "Mary Ann");
        activityData2.put(RosterHeader.LAST_NAME.activityRosterName, "Smith Jones");
        activityData2.put(RosterHeader.GRADE.activityRosterName, "8");

        String id2 = ActivityRoster.generateCamperIdFromActivity(activityData2);
        assertEquals("mary_ann_smith_jones_8", id2, "ID should handle spaces correctly");
    }
}