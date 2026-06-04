package com.echo.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.echo.domain.ActivityRoster;
import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.logging.RosterWarning;
import com.echo.logging.WarningManager;
import com.echo.validation.RosterRegexBuilder;

/**
 * Central feature of the application, adds activity assignment functionality to a roster.
 *
 * This feature iterates through an activity roster, linking assignments to campers for an enhancedRoster
 * Tallies round counts, which are later used for key filtering and reporting tools.
 *
 * This feature is a prerequisite for most other features as it establishes the basic activity structure that other features build upon.
 *
 * The feature handles several edge cases:
 * - Duplicate activity assignments for the same camper in the same round
 * - Activities that don't match any camper in the roster ("orphaned" activities)
 * - Activities not associated any campers present in the camper roster
 */
public class ActivityFeature implements RosterFeature {
    /**
     * Flag to control whether to include orphaned activities (activities without matching campers).
     * This can be set by the user through the UI to control how orphaned activities are handled.
     */
    public static boolean INCLUDE_ORPHANS = true;

    /** Unique identifier for this feature */
    private static final String FEATURE_ID = "activity";

    /** Display name for this feature */
    private static final String FEATURE_NAME = "Activity Assignments";

    /** Maximum number of rounds supported by the application */
    static final int MAX_ROUNDS = 3;

    /** Header for the count of rounds assigned to a camper */
    private static final String ROUNDS_ASSIGNED_HEADER = RosterHeader.ROUND_COUNT.standardName;

    /**
     * Headers required by this feature.
     * The ActivityFeature doesn't require specific headers in the enhanced roster
     * because it gets its data from the separate ActivityRoster.
     */
    private static final List<String> REQUIRED_HEADERS = new ArrayList<>();

    /**
     * Headers added by this feature to the enhanced roster.
     * These include the round-specific activity headers and the count of assigned rounds.
     *
     * The headers are dynamically generated based on the available rounds (currently 1-3).
     */
    private static final List<String> ADDED_HEADERS;

    static {
        List<String> headers = new ArrayList<>();
        // Add headers for each round
        for (int i = 1; i <= MAX_ROUNDS; i++) {
            headers.add(RosterHeader.buildRoundString(i));
        }
        // Add the rounds assigned header
        headers.add(ROUNDS_ASSIGNED_HEADER);
        ADDED_HEADERS = Collections.unmodifiableList(headers);
    }

    /**
     * Required formats for activity data validation.
     * - Round must be a single digit 1-3
     * - Activity must be a non-empty string
     */
    private static final Map<String, String> REQUIRED_FORMATS;

    static {
        // Convert Pattern objects to String representations for the interface
        Map<String, Pattern> patternMap = RosterRegexBuilder.buildActivityFormats();
        Map<String, String> formats = new HashMap<>();

        for (Map.Entry<String, Pattern> entry : patternMap.entrySet()) {
            formats.put(entry.getKey(), entry.getValue().pattern());
        }

        REQUIRED_FORMATS = Collections.unmodifiableMap(formats);
    }



    @Override
    public String getFeatureId() {
        return FEATURE_ID;
    }

    @Override
    public String getFeatureName() {
        return FEATURE_NAME;
    }

    @Override
    public List<String> getRequiredHeaders() {
        return REQUIRED_HEADERS;
    }

    @Override
    public List<String> getAddedHeaders() {
        return ADDED_HEADERS;
    }

    @Override
    public Map<String, String> getRequiredFormats() {
        return REQUIRED_FORMATS;
    }

    /**
     * Standard applyFeature method that is overridden to prevent incorrect usage.
     * The ActivityFeature requires an ActivityRoster to function, so this method
     * throws an exception to prevent it from being called without the necessary data.
     * @throws UnsupportedOperationException Always thrown to indicate this method should not be used
     */
    @Override
    public void applyFeature(EnhancedRoster roster, WarningManager warningManager) {
        throw new UnsupportedOperationException("Activity feature requires an ActivityRoster - use the overloaded version of applyFeature");
    }


