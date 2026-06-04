package com.echo.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.ActivityRoster;
import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.logging.RosterWarning;
import com.echo.logging.RosterWarning.WarningType;
import com.echo.logging.WarningManager;

/**
 * Comprehensive tests for the ActivityFeature class.
 * Tests all aspects of the feature including:
 * - Feature metadata
 * - Activity assignment processing
 * - Round count calculation
 * - Orphaned activity handling
 * - Duplicate activity detection
 */
public class ActivityFeatureTest {

    private ActivityFeature feature;
    private EnhancedRoster enhancedRoster;
    private ActivityRoster activityRoster;
    private WarningManager warningManager;

    @BeforeEach
    public void setUp() {
        feature = new ActivityFeature();
        enhancedRoster = new EnhancedRoster();
        activityRoster = new ActivityRoster();
        warningManager = new WarningManager();

        // Set up the enhanced roster with required headers and campers
        enhancedRoster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        enhancedRoster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        enhancedRoster.addHeader(RosterHeader.GRADE.camperRosterName);

        // Add campers to the enhanced roster
        Map<String, String> camper1Data = new HashMap<>();
        camper1Data.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camper1Data.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camper1Data.put(RosterHeader.GRADE.camperRosterName, "5th");
        Camper camper1 = new Camper(camper1Data);
        enhancedRoster.addCamper(camper1);

        Map<String, String> camper2Data = new HashMap<>();
        camper2Data.put(RosterHeader.FIRST_NAME.camperRosterName, "Jane");
        camper2Data.put(RosterHeader.LAST_NAME.camperRosterName, "Smith");
        camper2Data.put(RosterHeader.GRADE.camperRosterName, "6th");
        Camper camper2 = new Camper(camper2Data);
        enhancedRoster.addCamper(camper2);

        // Set up the activity roster with required headers
        activityRoster.addHeader(RosterHeader.FIRST_NAME.activityRosterName);
        activityRoster.addHeader(RosterHeader.LAST_NAME.activityRosterName);
        activityRoster.addHeader(RosterHeader.GRADE.activityRosterName);
        activityRoster.addHeader(RosterHeader.ACTIVITY.activityRosterName);
        activityRoster.addHeader(RosterHeader.ROUND.activityRosterName);
        activityRoster.addHeader(RosterHeader.CABIN.activityRosterName);
        activityRoster.addHeader(RosterHeader.PREFERRED_NAME.activityRosterName);

        // Add activities to the activity roster
        // John Doe - Round 1: Archery
        Map<String, String> activity1Data = new HashMap<>();
        activity1Data.put(RosterHeader.FIRST_NAME.activityRosterName, "John");
        activity1Data.put(RosterHeader.LAST_NAME.activityRosterName, "Doe");
        activity1Data.put(RosterHeader.GRADE.activityRosterName, "5th");
        activity1Data.put(RosterHeader.ACTIVITY.activityRosterName, "Archery");
        activity1Data.put(RosterHeader.ROUND.activityRosterName, "1");
        activity1Data.put(RosterHeader.CABIN.activityRosterName, "Cabin A");
        activity1Data.put(RosterHeader.PREFERRED_NAME.activityRosterName, "Johnny");
        Camper activity1 = new Camper(activity1Data);
        activityRoster.addCamper(activity1);

        // John Doe - Round 2: Swimming
        Map<String, String> activity2Data = new HashMap<>();
        activity2Data.put(RosterHeader.FIRST_NAME.activityRosterName, "John");
        activity2Data.put(RosterHeader.LAST_NAME.activityRosterName, "Doe");
        activity2Data.put(RosterHeader.GRADE.activityRosterName, "5th");
        activity2Data.put(RosterHeader.ACTIVITY.activityRosterName, "Swimming");
        activity2Data.put(RosterHeader.ROUND.activityRosterName, "2");
        activity2Data.put(RosterHeader.CABIN.activityRosterName, "Cabin A");
        activity2Data.put(RosterHeader.PREFERRED_NAME.activityRosterName, "Johnny");
        Camper activity2 = new Camper(activity2Data);
        activityRoster.addCamper(activity2);

        // Jane Smith - Round 1: Hiking
        Map<String, String> activity3Data = new HashMap<>();
        activity3Data.put(RosterHeader.FIRST_NAME.activityRosterName, "Jane");
        activity3Data.put(RosterHeader.LAST_NAME.activityRosterName, "Smith");
        activity3Data.put(RosterHeader.GRADE.activityRosterName, "6th");
        activity3Data.put(RosterHeader.ACTIVITY.activityRosterName, "Hiking");
        activity3Data.put(RosterHeader.ROUND.activityRosterName, "1");
        activity3Data.put(RosterHeader.CABIN.activityRosterName, "Cabin B");
        activity3Data.put(RosterHeader.PREFERRED_NAME.activityRosterName, "Jane");
        Camper activity3 = new Camper(activity3Data);
        activityRoster.addCamper(activity3);

        // Jane Smith - Round 3: Crafts
        Map<String, String> activity4Data = new HashMap<>();
        activity4Data.put(RosterHeader.FIRST_NAME.activityRosterName, "Jane");
        activity4Data.put(RosterHeader.LAST_NAME.activityRosterName, "Smith");
        activity4Data.put(RosterHeader.GRADE.activityRosterName, "6th");
        activity4Data.put(RosterHeader.ACTIVITY.activityRosterName, "Crafts");
        activity4Data.put(RosterHeader.ROUND.activityRosterName, "3");
        activity4Data.put(RosterHeader.CABIN.activityRosterName, "Cabin B");
        activity4Data.put(RosterHeader.PREFERRED_NAME.activityRosterName, "Jane");
        Camper activity4 = new Camper(activity4Data);
        activityRoster.addCamper(activity4);
    }

