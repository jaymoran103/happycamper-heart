package com.echo.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.echo.domain.RosterHeader;

/**
 * Utility class for building regex patterns used in roster validation.
 * Provides methods to create pattern maps for validating camper and activity data.
 *
 * More regex variations exist in RosterRegexBuilder in version 1
 */
public class RosterRegexBuilder {

    // Sessions/Programs patterns
    private static final String SIMPLE_SESSIONS_PROGRAMS = "(.*/.*)( and .*/.*)";
    private static final String COMPLEX_SESSIONS_PROGRAMS = "(Session [1-6][AB]?(-[1-6][AB]?)?/[A-Za-z ,\\-]+)( and (Session [1-6][AB]?(-[1-6][AB]?)?/[A-Za-z ,\\-]+))*";
    private static final String COMPLEX_SESSIONS_PROGRAMS_EXTRAS = "(Session [1-6][AB]?(-[1-6][AB]?)?/[A-Za-z ,\\-]+|.*Family Camp.*|.*Echo Corps.*)( and (Session [1-6][AB]?(-[1-6][AB]?)?/[A-Za-z ,\\-]+|.*Family Camp.*|.*Echo Corps.*))*";

    // Names patterns
    private static final String INCLUSIVE_NAMES = "^[A-Za-zÁÉÍÓÚáéíóúÑñüÜ ''.,/\\-\\(\\)]+$";
    private static final String SIMPLISH_NAMES = "^[A-Za-zÁÉÍÓÚáéíóúÑñüÜ -]+$";
    private static final String SIMPLE_NAMES = "^[A-Za-z -]+$";

    // Grades patterns
    private static final String LAZY_GRADES = "^([0-9]{1,2}(th|nd|st))$";
    private static final String VALID_GRADES_LIST = "^(1st|2nd|3rd|4th|5th|6th|7th|8th|9th|10th|11th|12th|12th\\+)$";
    private static final String BETTER_GRADES = "^(1st|2nd|3rd|[4-9]th|1[0-2]th|12th\\+)$";
    private static final String NUMERIC_GRADES = "^\\d+$|^\\d+th$|^\\d+nd$|^\\d+rd$|^\\d+st$";

    // Activity round pattern
    private static final String PERIOD_REGEX = "^(1|2|3)$";

    // Preference patterns
    private static final String PREFERENCE_LIST = "^[^,]+(,[^,]+)*(\\s+and\\s+[^,]+)?$";
    private static final String PREFERENCE_RANKING = "^([1-9][0-9]?)$";
    private static final String PREFERENCE_SCORE = "^\\d+(\\.\\d+)?$";
    private static final String PREFERENCE_PERCENTILE = "^\\d+$";
    private static final String SCORE_BY_ROUND = "^\\d+,\\d+,\\d+$";

    /**
     * Builds a map of regex patterns for validating camper data.
     *
     * @return A map of field names to regex patterns
     */
    public static Map<String, Pattern> buildCamperFormats() {
        Map<String, Pattern> formatMap = new HashMap<>();

        formatMap.put(RosterHeader.ESP.camperRosterName, Pattern.compile(COMPLEX_SESSIONS_PROGRAMS_EXTRAS));
        formatMap.put(RosterHeader.GRADE.camperRosterName, Pattern.compile(BETTER_GRADES));
        formatMap.put(RosterHeader.PREFERRED_NAME.camperRosterName, Pattern.compile(INCLUSIVE_NAMES));
        formatMap.put(RosterHeader.LAST_NAME.camperRosterName, Pattern.compile(INCLUSIVE_NAMES));
        formatMap.put(RosterHeader.FIRST_NAME.camperRosterName, Pattern.compile(INCLUSIVE_NAMES));

        // Add entries for test compatibility
        formatMap.put("Enrolled Sessions/Programs", Pattern.compile(COMPLEX_SESSIONS_PROGRAMS_EXTRAS));
        formatMap.put("Camp Grade", Pattern.compile(BETTER_GRADES));
        formatMap.put("Preferred Name", Pattern.compile(INCLUSIVE_NAMES));
        formatMap.put("Last Name", Pattern.compile(INCLUSIVE_NAMES));
        formatMap.put("First Name", Pattern.compile(INCLUSIVE_NAMES));

        return formatMap;
    }

