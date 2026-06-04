package com.echo.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.echo.logging.RosterException;
import com.echo.logging.RosterWarning;
import com.echo.logging.WarningManager;
import com.echo.validation.RosterRegexBuilder;

/**
 * Specialized roster for activity assignment data.
 *
 * Extends the base roster to provide functionality specific to campminder-generated elective rosters
 *
 * Works with Camper Roster to provide a complete picture of camper activity assignments. (output in an enhanced roster)
 * Tracks activity assignments, omitted activities, and duplicate assignments.
 */
public class ActivityRoster extends Roster {
    /**
     * List of headers that must be present in a valid activity roster.
     * These headers are checked during validation to ensure the roster has all required data.
     */
    private static final List<String> REQUIRED_HEADERS = Arrays.asList(
        // Specific to each assignment
        RosterHeader.ACTIVITY.activityRosterName,
        RosterHeader.ROUND.activityRosterName,

        //Specific to each camper, assumed to be the same across assignments for a given camper
        RosterHeader.PREFERRED_NAME.activityRosterName,
        RosterHeader.LAST_NAME.activityRosterName,
        RosterHeader.FIRST_NAME.activityRosterName,
        RosterHeader.CABIN.activityRosterName,
        RosterHeader.GRADE.activityRosterName
    );

    /**
     * Patterns used to validate the format of specific fields in the activity roster.
     * Each entry maps a field name to a regular expression pattern that valid values must match.
     */
    private static final Map<String, Pattern> VALIDATION_PATTERNS;

    static {
        // Get validation patterns from the centralized RosterRegexBuilder
        VALIDATION_PATTERNS = Collections.unmodifiableMap(RosterRegexBuilder.buildActivityFormats());
    }

    /**
     * Creates a new empty ActivityRoster with no headers or campers.
     * Headers and campers must be added separately after creation.
     */
    public ActivityRoster() {
        super();
    }

    /**
     * Validates that the roster has all required headers and that all activity data is valid.
     * This method checks both the roster structure (headers) and the content of each activity entry.
     * Issues are logged to the warning manager rather than throwing exceptions for non-critical problems.
     *
     * @param warningManager The warning manager to use for logging validation issues
     * @throws RosterException if critical validation fails (missing required headers)
     */
    public void validate(WarningManager warningManager) throws RosterException {
        // Check for required headers - should have been checked in import step but cost is minimal
        // Set<String> headerSet = new HashSet<>(getHeaderMap().keySet());
        // ValidationUtils.validateHeaders("Activity Roster",headerSet, REQUIRED_HEADERS);

        // Validate data in each row
        for (Camper activity : getCampers()) {
            validateActivity(activity,warningManager);
        }
    }

    /**
     * Gets the activity data organized by camper ID for efficient lookup.
     * This method creates a map where each key is a camper ID and the value is
     * the map of that camper's activity data.
     *
     * @return A map of camper IDs to their activity data maps
     */
    public Map<String, Map<String, String>> getKeyedData() {
        Map<String, Map<String, String>> keyedData = new HashMap<>();
        for (Camper activityData : getCampers()) {
            String key = activityData.getId();
            keyedData.put(key, activityData.getData());
        }
        return keyedData;
    }

    /**
     * Validates a single activity entry for required fields and correct data formats.
     * This method checks that the activity has all required fields and that the values
     * match the expected formats. Issues are logged to the warning manager.
     *
     * @param activity The activity entry to validate
     * @param warningManager The warning manager to log validation issues
     * @throws RosterException if critical validation fails (missing required fields)
     */
    private void validateActivity(Camper activity,WarningManager warningManager) throws RosterException {
        // Check for required fields
        Roster.validateHeaders(activity.getData().keySet(), REQUIRED_HEADERS);
        
        // Validate field formats
        for (Map.Entry<String, Pattern> entry : VALIDATION_PATTERNS.entrySet()) {
            String field = entry.getKey();
            Pattern pattern = entry.getValue();

            String value = activity.getValue(field);
            if (value != null && !pattern.matcher(value).matches()) {
                RosterWarning warning = RosterWarning.create_badDataFormat(activity.getData(),field,pattern.toString());
                warningManager.logWarning(warning);
            }
        }
    }

    /**
     * Gets the list of headers that must be present in a valid activity roster.
     * This method is used by external components to check if a file has the necessary
     * headers before attempting to load it as an activity roster.
     *
     * @return An unmodifiable list of required header names
     */
    public static List<String> getRequiredHeaders() {
        return REQUIRED_HEADERS;
    }

    /**
     * Generates a unique camper ID from an activity data row.
     * This method creates an ID that matches the ID generation in the camper roster,
     * allowing activities to be linked to the correct campers. The ID is based on
     * the camper's first name, last name, and grade level.
     *
     * FUTURE - move this and camper counterpart to single method with variable header names based on roster type
     *
     * @param activityData The map containing activity and camper data
     * @return A unique camper ID string in the format "firstname_lastname_grade"
     */
    public static String generateCamperIdFromActivity(Map<String, String> activityData) {
        // Use the activityRosterName if available, otherwise fall back to direct string

        String firstName = activityData.getOrDefault(RosterHeader.FIRST_NAME.activityRosterName, "");
        String lastName = activityData.getOrDefault(RosterHeader.LAST_NAME.activityRosterName, "");
        String grade = activityData.getOrDefault(RosterHeader.GRADE.activityRosterName, "");

        return String.format("%s_%s_%s", firstName, lastName, grade)
                .toLowerCase()
                .replace(" ", "_");
    }
}
