package com.echo.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.logging.RosterWarning;
import com.echo.logging.RosterWarning.WarningType;
import com.echo.logging.WarningManager;

/**
 * Comprehensive tests for the PreferenceFeature class.
 * Tests all aspects of the feature including:
 * - Preference parsing
 * - Score calculation
 * - Unrequested activity detection
 * - Percentile calculation
 * - Edge cases and error handling
 */
public class PreferenceFeatureTest {
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

        // Enable activity feature (required for preference feature)
        roster.enableFeature("activity");
    }

    @Test
    @DisplayName("Test feature metadata")
    public void testFeatureMetadata() {
        assertEquals("preference", feature.getFeatureId());
        assertEquals("Preference Evaluation", feature.getFeatureName());

        // Check required headers
        List<String> requiredHeaders = feature.getRequiredHeaders();
        assertTrue(requiredHeaders.contains(RosterHeader.PREFERENCES.standardName));

        // Check added headers
        List<String> addedHeaders = feature.getAddedHeaders();
        assertTrue(addedHeaders.contains(RosterHeader.PREFERENCE_SCORE.standardName));
        assertTrue(addedHeaders.contains(RosterHeader.PREFERENCE_PERCENTILE.standardName));
        assertTrue(addedHeaders.contains(RosterHeader.UNREQUESTED_ACTIVITIES.standardName));
        assertTrue(addedHeaders.contains(RosterHeader.SCORE_BY_ROUND.standardName));
    }

    @Test
    @DisplayName("Test preValidate method")
    public void testPreValidate() {
        // Should return true when all required headers are present
        assertTrue(feature.preValidate(roster, warningManager));

        // Create a roster without the preferences header
        EnhancedRoster incompleteRoster = new EnhancedRoster();
        incompleteRoster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        incompleteRoster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        incompleteRoster.enableFeature("activity");

        // Should return false when required headers are missing
        assertFalse(feature.preValidate(incompleteRoster, warningManager));

        // Verify warnings were generated
        boolean foundMissingHeaderWarning = false;
        for (Map.Entry<WarningType, ArrayList<RosterWarning>> entry : warningManager.getWarningLog().entrySet()) {
            if (entry.getKey() == WarningType.MISSING_FEATURE_HEADER) {
                for (RosterWarning warning : entry.getValue()) {
                    String[] data = warning.getDisplayData();
                    if (data.length > 0 && data[0].equals(RosterHeader.PREFERENCES.standardName)) {
                        foundMissingHeaderWarning = true;
                        break;
                    }
                }
            }
        }

        assertTrue(foundMissingHeaderWarning, "Should have warning for missing header");
    }

    @Test
    @DisplayName("Test feature application with complete data")
    public void testFeatureApplication() {
        Camper camper = createTestCamper(
            "Archery, Sports, Fishing",  // preferences
            "3",                         // round count
            "Archery", "Sports", "Swimming"  // assignments
        );

        roster.addCamper(camper);
        feature.applyFeature(roster, warningManager);

        // Verify all expected fields are populated
        assertTrue(camper.hasValue(RosterHeader.PREFERENCE_SCORE.standardName));
        assertTrue(camper.hasValue(RosterHeader.SCORE_BY_ROUND.standardName));
        assertTrue(camper.hasValue(RosterHeader.UNREQUESTED_ACTIVITIES.standardName));

        // Verify feature is enabled
        assertTrue(roster.hasFeature("preference"));
    }

    @Test
    @DisplayName("Test exempt activity management")
    void testExemptActivityManagement() {
        // Default exempt activities
        assertTrue(PreferenceFeatureUtils.isExemptActivity("Swimming"));
        assertTrue(PreferenceFeatureUtils.isExemptActivity("Horseback Riding"));
        assertFalse(PreferenceFeatureUtils.isExemptActivity("Archery"));

        // Add new exempt activity
        PreferenceFeature.addExemptActivity("Archery");
        assertTrue(PreferenceFeatureUtils.isExemptActivity("Archery"));
    }

    @ParameterizedTest
    @CsvSource({
       
        "'Archery, Sports, Fishing', 3, Sports, Fishing, Archery, 100",//top 3 requests = 100
        "'Archery, Sports, Fishing', 3, Swimming, Archery, Sports, 100",// 2 top requests + 1 unrequested/exempt = 100
        "'Archery, Sports, Fishing', 3, Swimming, Sports, Archery, 100",// Different order, same effect
        "'Archery, Sports, Fishing', 3, Archery, Sports, Swimming, 100",
        "'Archery, Sports, Fishing', 3, Archery, Swimming, Fishing, 95",
        "'Archery, Sports', 3, Water Polo, Biking, Sailing, 0"
    })
    @DisplayName("Test various preference scenarios")
    public void testPreferenceScenarios(String preferences, String roundCount, String r1, String r2, String r3, int expectedScore) {
        // Add Swimming to exempt activities to match test expectations
        if (!PreferenceFeatureUtils.isExemptActivity("Swimming")) {
            PreferenceFeature.addExemptActivity("Swimming");
        }

        Camper camper = createTestCamper(preferences, roundCount, r1, r2, r3);

        roster.addCamper(camper);
        feature.applyFeature(roster, warningManager);

        assertEquals(expectedScore,
            Integer.parseInt(camper.getValue(RosterHeader.PREFERENCE_SCORE.standardName)));
    }

    @Test
    @DisplayName("Test preference parsing")
    public void testPreferenceParsing() {
        // Test standard comma-separated list
        List<String> preferences1 = PreferenceFeatureUtils.parsePreferenceField("Archery, Sports, Fishing");
        assertEquals(3, preferences1.size());
        assertEquals("Archery", preferences1.get(0));
        assertEquals("Sports", preferences1.get(1));
        assertEquals("Fishing", preferences1.get(2));

        // Test list with 'and' conjunction
        List<String> preferences2 = PreferenceFeatureUtils.parsePreferenceField("Archery, Sports and Fishing");
        assertEquals(3, preferences2.size());
        assertEquals("Archery", preferences2.get(0));
        assertEquals("Sports", preferences2.get(1));
        assertEquals("Fishing", preferences2.get(2));

        // Test single item
        List<String> preferences3 = PreferenceFeatureUtils.parsePreferenceField("Archery");
        assertEquals(1, preferences3.size());
        assertEquals("Archery", preferences3.get(0));
    }

    @Test
    @DisplayName("Test unrequested activities detection")
    public void testUnrequestedActivitiesDetection() {
        Camper camper = createTestCamper(
            "Archery, Sports, Fishing",  // preferences
            "3",                         // round count
            "Archery", "Swimming", "Hiking"  // assignments
        );

        roster.addCamper(camper);
        feature.applyFeature(roster, warningManager);

        // Verify unrequested activities
        String unrequestedActivities = camper.getValue(RosterHeader.UNREQUESTED_ACTIVITIES.standardName);

        // Swimming is exempt, so it shouldn't be in unrequested activities
        assertFalse(unrequestedActivities.contains("Swimming"));
        assertTrue(unrequestedActivities.contains("Hiking"));
        assertFalse(unrequestedActivities.contains("Archery"));
    }

    @Test
    @DisplayName("Test percentile calculation")
    public void testPercentileCalculation() {
        // Create campers with different preference scores
        Camper camper1 = createTestCamper("Archery, Sports, Fishing", "3", "Archery", "Sports", "Fishing");
        Camper camper2 = createTestCamper("Archery, Sports, Fishing", "3", "Archery", "Sports", "Swimming");
        Camper camper3 = createTestCamper("Archery, Sports, Fishing", "3", "Swimming", "Biking", "Hiking");

        roster.addCamper(camper1);
        roster.addCamper(camper2);
        roster.addCamper(camper3);

        feature.applyFeature(roster, warningManager);

        // Verify percentiles
        // camper1 should have highest percentile (100)
        // camper2 should have middle percentile
        // camper3 should have lowest percentile
        int percentile1 = Integer.parseInt(camper1.getValue(RosterHeader.PREFERENCE_PERCENTILE.standardName));
        int percentile2 = Integer.parseInt(camper2.getValue(RosterHeader.PREFERENCE_PERCENTILE.standardName));
        int percentile3 = Integer.parseInt(camper3.getValue(RosterHeader.PREFERENCE_PERCENTILE.standardName));

        assertEquals(100, percentile1);
        assertTrue(percentile1 >= 0 && percentile1 <= 100);
        assertTrue(percentile2 >= 0 && percentile2 <= 100);
        assertTrue(percentile3 >= 0 && percentile3 <= 100);
    }

    @Test
    @DisplayName("Test handling of missing preference data")
    public void testMissingPreferenceData() {
        // Create a camper with no preference data
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "Missing");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Preferences");
        camperData.put(RosterHeader.ROUND_COUNT.standardName, "3");
        camperData.put(RosterHeader.ROUND_1.standardName, "Archery");
        camperData.put(RosterHeader.ROUND_2.standardName, "Sports");
        camperData.put(RosterHeader.ROUND_3.standardName, "Swimming");
        // Deliberately not setting preferences

        Camper camper = new Camper("missing_preferences", camperData);
        roster.addCamper(camper);

        // Apply the feature
        feature.applyFeature(roster, warningManager);

        // Verify that a warning was generated
        boolean foundMissingFieldWarning = false;
        for (Map.Entry<WarningType, ArrayList<RosterWarning>> entry : warningManager.getWarningLog().entrySet()) {
            if (entry.getKey() == WarningType.CAMPER_MISSING_FIELD) {
                for (RosterWarning warning : entry.getValue()) {
                    String[] data = warning.getDisplayData();
                    if (data.length > 1 && data[1].equals(RosterHeader.PREFERENCES.standardName)) {
                        foundMissingFieldWarning = true;
                        break;
                    }
                }
            }
        }

        assertTrue(foundMissingFieldWarning, "Should have warning for missing preference field");
    }

    @Test
    @DisplayName("Test postValidate method")
    public void testPostValidate() {
        // Should always return true for this feature
        assertTrue(feature.postValidate(roster, warningManager));
    }

    @Test
    @DisplayName("Test score by round format")
    public void testScoreByRoundFormat() {
        Camper camper = createTestCamper(
            "Archery, Sports, Fishing",  // preferences
            "3",                         // round count
            "Archery", "Swimming", "Fishing"  // assignments
        );

        roster.addCamper(camper);
        feature.applyFeature(roster, warningManager);

        // Verify score by round format
        String scoreByRound = camper.getValue(RosterHeader.SCORE_BY_ROUND.standardName);

        assertNotNull(scoreByRound);
        String[] scores = scoreByRound.split(", ");
        assertEquals(3, scores.length);
    }

    /**
     * Creates a test camper with the specified preferences and round assignments
     */
    private Camper createTestCamper(String preferences, String roundCount, String r1, String r2, String r3) {
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "Tess");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Camper");
        camperData.put(RosterHeader.ROUND_COUNT.standardName, roundCount);
        camperData.put(RosterHeader.ROUND_1.standardName, r1);
        camperData.put(RosterHeader.ROUND_2.standardName, r2);
        camperData.put(RosterHeader.ROUND_3.standardName, r3);
        camperData.put(RosterHeader.PREFERENCES.standardName, preferences);

        return new Camper("camperID", camperData);
    }
}