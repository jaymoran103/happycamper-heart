package com.echo.validation;

import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.echo.ReflectionUtils;
import com.echo.domain.RosterHeader;

/**
 * Tests for the RosterRegexBuilder class.
 */
public class RosterRegexBuilderTest {

    @Test
    @DisplayName("buildCamperFormats should return non-empty map")
    public void testBuildCamperFormats() {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildCamperFormats();
        assertNotNull(formatMap, "Format map should not be null");
        assertFalse(formatMap.isEmpty(), "Format map should not be empty");
    }

    @Test
    @DisplayName("buildActivityFormats should return non-empty map")
    public void testBuildActivityFormats() {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildActivityFormats();
        assertNotNull(formatMap, "Format map should not be null");
        assertFalse(formatMap.isEmpty(), "Format map should not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Session 1/Traditional Camp",
        "Session 2/Backpacking",
        "Session 3A/Leadership",
        "Session 4/Traditional Camp and Memorial Day Family Camp",
        "Session 1/Traditional Camp and Session 2/Backpacking"
    })
    @DisplayName("Camper formats should match valid session/program formats")
    public void testCamperFormats_ValidSessionsPrograms(String input) {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildCamperFormats();
        Pattern pattern = formatMap.get(RosterHeader.ESP.camperRosterName);
        assertNotNull(pattern, "Pattern should exist for ESP");
        assertTrue(pattern.matcher(input).matches(), "Pattern should match valid input: " + input);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Session X/Invalid",
        "Session/Missing Program",
        "Traditional Camp",
        "1/Traditional Camp",
        "Session 1 Traditional Camp"
    })
    @DisplayName("Camper formats should reject invalid session/program formats")
    public void testCamperFormats_InvalidSessionsPrograms(String input) {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildCamperFormats();
        Pattern pattern = formatMap.get(RosterHeader.ESP.camperRosterName);
        assertNotNull(pattern, "Pattern should exist for ESP");
        assertFalse(pattern.matcher(input).matches(), "Pattern should reject invalid input: " + input);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th", "11th", "12th", "12th+"
    })
    @DisplayName("Camper formats should match valid grade formats")
    public void testCamperFormats_ValidGrades(String input) {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildCamperFormats();
        Pattern pattern = formatMap.get(RosterHeader.GRADE.camperRosterName);
        assertNotNull(pattern, "Pattern should exist for Camp Grade");
        assertTrue(pattern.matcher(input).matches(), "Pattern should match valid input: " + input);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0th", "13th", "6", "Grade 6", "fifth", "K", "Kindergarten"
    })
    @DisplayName("Camper formats should reject invalid grade formats")
    public void testCamperFormats_InvalidGrades(String input) {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildCamperFormats();
        Pattern pattern = formatMap.get(RosterHeader.GRADE.camperRosterName);
        assertNotNull(pattern, "Pattern should exist for Camp Grade");
        assertFalse(pattern.matcher(input).matches(), "Pattern should reject invalid input: " + input);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "John", "Mary-Kate", "Mary Kate", "O'Connor", "Smith Jr.", "García", "Müller"
    })
    @DisplayName("Camper formats should match valid name formats")
    public void testCamperFormats_ValidNames(String input) {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildCamperFormats();
        Pattern firstNamePattern = formatMap.get(RosterHeader.FIRST_NAME.camperRosterName);
        Pattern lastNamePattern = formatMap.get(RosterHeader.LAST_NAME.camperRosterName);
        Pattern prefNamePattern = formatMap.get(RosterHeader.PREFERRED_NAME.camperRosterName);

        assertNotNull(firstNamePattern, "Pattern should exist for First Name");
        assertNotNull(lastNamePattern, "Pattern should exist for Last Name");
        assertNotNull(prefNamePattern, "Pattern should exist for Preferred Name");

        assertTrue(firstNamePattern.matcher(input).matches(), "First Name pattern should match valid input: " + input);
        assertTrue(lastNamePattern.matcher(input).matches(), "Last Name pattern should match valid input: " + input);
        assertTrue(prefNamePattern.matcher(input).matches(), "Preferred Name pattern should match valid input: " + input);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2", "3"})
    @DisplayName("Activity formats should match valid period values")
    public void testActivityFormats_ValidPeriods(String input) {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildActivityFormats();
        Pattern pattern = formatMap.get(RosterHeader.ROUND.activityRosterName);
        assertNotNull(pattern, "Pattern should exist for Period");
        assertTrue(pattern.matcher(input).matches(), "Pattern should match valid input: " + input);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "4", "A", "Period 1"})
    @DisplayName("Activity formats should reject invalid period values")
    public void testActivityFormats_InvalidPeriods(String input) {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildActivityFormats();
        Pattern pattern = formatMap.get(RosterHeader.ROUND.activityRosterName);
        assertNotNull(pattern, "Pattern should exist for Period");
        assertFalse(pattern.matcher(input).matches(), "Pattern should reject invalid input: " + input);
    }

    @Test
    @DisplayName("buildPreferenceFormats should return non-empty map")
    public void testBuildPreferenceFormats() {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildPreferenceFormats();
        assertNotNull(formatMap, "Format map should not be null");
        assertFalse(formatMap.isEmpty(), "Format map should not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Archery, Swimming, Hiking",
        "Archery, Swimming, Hiking and Biking",
        "Arts & Crafts, Friendship Bracelet, Archery, Sailing, Sports, Nature, Open Toed Shoes, Fairy, Paddle Sports and Gold Swimming"
    })
    @DisplayName("Preference formats should match valid preference lists")
    public void testPreferenceFormats_ValidLists(String input) {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildPreferenceFormats();
        Pattern pattern = formatMap.get(RosterHeader.PREFERENCES.standardName);
        assertNotNull(pattern, "Pattern should exist for Preferences");
        assertTrue(pattern.matcher(input).matches(), "Pattern should match valid input: " + input);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",", "Archery,", ", Swimming"})
    @DisplayName("Preference formats should reject invalid preference lists")
    public void testPreferenceFormats_InvalidLists(String input) {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildPreferenceFormats();
        Pattern pattern = formatMap.get(RosterHeader.PREFERENCES.standardName);
        assertNotNull(pattern, "Pattern should exist for Preferences");
        assertFalse(pattern.matcher(input).matches(), "Pattern should reject invalid input: " + input);
    }

    @ParameterizedTest
    @ValueSource(strings = {"85", "85.5", "100", "0"})
    @DisplayName("Preference formats should match valid preference scores")
    public void testPreferenceFormats_ValidScores(String input) throws Exception {
        // Access the private pattern directly using reflection
        String patternString = ReflectionUtils.getStaticFieldValue(RosterRegexBuilder.class, "PREFERENCE_SCORE");
        Pattern pattern = Pattern.compile(patternString);
        assertNotNull(pattern, "Pattern should exist for Preference Score");
        assertTrue(pattern.matcher(input).matches(), "Pattern should match valid input: " + input);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "A", "-1", "101%"})
    @DisplayName("Preference formats should reject invalid preference scores")
    public void testPreferenceFormats_InvalidScores(String input) throws Exception {
        // Access the private pattern directly using reflection
        String patternString = ReflectionUtils.getStaticFieldValue(RosterRegexBuilder.class, "PREFERENCE_SCORE");
        Pattern pattern = Pattern.compile(patternString);
        assertNotNull(pattern, "Pattern should exist for Preference Score");
        assertFalse(pattern.matcher(input).matches(), "Pattern should reject invalid input: " + input);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10,9,8", "0,0,0"})
    @DisplayName("Preference formats should match valid round scores")
    public void testPreferenceFormats_ValidRoundScores(String input) throws Exception {
        String patternString = ReflectionUtils.getStaticFieldValue(RosterRegexBuilder.class, "SCORE_BY_ROUND");
        Pattern pattern = Pattern.compile(patternString);
        assertNotNull(pattern, "Pattern should exist for Score By Round");
        assertTrue(pattern.matcher(input).matches(), "Pattern should match valid input: " + input);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "10,9", "10,9,8,7", "A,B,C"})
    @DisplayName("Preference formats should reject invalid round scores")
    public void testPreferenceFormats_InvalidRoundScores(String input) throws Exception {
        String patternString = ReflectionUtils.getStaticFieldValue(RosterRegexBuilder.class, "SCORE_BY_ROUND");
        Pattern pattern = Pattern.compile(patternString);
        assertNotNull(pattern, "Pattern should exist for Score By Round");
        assertFalse(pattern.matcher(input).matches(), "Pattern should reject invalid input: " + input);
    }

    @Test
    @DisplayName("buildProgramFormats should return non-empty map")
    public void testBuildProgramFormats() {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildProgramFormats();
        assertNotNull(formatMap, "Format map should not be null");
        assertFalse(formatMap.isEmpty(), "Format map should not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Session 1/Trailblazers",
        "Session 1A/Trailblazers",
        "Session 2-3/Explorers",
        "Session 1/Trailblazers and Session 2/Explorers",
        "Family Camp/Summer",
        "Echo Corps/Leadership"
    })
    @DisplayName("Program formats should match valid ESP formats")
    public void testProgramFormats_ValidESP(String input) {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildProgramFormats();
        Pattern pattern = formatMap.get(RosterHeader.ESP.camperRosterName);
        assertNotNull(pattern, "Pattern should exist for ESP");
        assertTrue(pattern.matcher(input).matches(), "Pattern should match valid input: " + input);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Trailblazers",
        "Explorers",
        "Arts and Crafts",
        "Leadership Program"
    })
    @DisplayName("Program formats should match valid program names")
    public void testProgramFormats_ValidPrograms(String input) {
        Map<String, Pattern> formatMap = RosterRegexBuilder.buildProgramFormats();
        Pattern pattern = formatMap.get(RosterHeader.PROGRAM.standardName);
        assertNotNull(pattern, "Pattern should exist for Program");
        assertTrue(pattern.matcher(input).matches(), "Pattern should match valid input: " + input);
    }

}