    /**
     * Builds a map of regex patterns for validating activity data.
     *
     * @return A map of field names to regex patterns
     */
    public static Map<String, Pattern> buildActivityFormats() {
        Map<String, Pattern> formatMap = new HashMap<>();

        formatMap.put(RosterHeader.ROUND.activityRosterName, Pattern.compile(PERIOD_REGEX));
        formatMap.put(RosterHeader.PREFERRED_NAME.activityRosterName, Pattern.compile(INCLUSIVE_NAMES));
        formatMap.put(RosterHeader.LAST_NAME.activityRosterName, Pattern.compile(INCLUSIVE_NAMES));
        formatMap.put(RosterHeader.GRADE.activityRosterName, Pattern.compile(VALID_GRADES_LIST));
        formatMap.put(RosterHeader.ACTIVITY.activityRosterName, Pattern.compile("^.+$"));  // Activities must have a non-empty name

        // Add entries for test compatibility
        formatMap.put("Period", Pattern.compile(PERIOD_REGEX));
        formatMap.put("Preferred Name", Pattern.compile(INCLUSIVE_NAMES));
        formatMap.put("Last Name", Pattern.compile(INCLUSIVE_NAMES));
        formatMap.put("Grade", Pattern.compile(VALID_GRADES_LIST));
        formatMap.put("Activity", Pattern.compile("^.+$"));  // Activities must have a non-empty name

        return formatMap;
    }

    /**
     * Builds a map of regex patterns for validating preference data.
     * These patterns are used to validate the format of preference-related fields
     * such as preference lists, rankings, scores, and percentiles.
     * 
     * NOTE: only the PREFERENCES field should matter, since the others aren't input files and aren't further manuipulated after being created 
     *
     * @return A map of field names to regex patterns
     */
    public static Map<String, Pattern> buildPreferenceFormats() {
        Map<String, Pattern> formatMap = new HashMap<>();

        // Preference list format (comma-separated with optional "and" before the last item)
        formatMap.put(RosterHeader.PREFERENCES.camperRosterName, Pattern.compile(PREFERENCE_LIST));
        formatMap.put(RosterHeader.PREFERENCES.standardName, Pattern.compile(PREFERENCE_LIST));

        // Preference score format (numeric value with optional decimal places)
        // formatMap.put(RosterHeader.PREFERENCE_SCORE.standardName, Pattern.compile(PREFERENCE_SCORE));

        // Preference percentile format (integer percentage)
        // formatMap.put(RosterHeader.PREFERENCE_PERCENTILE.standardName, Pattern.compile(PREFERENCE_PERCENTILE));

        // Round-specific preference scores
        // formatMap.put(RosterHeader.SCORE_BY_ROUND.standardName, Pattern.compile("^\\d+,\\d+,\\d+$"));

        return formatMap;
    }

    /**
     * Builds a map of regex patterns for validating program data.
     * These patterns are used to validate the format of program-related fields
     * such as session information and program names.
     *
     * @return A map of field names to regex patterns
     */
    public static Map<String, Pattern> buildProgramFormats() {
        Map<String, Pattern> formatMap = new HashMap<>();

        // Session/program format
        formatMap.put(RosterHeader.ESP.camperRosterName, Pattern.compile(COMPLEX_SESSIONS_PROGRAMS_EXTRAS));
        formatMap.put(RosterHeader.PROGRAM.standardName, Pattern.compile("^[A-Za-z ,\\-]+$"));//TODO add check for a solo 'and' that might throw off the final token check?

        return formatMap;
    }
}
