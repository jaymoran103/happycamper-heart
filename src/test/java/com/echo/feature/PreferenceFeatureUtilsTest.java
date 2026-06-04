package com.echo.feature;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.echo.domain.Camper;
import com.echo.domain.RosterHeader;

class PreferenceFeatureUtilsTest {

    @BeforeEach
    void resetExemptActivities() {
        new PreferenceFeature();
    }

    @Test
    @DisplayName("Test preference field parsing")
    void testPreferenceFieldParsing() {
        String input = "Archery, Sports, Fishing and Water Polo";
        List<String> expected = Arrays.asList("Archery", "Sports", "Fishing", "Water Polo");

        assertEquals(expected, PreferenceFeatureUtils.parsePreferenceField(input));
    }

    @Test
    @DisplayName("Test round points calculation")
    void testRoundPointsCalculation() {
        List<String> preferences = Arrays.asList("Arts & Crafts", "Sports", "Fishing");
        String[] assignments = {"Arts & Crafts", "Water Polo", "Sports"};

        int[] expected = {10, 0, 9};
        int[] roundPoints = PreferenceFeatureUtils.determineRoundPoints(preferences, assignments);

        assertEquals(10, roundPoints[0]);
        assertEquals(0, roundPoints[1]);
        assertEquals(9, roundPoints[2]);
        // assertArrayEquals(expected,roundPoints);
    }

    @Test
    @DisplayName("Test unrequested activities determination")
    void testUnrequestedActivities() {
        List<String> preferences = Arrays.asList("Archery", "Sports");
        String[] assignments = {"Archery", "Water Polo", "Biking"};

        List<String> expected = Arrays.asList("Water Polo", "Biking");
        Camper testCamper = createTestCamper("Archery, Sports", "3", "Archery", "Water Polo", "Biking");
        assertEquals(expected, PreferenceFeatureUtils.determineUnrequestedActivities(testCamper, preferences, assignments));
    }

    @ParameterizedTest
    @CsvSource({
        "1st, 10",    // First choice gets 10 points
        "2nd, 9",     // Second choice gets 9 points
        "3rd, 8",     // Third choice gets 8 points
        "10th, 1",    // Last choice gets 1 point
        "NotInList, 0"  // Activity not in preferences gets 0 points
    })
    @DisplayName("Test activity scoring")
    void testActivityScoring(String activity, int expectedScore) {
        List<String> preferences = Arrays.asList(
            "1st", "2nd", "3rd", "4th", "5th","6th", "7th", "8th", "9th", "10th");

        int score = PreferenceFeatureUtils.scoreActivity(preferences, activity);
        assertEquals(expectedScore, score);
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
        camperData.put("Activity Preferences", preferences);

        return new Camper("camperID", camperData);
    }


} 