    /**
     * Validates that the roster has all prerequisites for this feature.
     * Since the ActivityFeature gets its data from a separate ActivityRoster,
     * there are no specific prerequisites to check in the enhanced roster.
     *
     * @param roster The roster to validate
     * @param warningManager The warning manager to use for logging issues
     * @return Always returns true as there are no prerequisites to check
     */
    @Override
    public boolean preValidate(EnhancedRoster roster, WarningManager warningManager) {
        return true;
    }

    /**
     * Validates that the feature was applied correctly.
     * Currently, this method always returns true as there are no specific
     * post-validation checks implemented for the ActivityFeature.
     *
     * @param roster The roster to validate
     * @param warningManager The warning manager to use for logging issues
     * @return Always returns true as there are no post-validation checks implemented
     */
    @Override
    public boolean postValidate(EnhancedRoster roster, WarningManager warningManager) {
        return true;
    }




    /**
     * Processes activity data and adds it to the enhanced roster.
     * This is the main method that implements the feature's functionality:
     * - Adds the necessary headers to the roster
     * - Arranges activities by camper ID to handle multiple activities per camper
     * - Applies the arranged activity data to the roster
     * - Updates the assignment counts for each camper
     * - Enables the feature in the roster
     *
     * @param roster The enhanced roster to add activity data to
     * @param activityRoster The activity roster containing the source activity data
     * @param warningManager The warning manager to use for logging issues
     */
     public void applyFeature(EnhancedRoster roster, ActivityRoster activityRoster, WarningManager warningManager) {

        // Add new headers
        for (String header : getAddedHeaders()) {
            roster.addHeader(header);
        }

        // First pass: collect all activities for each camper
        List<Map<String, String>> activityDataList = activityRoster.getCampers().stream().map(Camper::getData).toList();
        Map<String, Map<String, String>> mergedActivities = arrangeActivitiesByCamper(activityDataList, warningManager);

        // Second pass: update the roster with merged activity data
        applyActivityDataToRoster(roster, mergedActivities, warningManager);

        // Third step:Update assignment counts
        updateAssignmentCounts(roster);

        // Finally enable this feature
        roster.enableFeature(FEATURE_ID);
    }

    /**
     * Arranges activity data by camper ID to handle multiple activities per camper.
     * This method iterates through all activity data rows and organizes them by camper ID,
     * merging multiple activities for the same camper into a single map. It also handles
     * duplicate activity assignments by logging warnings and skipping the duplicates.
     *
     * @param activityDataList List of activity data rows (maps of header/value pairs)
     * @param warningManager The warning manager to use for logging issues
     * @return Map of merged activity data, keyed by generated camper IDs
     */
    private Map<String, Map<String, String>> arrangeActivitiesByCamper(List<Map<String, String>> activityDataList, WarningManager warningManager) {
        Map<String, Map<String, String>> mergedActivities = new HashMap<>();

        for (Map<String, String> row : activityDataList) {
            try {
                // Check for invalid round number
                String roundValue = row.get(RosterHeader.ROUND.activityRosterName);
                if (roundValue == null || !isValidRound(roundValue)) {
                    // Log a warning for invalid round number
                    String message = "Must be a number between 1 and " + MAX_ROUNDS;
                    RosterWarning warning = RosterWarning.create_badDataFormat(row,RosterHeader.ROUND.activityRosterName,message);
                    warningManager.logWarning(warning);
                    continue; // Skip this row
                }

                // Generate a key for this activity row
                String camperId = ActivityRoster.generateCamperIdFromActivity(row);

                // Get or create the merged activity data for this camper
                Map<String, String> mergedActivity = mergedActivities.computeIfAbsent(camperId, _ -> new HashMap<>());

                // Get fields to add from the row
                String cabin = row.get(RosterHeader.CABIN.activityRosterName);
                String newAssignment = row.get(RosterHeader.ACTIVITY.activityRosterName);
                String roundHeader = RosterHeader.buildRoundString(row);
                String firstName = row.get(RosterHeader.FIRST_NAME.activityRosterName);
                String prefName = row.get(RosterHeader.PREFERRED_NAME.activityRosterName);
                String lastName = row.get(RosterHeader.LAST_NAME.activityRosterName);
                String grade = row.get(RosterHeader.GRADE.activityRosterName);

                // Add camper-related fields to the merged activity data if not already present (only absent roughly 1/3 times this is used)
                // Using putIfAbsent ensures we don't overwrite existing values if this camper
                // has multiple activities
                mergedActivity.putIfAbsent(RosterHeader.CABIN.standardName, cabin);
                mergedActivity.putIfAbsent(RosterHeader.PREFERRED_NAME.camperRosterName, prefName);
                mergedActivity.putIfAbsent(RosterHeader.FIRST_NAME.camperRosterName, firstName);
                mergedActivity.putIfAbsent(RosterHeader.LAST_NAME.camperRosterName, lastName);
                mergedActivity.putIfAbsent(RosterHeader.GRADE.camperRosterName, grade);

                // Add the activity to the merged data for this round
                // putIfAbsent returns the existing value if there exists an assignment already, indicating a problematic data set
                String existingAssignment = mergedActivity.putIfAbsent(roundHeader, newAssignment);

                // If there was already an activity assigned for this round, we have a duplicate
                // Log a warning and keep the original assignment (the new one is skipped)
                if (existingAssignment != null) {
                    RosterWarning warning = RosterWarning.create_duplicateActivity(row, roundHeader, existingAssignment, newAssignment);
                    warningManager.logWarning(warning);
                }
            }
            catch (Exception e) {
                // Log the error and continue with the next row
                System.err.println("Error processing activity row: " + e.getMessage());
            }
        }
        return mergedActivities;
    }