    @Test
    @DisplayName("Test feature metadata")
    public void testFeatureMetadata() {
        assertEquals("activity", feature.getFeatureId());
        assertEquals("Activity Assignments", feature.getFeatureName());

        // Check required headers - ActivityFeature doesn't require any headers in the enhanced roster
        List<String> requiredHeaders = feature.getRequiredHeaders();
        assertTrue(requiredHeaders.isEmpty());

        // Check added headers
        List<String> addedHeaders = feature.getAddedHeaders();
        assertTrue(addedHeaders.contains(RosterHeader.ROUND_1.standardName));
        assertTrue(addedHeaders.contains(RosterHeader.ROUND_2.standardName));
        assertTrue(addedHeaders.contains(RosterHeader.ROUND_3.standardName));
        assertTrue(addedHeaders.contains(RosterHeader.ROUND_COUNT.standardName));

        // Check required formats
        Map<String, String> requiredFormats = feature.getRequiredFormats();
        assertNotNull(requiredFormats.get("Period"));
        assertNotNull(requiredFormats.get("Activity"));
    }

    @Test
    @DisplayName("Test standard applyFeature throws exception")
    public void testStandardApplyFeatureThrowsException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            feature.applyFeature(enhancedRoster, warningManager);
        });
    }

    @Test
    @DisplayName("Test preValidate and postValidate")
    public void testValidationMethods() {
        assertTrue(feature.preValidate(enhancedRoster, warningManager));
        assertTrue(feature.postValidate(enhancedRoster, warningManager));
    }

    @Test
    @DisplayName("Test applyFeature with activity roster")
    public void testApplyFeatureWithActivityRoster() {
        // Apply the feature
        feature.applyFeature(enhancedRoster, activityRoster, warningManager);

        // Verify the feature is enabled
        assertTrue(enhancedRoster.hasFeature("activity"));

        // Verify headers were added
        assertTrue(enhancedRoster.hasHeader(RosterHeader.ROUND_1.standardName));
        assertTrue(enhancedRoster.hasHeader(RosterHeader.ROUND_2.standardName));
        assertTrue(enhancedRoster.hasHeader(RosterHeader.ROUND_3.standardName));
        assertTrue(enhancedRoster.hasHeader(RosterHeader.ROUND_COUNT.standardName));

        // Verify activities were assigned to campers
        String johnId = "john_doe_5th";
        String janeId = "jane_smith_6th";

        assertEquals("Archery", enhancedRoster.getValue(johnId, RosterHeader.ROUND_1.standardName));
        assertEquals("Swimming", enhancedRoster.getValue(johnId, RosterHeader.ROUND_2.standardName));
        assertEquals(null, enhancedRoster.getValue(johnId, RosterHeader.ROUND_3.standardName));
        assertEquals("2", enhancedRoster.getValue(johnId, RosterHeader.ROUND_COUNT.standardName));

        assertEquals("Hiking", enhancedRoster.getValue(janeId, RosterHeader.ROUND_1.standardName));
        assertEquals(null, enhancedRoster.getValue(janeId, RosterHeader.ROUND_2.standardName));
        assertEquals("Crafts", enhancedRoster.getValue(janeId, RosterHeader.ROUND_3.standardName));
        assertEquals("2", enhancedRoster.getValue(janeId, RosterHeader.ROUND_COUNT.standardName));
    }

    @Test
    @DisplayName("Test getActivityForCamper static method")
    public void testGetActivityForCamper() {
        // Apply the feature first
        feature.applyFeature(enhancedRoster, activityRoster, warningManager);

        // Test with camper ID
        String johnId = "john_doe_5th";
        assertEquals("Archery", ActivityFeature.getActivityForCamper(enhancedRoster, johnId, 1));
        assertEquals("Swimming", ActivityFeature.getActivityForCamper(enhancedRoster, johnId, 2));
        assertEquals(null, ActivityFeature.getActivityForCamper(enhancedRoster, johnId, 3));

        // Test with camper object
        Camper john = enhancedRoster.getCamperById(johnId);
        assertEquals("Archery", ActivityFeature.getActivityForCamper(john, 1));
        assertEquals("Swimming", ActivityFeature.getActivityForCamper(john, 2));
        assertEquals(null, ActivityFeature.getActivityForCamper(john, 3));

        // Test invalid period
        assertThrows(IllegalArgumentException.class, () -> {
            ActivityFeature.getActivityForCamper(enhancedRoster, johnId, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ActivityFeature.getActivityForCamper(enhancedRoster, johnId, 4);
        });
    }

    @Test
    @DisplayName("Test getAssignmentCount static method")
    public void testGetAssignmentCount() {
        // Apply the feature first
        feature.applyFeature(enhancedRoster, activityRoster, warningManager);

        String johnId = "john_doe_5th";
        String janeId = "jane_smith_6th";

        assertEquals(2, ActivityFeature.getAssignmentCount(enhancedRoster, johnId));
        assertEquals(2, ActivityFeature.getAssignmentCount(enhancedRoster, janeId));

        // Test with feature not enabled
        EnhancedRoster newRoster = new EnhancedRoster();
        assertThrows(UnsupportedOperationException.class, () -> {
            ActivityFeature.getAssignmentCount(newRoster, johnId);
        });
    }

    @Test
    @DisplayName("Test handling of duplicate activities in the same round")
    public void testDuplicateActivities() {
        // Add a duplicate activity for John in round 1
        Map<String, String> duplicateActivity = new HashMap<>();
        duplicateActivity.put(RosterHeader.FIRST_NAME.activityRosterName, "John");
        duplicateActivity.put(RosterHeader.LAST_NAME.activityRosterName, "Doe");
        duplicateActivity.put(RosterHeader.GRADE.activityRosterName, "5th");
        duplicateActivity.put(RosterHeader.ACTIVITY.activityRosterName, "Fishing");
        duplicateActivity.put(RosterHeader.ROUND.activityRosterName, "1");
        duplicateActivity.put(RosterHeader.CABIN.activityRosterName, "Cabin A");
        duplicateActivity.put(RosterHeader.PREFERRED_NAME.activityRosterName, "Johnny");
        activityRoster.addCamper(new Camper(duplicateActivity));

        // Apply the feature
        feature.applyFeature(enhancedRoster, activityRoster, warningManager);

        // Verify that warnings were generated
        assertFalse(warningManager.getWarningLog().isEmpty(), "Should have warnings");

        // Check for duplicate activity warning
        boolean foundDuplicateWarning = false;
        for (Map.Entry<WarningType, ArrayList<RosterWarning>> entry : warningManager.getWarningLog().entrySet()) {
            if (entry.getKey() == WarningType.DUPLICATE_ACTIVITY) {
                for (RosterWarning warning : entry.getValue()) {
                    String[] data = warning.getDisplayData();
                    if (data.length > 0 && data[0].contains("John") && data[0].contains("Doe")) {
                        foundDuplicateWarning = true;
                        break;
                    }
                }
            }
        }

        assertTrue(foundDuplicateWarning, "Should have warning for duplicate activity");

        // Verify that one of the activities was assigned (the last one processed)
        String johnId = "john_doe_5th";
        String activity = enhancedRoster.getValue(johnId, RosterHeader.ROUND_1.standardName);
        assertNotNull(activity);
        assertTrue(activity.equals("Archery") || activity.equals("Fishing"),
                   "Should have assigned one of the duplicate activities");
    }

    @Test
    @DisplayName("Test handling of orphaned activities")
    public void testOrphanedActivities() {
        // Add an activity for a camper not in the enhanced roster
        Map<String, String> orphanedActivity = new HashMap<>();
        orphanedActivity.put(RosterHeader.FIRST_NAME.activityRosterName, "Bob");
        orphanedActivity.put(RosterHeader.LAST_NAME.activityRosterName, "Johnson");
        orphanedActivity.put(RosterHeader.GRADE.activityRosterName, "4th");
        orphanedActivity.put(RosterHeader.ACTIVITY.activityRosterName, "Canoeing");
        orphanedActivity.put(RosterHeader.ROUND.activityRosterName, "2");
        orphanedActivity.put(RosterHeader.CABIN.activityRosterName, "Cabin C");
        activityRoster.addCamper(new Camper(orphanedActivity));

        // Apply the feature
        feature.applyFeature(enhancedRoster, activityRoster, warningManager);

        // Verify that a warning was generated for the orphaned activity
        boolean foundOrphanWarning = false;
        for (Map.Entry<WarningType, ArrayList<RosterWarning>> entry : warningManager.getWarningLog().entrySet()) {
            if (entry.getKey() == WarningType.UNMATCHED_ACTIVITY_ADDED) {
                for (RosterWarning warning : entry.getValue()) {
                    String[] data = warning.getDisplayData();
                    if (data.length > 0 && data[0].contains("Bob") && data[0].contains("Johnson")) {
                        foundOrphanWarning = true;
                        break;
                    }
                }
            }
        }

        assertTrue(foundOrphanWarning, "Should have warning for orphaned activity");
    }

    @Test
    @DisplayName("Test handling of invalid round numbers")
    public void testInvalidRoundNumbers() {
        // Add an activity with an invalid round number
        Map<String, String> invalidRoundActivity = new HashMap<>();
        invalidRoundActivity.put(RosterHeader.FIRST_NAME.activityRosterName, "John");
        invalidRoundActivity.put(RosterHeader.LAST_NAME.activityRosterName, "Doe");
        invalidRoundActivity.put(RosterHeader.GRADE.activityRosterName, "5th");
        invalidRoundActivity.put(RosterHeader.ACTIVITY.activityRosterName, "Canoeing");
        invalidRoundActivity.put(RosterHeader.ROUND.activityRosterName, "invalid");
        invalidRoundActivity.put(RosterHeader.CABIN.activityRosterName, "Cabin A");
        activityRoster.addCamper(new Camper(invalidRoundActivity));

        // Apply the feature
        feature.applyFeature(enhancedRoster, activityRoster, warningManager);

        // Verify that a warning was generated for the invalid round number
        boolean foundInvalidRoundWarning = false;
        for (Map.Entry<WarningType, ArrayList<RosterWarning>> entry : warningManager.getWarningLog().entrySet()) {
            if (entry.getKey() == WarningType.BAD_DATA_FORMAT) {
                for (RosterWarning warning : entry.getValue()) {
                    String[] data = warning.getDisplayData();
                    if (data.length > 1 &&
                        data[0].contains("John") &&
                        data[0].contains("Doe") &&
                        data[1].equals(RosterHeader.ROUND.activityRosterName)) {
                        foundInvalidRoundWarning = true;
                        break;
                    }
                }
            }
        }

        assertTrue(foundInvalidRoundWarning, "Should have warning for invalid round number");
    }
}
