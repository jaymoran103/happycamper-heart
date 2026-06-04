package com.echo.domain;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for the RosterHeader enum.
 */
public class RosterHeaderTest {

    @Test
    @DisplayName("Enum values should have correct properties")
    public void testEnumProperties() {
        // Test a few representative enum values
        assertEquals("First Name", RosterHeader.FIRST_NAME.camperRosterName);
        assertEquals("First Name", RosterHeader.FIRST_NAME.standardName);
        assertFalse(RosterHeader.FIRST_NAME.defaultVisibility);

        assertEquals("Last Name", RosterHeader.LAST_NAME.camperRosterName);
        assertEquals("Last Name", RosterHeader.LAST_NAME.standardName);
        assertTrue(RosterHeader.LAST_NAME.defaultVisibility);

        assertEquals("Enrolled Sessions/Programs", RosterHeader.ESP.camperRosterName);
        assertEquals("Enrolled Sessions/Programs", RosterHeader.ESP.standardName);
        assertFalse(RosterHeader.ESP.defaultVisibility);

        assertEquals("Round 1", RosterHeader.ROUND_1.standardName);
        assertTrue(RosterHeader.ROUND_1.defaultVisibility);

        assertEquals("Preference Score", RosterHeader.PREFERENCE_SCORE.standardName);
        assertTrue(RosterHeader.PREFERENCE_SCORE.defaultVisibility);
    }

    @Test
    @DisplayName("updateHeaderMapOrder should sort headers correctly")
    public void testUpdateHeaderMapOrder() {
        // Create a header map with headers in random order
        Map<String, Integer> headerMap = new HashMap<>();
        headerMap.put(RosterHeader.ROUND_3.standardName, 0);
        headerMap.put(RosterHeader.FIRST_NAME.standardName, 1);
        headerMap.put(RosterHeader.LAST_NAME.standardName, 2);
        headerMap.put(RosterHeader.GRADE.standardName, 3);
        headerMap.put(RosterHeader.ROUND_1.standardName, 4);
        headerMap.put(RosterHeader.ROUND_2.standardName, 5);
        headerMap.put(RosterHeader.PROGRAM.standardName, 6);

        // Update the order
        RosterHeader.updateHeaderMapOrder(headerMap);

        // Verify the order
        // Name fields should come first
        assertTrue(headerMap.get(RosterHeader.FIRST_NAME.standardName) < headerMap.get(RosterHeader.GRADE.standardName));
        assertTrue(headerMap.get(RosterHeader.LAST_NAME.standardName) < headerMap.get(RosterHeader.GRADE.standardName));

        // Round fields should be in order
        assertTrue(headerMap.get(RosterHeader.ROUND_1.standardName) < headerMap.get(RosterHeader.ROUND_2.standardName));
        assertTrue(headerMap.get(RosterHeader.ROUND_2.standardName) < headerMap.get(RosterHeader.ROUND_3.standardName));
    }

    @ParameterizedTest
    @CsvSource({
        "First Name, FIRST_NAME",
        "Last Name, LAST_NAME",
        "Grade, GRADE",
        "Enrolled Sessions/Programs, ESP",
        "Round 1, ROUND_1",
        "Round 2, ROUND_2",
        "Round 3, ROUND_3",
        "Rounds Assigned, ROUND_COUNT",
        "Program, PROGRAM",
        "Preference Score, PREFERENCE_SCORE"
    })
    @DisplayName("determineHeaderType should recognize header names")
    public void testDetermineHeaderType(String headerName, String expectedEnum) {
        RosterHeader header = RosterHeader.determineHeaderType(headerName);
        assertNotNull(header, "Should recognize header name");
        assertEquals(RosterHeader.valueOf(expectedEnum), header, "Should return correct enum value");
    }

    @ParameterizedTest
    @CsvSource({
        "First Name, FIRST_NAME, com.echo.domain.EnhancedRoster",
        "Last Name, LAST_NAME, com.echo.domain.EnhancedRoster",
        "First Name, FIRST_NAME, com.echo.domain.CamperRoster",
        "Last Name, LAST_NAME, com.echo.domain.CamperRoster",
        "Activity, ACTIVITY, com.echo.domain.ActivityRoster",
        "Period, ROUND, com.echo.domain.ActivityRoster"
    })
    @DisplayName("determineHeaderType with class should recognize header names")
    public void testDetermineHeaderTypeWithClass(String headerName, String expectedEnum, String className) throws ClassNotFoundException {
        Class<?> rosterClass = Class.forName(className);
        RosterHeader header = RosterHeader.determineHeaderType(headerName, rosterClass);
        assertNotNull(header, "Should recognize header name");
        assertEquals(RosterHeader.valueOf(expectedEnum), header, "Should return correct enum value");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Unknown Header", "Not a Header", ""})
    @DisplayName("determineHeaderType should return null for unknown headers")
    public void testDetermineHeaderTypeUnknown(String headerName) {
        RosterHeader header = RosterHeader.determineHeaderType(headerName);
        assertEquals(null, header, "Should return null for unknown header");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Unknown Header", "Not a Header", ""})
    @DisplayName("determineHeaderType with class should return null for unknown headers")
    public void testDetermineHeaderTypeWithClassUnknown(String headerName) throws ClassNotFoundException {
        Class<?> rosterClass = Class.forName("com.echo.domain.EnhancedRoster");
        RosterHeader header = RosterHeader.determineHeaderType(headerName, rosterClass);
        assertEquals(null, header, "Should return null for unknown header");
    }
}
