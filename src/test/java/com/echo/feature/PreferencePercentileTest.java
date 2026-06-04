package com.echo.feature;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.logging.WarningManager;

/**
 * Tests for the percentile score feature in PreferenceFeature.
 */
public class PreferencePercentileTest {

    private PreferenceFeature feature;
    private EnhancedRoster roster;
    private WarningManager warningManager;

    @BeforeEach
    public void setUp() {
        feature = new PreferenceFeature();
        roster = new EnhancedRoster();
        warningManager = new WarningManager();

        // Add required headers
        roster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.PREFERENCES.standardName);
        roster.addHeader(RosterHeader.ROUND_COUNT.standardName);
        roster.addHeader(RosterHeader.ROUND_1.standardName);
        roster.addHeader(RosterHeader.ROUND_2.standardName);
        roster.addHeader(RosterHeader.ROUND_3.standardName);
    }

    /**
     * Helper method to create a test camper with preferences and assignments.
     */
    private Camper createTestCamper(String firstName, String lastName, String preferences, String roundCount, String r1, String r2, String r3) {
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.camperRosterName, firstName);
        data.put(RosterHeader.LAST_NAME.camperRosterName, lastName);
        data.put(RosterHeader.PREFERENCES.standardName, preferences);
        data.put(RosterHeader.ROUND_COUNT.standardName, roundCount);
        data.put(RosterHeader.ROUND_1.standardName, r1);
        data.put(RosterHeader.ROUND_2.standardName, r2);
        data.put(RosterHeader.ROUND_3.standardName, r3);

        String id = firstName.toLowerCase() + "_" + lastName.toLowerCase() + "_";
        return new Camper(id, data);
    }

    @Test
    @DisplayName("Test percentile calculation with different scores")
    public void testPercentileCalculation() {
        // Create campers with different preference scores

        // Perfect score (100%)
        Camper camper1 = createTestCamper(
            "Perfect", "Score",
            "Archery, Sports, Fishing, Water Polo, Skiing",
            "3",
            "Archery", "Sports", "Fishing"
        );

        // Good score (78%)
        Camper camper2 = createTestCamper(
            "Good", "Score",
            "Archery, Sports, Fishing, Water Polo, Skiing",
            "3",
            "Archery", "Water Polo", "Skiing"
        );

        // Average score (56%)
        Camper camper3 = createTestCamper(
            "Average", "Score",
            "Archery, Sports, Fishing, Water Polo, Skiing",
            "3",
            "Water Polo", "Skiing", "Biking"
        );

        // Poor score (33%)
        Camper camper4 = createTestCamper(
            "Poor", "Score",
            "Archery, Sports, Fishing, Water Polo, Skiing",
            "3",
            "Biking", "Sailing", "Fishing"
        );

        // Add campers to roster
        roster.addCamper(camper1);
        roster.addCamper(camper2);
        roster.addCamper(camper3);
        roster.addCamper(camper4);

        // Apply the feature
        feature.applyFeature(roster, warningManager);

        // Verify percentile scores
        // camper1 should be 100th percentile (highest score)
        // camper2 should be 75th percentile (3/4 campers)
        // camper3 should be 50th percentile (2/4 campers)
        // camper4 should be 25th percentile (1/4 campers)

        assertEquals("100", camper1.getValue(RosterHeader.PREFERENCE_PERCENTILE.standardName));
        assertEquals("75", camper2.getValue(RosterHeader.PREFERENCE_PERCENTILE.standardName));
        assertEquals("50", camper3.getValue(RosterHeader.PREFERENCE_PERCENTILE.standardName));
        assertEquals("25", camper4.getValue(RosterHeader.PREFERENCE_PERCENTILE.standardName));
    }

    @Test
    @DisplayName("Test percentile calculation with tied scores")
    public void testPercentileCalculationWithTiedScores() {
        // Create campers with tied preference scores

        // Perfect score (100%)
        Camper camper1 = createTestCamper(
            "Perfect", "One",
            "Archery, Sports, Fishing, Water Polo, Skiing",
            "3",
            "Archery", "Sports", "Fishing"
        );

        // Also perfect score (100%)
        Camper camper2 = createTestCamper(
            "Perfect", "Two",
            "Archery, Sports, Fishing, Water Polo, Skiing",
            "3",
            "Archery", "Sports", "Fishing"
        );

        // Poor score (33%)
        Camper camper3 = createTestCamper(
            "Poor", "One",
            "Archery, Sports, Fishing, Water Polo, Skiing",
            "3",
            "Biking", "Sailing", "Fishing"
        );

        // Also poor score (33%)
        Camper camper4 = createTestCamper(
            "Poor", "Two",
            "Archery, Sports, Fishing, Water Polo, Skiing",
            "3",
            "Biking", "Sailing", "Fishing"
        );

        // Add campers to roster
        roster.addCamper(camper1);
        roster.addCamper(camper2);
        roster.addCamper(camper3);
        roster.addCamper(camper4);

        // Apply the feature
        feature.applyFeature(roster, warningManager);

        // Verify percentile scores
        // camper1 and camper2 should both be 100th percentile (4/4 campers)
        // camper3 and camper4 should both be 50th percentile (2/4 campers)

        assertEquals("100", camper1.getValue(RosterHeader.PREFERENCE_PERCENTILE.standardName));
        assertEquals("100", camper2.getValue(RosterHeader.PREFERENCE_PERCENTILE.standardName));
        assertEquals("50", camper3.getValue(RosterHeader.PREFERENCE_PERCENTILE.standardName));
        assertEquals("50", camper4.getValue(RosterHeader.PREFERENCE_PERCENTILE.standardName));
    }

    @Test
    @DisplayName("Test percentile calculation with missing preference data")
    public void testPercentileCalculationWithMissingData() {
        // Create campers with and without preference data

        // Perfect score (100%)
        Camper camper1 = createTestCamper(
            "Perfect", "Score",
            "Archery, Sports, Fishing, Water Polo, Skiing",
            "3",
            "Archery", "Sports", "Fishing"
        );

        // Missing preference data
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.camperRosterName, "Missing");
        data.put(RosterHeader.LAST_NAME.camperRosterName, "Data");
        data.put(RosterHeader.ROUND_COUNT.standardName, "3");
        data.put(RosterHeader.ROUND_1.standardName, "Archery");
        data.put(RosterHeader.ROUND_2.standardName, "Sports");
        data.put(RosterHeader.ROUND_3.standardName, "Fishing");
        // Deliberately not setting preferences
        Camper camper2 = new Camper("missing_data_", data);

        // Add campers to roster
        roster.addCamper(camper1);
        roster.addCamper(camper2);

        // Apply the feature
        feature.applyFeature(roster, warningManager);

        // Verify percentile scores
        // camper1 should be 100th percentile (highest score)
        // camper2 should be null (no preference data)

        assertEquals("100", camper1.getValue(RosterHeader.PREFERENCE_PERCENTILE.standardName));
        assertNull(camper2.getValue(RosterHeader.PREFERENCE_PERCENTILE.standardName));
    }
}