    /**
     * Checks if a round value is valid (between 1 and MAX_ROUNDS).
     * FUTURE - delegate to RosterHeader or piggyback off RosterHeader.isRoundHeader?
     *
     * @param roundValue The round value to check
     * @return true if the round value is valid, false otherwise
     */
    private boolean isValidRound(String roundValue) {
        try {
            int round = Integer.parseInt(roundValue);
            return round >= 1 && round <= MAX_ROUNDS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Helper method applies the merged activity data to the corresponding campers in the roster.
     *
     * Takes the activity data organized by camper ID and updates the corresponding campers in the roster.
     * If a camper is not found in the roster, it logs a warning for the "orphaned" activity and
     * optionally adds it to the roster based on the INCLUDE_ORPHANS setting.
     *
     * @param roster The roster to update with activity data
     * @param mergedActivities Map of merged activity data, keyed by generated camper IDs
     * @param warningManager The warning manager to use for logging issues
     */
    private void applyActivityDataToRoster(EnhancedRoster roster, Map<String, Map<String, String>> mergedActivities, WarningManager warningManager) {
        // Second pass: update the roster with merged activity data
        for (Map.Entry<String, Map<String, String>> entry : mergedActivities.entrySet()) {
            String camperId = entry.getKey();
            Map<String, String> activityDataRow = entry.getValue();

            // Find the camper in the roster
            Camper camper = roster.getCamperById(camperId);

            // If the camper exists in the roster, apply all the activity data
            if (camper != null) {
                for (String header : activityDataRow.keySet()) {
                    String value = activityDataRow.get(header);
                    camper.setValue(header, value);
                }
            }
            // If the camper doesn't exist in the roster, this is an "orphaned" activity
            else {
                // Calculate the number of rounds assigned to this orphaned activity
                activityDataRow.put(RosterHeader.ROUND_COUNT.standardName, Integer.toString(tallyCamperRounds(activityDataRow)));

                // Log a warning about the unmatched activity
                // RosterWarning warning = RosterWarning.build_unmatchedActivity(activityDataRow);
                // warningManager.logWarning(warning);

                // If INCLUDE_ORPHANS is true, add the orphaned activity as a new camper
                // FUTURE - Give user option to include unmatched activities or not
                // FUTURE - Add a misc "notes" column system for this sort of behavior? seems helpful for exporting and checking later, but adds uneccessary complexity for now
                if (INCLUDE_ORPHANS) {
                    // Create a new camper with the orphaned activity data
                    Camper orphanedCamper = new Camper(activityDataRow);

                    // Add a note to indicate this is an orphaned ("unmatched") activity
                    // orphanedCamper.setValue("Notes", "UNMATCHED ACTIVITY: No matching camper found in roster");

                    // Add the camper to the roster
                    roster.addCamper(orphanedCamper);

                    // Log that we've added an orphaned activity
                    warningManager.logWarning(RosterWarning.build_unmatchedActivityAdded(activityDataRow));
                }
            }
        }
    }

    /**
     * Updates the assignment counts for all campers in the roster.
     * This method calculates how many rounds each camper has been assigned to
     * and stores the count in the ROUNDS_ASSIGNED_HEADER field.
     *
     * @param roster The roster containing campers to update
     */
    private void updateAssignmentCounts(EnhancedRoster roster) {
        for (Camper camper : roster.getCampers()) {
            int count = tallyCamperRounds(camper.getData());
            camper.setValue(ROUNDS_ASSIGNED_HEADER, Integer.toString(count));
        }
    }

    /**
     * Counts the number of rounds that have activities assigned for a camper.
     * This method checks each round header and increments the count for each
     * non-empty activity assignment.
     *
     * @param camperMap The map of camper data containing round assignments
     * @return The number of rounds with activities assigned
     */
    private int tallyCamperRounds(Map<String, String> camperMap) {
        int count = 0;
        // Check each round
        for (int i = 1; i <= MAX_ROUNDS; i++) {
            String roundHeader = RosterHeader.buildRoundString(i);
            if (!DataConstants.isEmpty(camperMap.get(roundHeader))) {
                count++;
            }
        }
        return count;
    }



    /**
     * Gets the activity assigned to a camper in a specific period.
     * This static utility method provides a convenient way for other features
     * to access activity assignments without having to know the header structure.
     *
     * @param roster The enhanced roster containing the camper
     * @param camperId The unique ID of the camper
     * @param period The period number (1 to MAX_ROUNDS)
     * @return The activity name, or null if no activity is assigned for that period
     * @throws UnsupportedOperationException if the ActivityFeature is not enabled in the roster
     * @throws IllegalArgumentException if the period is not valid (must be 1 to MAX_ROUNDS)
     */
    public static String getActivityForCamper(EnhancedRoster roster, String camperId, int period) {
        if (!roster.hasFeature(FEATURE_ID)) {
            throw new UnsupportedOperationException("Activity feature not enabled");
        }

        if (period < 1 || period > MAX_ROUNDS) {
            throw new IllegalArgumentException("Period must be between 1 and " + MAX_ROUNDS);
        }

        return getActivityForCamper(roster.getCamperById(camperId), period);
    }

    /**
     * Gets the activity assigned to a camper in a specific period.
     * This overloaded version works directly with a Camper object instead of looking it up
     * in the roster by ID.
     *
     * @param camper The camper object
     * @param period The period number (1 to MAX_ROUNDS)
     * @return The activity name, or null if no activity is assigned for that period
     * @throws IllegalArgumentException if the period is not valid (must be 1 to MAX_ROUNDS)
     */
    public static String getActivityForCamper(Camper camper, int period) {
        if (period < 1 || period > MAX_ROUNDS) {
            throw new IllegalArgumentException("Period must be between 1 and " + MAX_ROUNDS);
        }

        String header = RosterHeader.buildRoundString(period);
        return camper.getValue(header);
    }

    /**
     * Gets the number of activities assigned to a camper.
     * This static utility method provides a convenient way for other features
     * to access the assignment count without having to recalculate it.
     *
     * @param roster The enhanced roster containing the camper
     * @param camperId The unique ID of the camper
     * @return The number of assigned activities (0-3)
     * @throws UnsupportedOperationException if the ActivityFeature is not enabled in the roster
     */
    public static int getAssignmentCount(EnhancedRoster roster, String camperId) {
        if (!roster.hasFeature(FEATURE_ID)) {
            throw new UnsupportedOperationException("Activity feature not enabled");
        }

        String value = roster.getValue(camperId, ROUNDS_ASSIGNED_HEADER);
        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}



