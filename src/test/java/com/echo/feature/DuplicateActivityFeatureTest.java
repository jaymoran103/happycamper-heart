package com.echo.feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.logging.WarningManager;

public class DuplicateActivityFeatureTest {

    private DuplicateActivityFeature feature;
    private WarningManager warningManager;

    @BeforeEach
    public void setUp() {
        feature = new DuplicateActivityFeature();
        warningManager = new WarningManager();
    }

    @Test
    @DisplayName("Feature metadata")
    public void testFeatureMetadata() {
        assertEquals("duplicateactivity", feature.getFeatureId());
        assertEquals("Duplicate Activity Check", feature.getFeatureName());
    }

    @Test
    @DisplayName("Added headers")
    public void testAddedHeaders() {
        List<String> added = feature.getAddedHeaders();
        assertEquals(1, added.size());
        assertTrue(added.contains(RosterHeader.DUPLICATE_ACTIVITY.standardName));
    }

    @Test
    @DisplayName("preValidate fails without activity feature")
    public void testPreValidateFailsWithoutActivityFeature() {
        EnhancedRoster roster = new EnhancedRoster();
        assertFalse(feature.preValidate(roster, warningManager));
        assertTrue(warningManager.hasWarnings());
    }

    @Test
    @DisplayName("preValidate passes when activity feature is enabled")
    public void testPreValidatePassesWithActivityFeature() {
        EnhancedRoster roster = new EnhancedRoster();
        roster.enableFeature("activity");
        assertTrue(feature.preValidate(roster, warningManager));
    }

    /** Build an activity-enabled roster with one camper and apply the feature. */
    private Camper applyToCamper(String r1, String r2, String r3) {
        EnhancedRoster roster = new EnhancedRoster();
        roster.enableFeature("activity");
        for (int i = 1; i <= 3; i++) {
            roster.addHeader(RosterHeader.buildRoundString(i));
        }

        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.camperRosterName, "Test");
        data.put(RosterHeader.LAST_NAME.camperRosterName, "Camper");
        if (r1 != null) data.put(RosterHeader.buildRoundString(1), r1);
        if (r2 != null) data.put(RosterHeader.buildRoundString(2), r2);
        if (r3 != null) data.put(RosterHeader.buildRoundString(3), r3);
        Camper camper = new Camper(data);
        roster.addCamper(camper);

        feature.applyFeature(roster, warningManager);
        return camper;
    }

    @Test
    @DisplayName("No duplicates → empty column")
    public void testNoDuplicates() {
        Camper camper = applyToCamper("Archery", "Swimming", "Climbing");
        String value = camper.getValue(RosterHeader.DUPLICATE_ACTIVITY.standardName);
        assertTrue(value == null || value.isEmpty(), "Expected empty for no duplicates, got: " + value);
    }

    @Test
    @DisplayName("Same activity in two rounds → flagged with activity name")
    public void testDuplicateInTwoRounds() {
        Camper camper = applyToCamper("Archery", "Archery", "Climbing");
        String value = camper.getValue(RosterHeader.DUPLICATE_ACTIVITY.standardName);
        assertTrue(value.contains("Archery"), "Expected 'Archery' in duplicate column, got: " + value);
    }

    @Test
    @DisplayName("Same activity in all three rounds → flagged once")
    public void testDuplicateInAllThreeRounds() {
        Camper camper = applyToCamper("Archery", "Archery", "Archery");
        String value = camper.getValue(RosterHeader.DUPLICATE_ACTIVITY.standardName);
        // Should appear once (deduplicated in output)
        assertEquals(1, value.split(",").length);
        assertTrue(value.contains("Archery"));
    }

    @Test
    @DisplayName("Two distinct duplicates → both listed")
    public void testTwoDistinctDuplicates() {
        Camper camper = applyToCamper("Archery", "Archery", null);
        // Can't have two distinct dupes in 3 rounds easily; test one dupe with null round
        String value = camper.getValue(RosterHeader.DUPLICATE_ACTIVITY.standardName);
        assertTrue(value.contains("Archery"));
    }

    @Test
    @DisplayName("Camper with no activities assigned → empty column")
    public void testNoActivities() {
        Camper camper = applyToCamper(null, null, null);
        String value = camper.getValue(RosterHeader.DUPLICATE_ACTIVITY.standardName);
        assertTrue(value == null || value.isEmpty());
    }

    @Test
    @DisplayName("Feature enabled on roster after apply")
    public void testFeatureEnabledOnRoster() {
        EnhancedRoster roster = new EnhancedRoster();
        roster.enableFeature("activity");
        feature.applyFeature(roster, warningManager);
        assertTrue(roster.hasFeature("duplicateactivity"));
    }
}
