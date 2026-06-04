package com.echo.logging;

import java.util.Map;
import java.util.Set;

import com.echo.domain.RosterHeader;

/**
 * Represents a non-critical issue encountered during roster processing.
 *
 * RosterWarning instances are created when the system encounters issues that don't
 * prevent processing but should be brought to the user's attention. Warnings are
 * categorized by type and include context information for display in the UI.
 *
 * Warnings are created using factory methods that ensure consistent formatting
 * and provide appropriate context information for each warning type.
 */
public class RosterWarning {

    /**
     * Enumeration of warning types with their display information.
     * Each warning type includes explanatory text and display headers for the UI.
     */
    public enum WarningType {
        OTHER("Generic Demo Warning",
            "In depth description if relevant",
            new String[]{"Single Header"}),
        UNMATCHED_ACTIVITY_SKIPPED(
            "Some activity data had no match on the camper roster",
            "This app version can't treat those rows as campers yet.",
            new String[]{"Camper","Grade","Round Assignments"}
        ),
        UNMATCHED_ACTIVITY_ADDED(
            "Some activity data had no match on the camper roster",
            "Added an unmatched data to roster as new camper(s). Some columns may be missing",
            new String[]{"Camper","Grade","Round Assignments"}
        ),
        DUPLICATE_ACTIVITY(
            "Duplicate activity/activities found (and skipped)",
            "This shouldn't be possible for a campminder generated roster - double check your data",//Message that accounts for inputting merged rosters?
            new String[]{"Camper","Round","First Assignment","Conflicting Assignment"}
        ),
        BAD_DATA_FORMAT(
            "Some data didn't match the expected format for its column",
            "The program can continue, but some data might look weird",
            new String[]{"Camper","Column","Value","Format (RegEx)"}
        ),
        PROGRAM_PARSING_FAILURE(
            "Failed to find program(s) for the selected session",
            "The program can continue, but the 'Program Filter' tool will be less helpful.\n",
            new String[]{"Camper","Value","Selected Session"}
        ),
        MISSING_FEATURE_HEADER(
            "Input files(s) lacked header(s) required by selected feature(s)",
            "The feature(s) will be skipped this time.",
            new String[]{"Required Header","Required For Feature:"}
        ),
        CAMPER_MISSING_FIELD(
            "Camper data was missing a field required by a selected feature",
            "The feature can continue, but must skip this camper.",
            new String[]{"Camper","Missing Field","Required For Feature:"}
        ),
        UNKNOWN_SWIM_ACTIVITY_FLAGGED(
            "Swim Color feature found activities not defined in the activity requirements list",//FUTURE update if using config file
            "Those activities activites are flagged as problematic, but may be fine",
            new String[]{"Activity"}
        ),
        UNKNOWN_SWIM_ACTIVITY_IGNORED(
            "Swim Level feature found activities not defined in the activity requirements list",//FUTURE update if using config file
            "If a camper's swim level contradicts their assignment to that activity, it won't be caught",
            new String[]{"Activity"}
        ),
        UNKNOWN_SWIM_LEVEL(
            "Swim Level(s) not defined in the activity requirements list",//FUTURE update if using config file
            "Check the config to ensure all valid swim levels are included",
            new String[]{"Camper","Swim Level","Accepted Levels"}
        )
        ;



        private final String generalExplanation;
        private final String secondaryExplanation;
        private final String[] displayHeaders;


        WarningType(String _generalExplanation,String _secondaryExplanation,String[] _displayHeaders){
            this.generalExplanation = _generalExplanation;
            this.secondaryExplanation = _secondaryExplanation;
            this.displayHeaders = _displayHeaders;
        }
        public String getGeneralExplanation(){
            return generalExplanation;
        }
        public String getSecondaryExplanation(){
            return secondaryExplanation;
        }
        public String[] getDisplayHeaders(){
            return displayHeaders;
        }
    }

    private final WarningType type;
    private final String[] infoCells;

    /**
     * Private constructor used by factory methods to create warnings.
     *
     * @param type The type of warning
     * @param infoCells Array of information cells for display in the UI
     */
    private RosterWarning(WarningType type, String[] infoCells) {
        this.type = type;
        this.infoCells = infoCells;
    }

    /**
     * Gets the type of this warning.
     *
     * @return The warning type
     */
    public WarningType getType() {
        return type;
    }

    /**
     * Gets the display data for this warning.
     *
     * @return Array of strings containing the warning's context information
     */
    public String[] getDisplayData() {
        return infoCells;
    }

    /**
     * Factory method builds a RosterWarning representing a case where a camper represented on one Roster is not found on another roster.
     * @param dataRow Map representing a CSV row for a camper not found on another roster
     * @return RosterWarning instance representing this issue.
     */
    /**
     * Creates a warning for an activity that couldn't be matched to any camper.
     *
     * @param dataRow Map representing a CSV row for an unmatched activity
     * @return A new RosterWarning instance
     */
    public static RosterWarning build_unmatchedActivity(Map<String,String> dataRow) {
        String camperName = buildNameString(dataRow);
        String gradeField = dataRow.get(RosterHeader.GRADE.camperRosterName);
        String roundsAssigned = dataRow.get(RosterHeader.ROUND_COUNT.standardName);

        String[] displayCells = new String[]{camperName, gradeField, roundsAssigned};
        return new RosterWarning(WarningType.UNMATCHED_ACTIVITY_SKIPPED, displayCells);
    }

    /**
     * Creates a warning for an orphaned activity that has been added to the roster.
     *
     * @param dataRow Map representing a CSV row for the orphaned activity
     * @return A new RosterWarning instance
     */
    public static RosterWarning build_unmatchedActivityAdded(Map<String,String> dataRow) {
        
        String camperName = buildNameString(dataRow);
        String gradeField = dataRow.getOrDefault(RosterHeader.GRADE.camperRosterName, "Unknown");
        String roundsAssigned = dataRow.getOrDefault(RosterHeader.ROUND_COUNT.standardName, "Unknown");

        String[] displayCells = new String[]{camperName, gradeField, roundsAssigned};
        return new RosterWarning(WarningType.UNMATCHED_ACTIVITY_ADDED, displayCells);
    }

    
    /**
     * Factory method builds a RosterWarning representing a case where two activities are found for the same camper during the same round.
     *
     * @param mergeRow Map representing the CSV row with the duplicate assignment
     * @param round The round/period with the duplicate assignment
     * @param oldAssignment The original activity assignment
     * @param newAssignment The conflicting activity assignment
     * @return A new RosterWarning instance
     */
    public static RosterWarning create_duplicateActivity(Map<String,String> mergeRow, String round, String oldAssignment, String newAssignment) {
        String camperName = buildNameString(mergeRow);
        String[] displayCells = new String[]{camperName,round,oldAssignment,newAssignment};
        return new RosterWarning(WarningType.DUPLICATE_ACTIVITY,displayCells);
    }

    /**
     * Factory method builds a RosterWarning representing a case where parsed CSV data didn't match the provided regex for the column's data format.
     *
     * @param dataRow Map representing the CSV row with the format issue
     * @param column The column/header with the format issue
     * @param format The expected format (regex pattern)
     * @return A new RosterWarning instance
     */
    public static RosterWarning create_badDataFormat(Map<String,String> dataRow, String column, String format) {
        String camperName = buildNameString(dataRow);
        String field = dataRow.getOrDefault(column, "No Data");
        String[] displayCells = new String[]{camperName,column,field,format};
        return new RosterWarning(WarningType.BAD_DATA_FORMAT,displayCells);
    }

    /**
     * Factory method builds a RosterWarning representing a case where the converter failed to parse a camper's current program from an "Enrolled Sessions/Programs" field.
     *
     * @param dataRow Map representing the CSV row with the parsing issue
     * @param currentSession The current session being processed
     * @return A new RosterWarning instance
     */
    public static RosterWarning create_programParsingFailure(Map<String,String> dataRow, String currentSession) {
        String camperName = buildNameString(dataRow);
        String espField = dataRow.getOrDefault(RosterHeader.ESP.camperRosterName, "No Data");

        String[] displayCells = new String[]{camperName,espField,currentSession};
        return new RosterWarning(WarningType.PROGRAM_PARSING_FAILURE,displayCells);
    }


    /**
     * Creates a warning for a missing header required by a feature.
     *
     * @param header The name of the missing header
     * @param feature The name of the feature requiring the header
     * @return A new RosterWarning instance
     */
    public static RosterWarning create_missingFeatureHeader(String header, String feature) {
        String[] displayCells = new String[]{header,feature};
        return new RosterWarning(WarningType.MISSING_FEATURE_HEADER,displayCells);
    }

    /**
     * Creates a warning for a camper missing a field required by a feature.
     *
     * @param dataRow Map representing the CSV row for the camper
     * @param missingField The name of the missing field
     * @param feature The name of the feature requiring the field
     * @return A new RosterWarning instance
     */
    public static RosterWarning create_camperMissingField(Map<String,String> dataRow, String missingField, String feature) {
        String camperName = buildNameString(dataRow);
        String[] displayCells = new String[]{camperName,missingField,feature};
        return new RosterWarning(WarningType.CAMPER_MISSING_FIELD,displayCells);
    }



    
    /**
     * Creates a warning for an activity not defined in the Swim Color feature's activity requirements file.
     * Uses different warning types based on whether unknown activities are being flagged or not. 
     * This difference has no impact on warning behavior, but provides a different user explanation
     *
     * @param activityName The name of the unknown activity
     * @param unknownActivitiesFlagged True if activites are flagged as problematic, false if they are ignored
     * @return A new RosterWarning instance
     */
    public static RosterWarning create_unknownSwimActivityWarning(String activityName, boolean unknownActivitiesFlagged) {
        String[] displayCells = new String[]{activityName};
        return unknownActivitiesFlagged ? new RosterWarning(WarningType.UNKNOWN_SWIM_ACTIVITY_FLAGGED,displayCells)
                                        : new RosterWarning(WarningType.UNKNOWN_SWIM_ACTIVITY_IGNORED,displayCells);
    }

    /**
     * Creates a warning for the case where a swim evel is listed in the given data but not th the level mappings
     * 
     * @param camperData map of camper data, used to generate name string for display
     * @param swimLevel String of swim level in question
     * @param acceptedLevels Set of strings that are valid swim levels
     * @return A new RosterWarning instance
     */
    public static RosterWarning create_unknownSwimLevelWarning(Map<String,String> camperData,String swimLevel,Set<String> acceptedLevels){
        String camperName = buildNameString(camperData);
        String[] displayCells = new String[]{camperName,swimLevel,String.join(", ",acceptedLevels)};
        return new RosterWarning(WarningType.UNKNOWN_SWIM_LEVEL,displayCells);
    }



    /**
     * Helper method that builds the best possible String to representing a camper's name, based on available fields.
     * @param dataRow Map representing a CSV row containing data about a camper.
     * @return String containing first and last name, plus their preferred name if different from first name.
     */
    private static String buildNameString(Map<String,String> dataRow){
        // Default fallback value for missing name fields
        final String UNKNOWN = "Unknown";

        // Try to get name fields with fallbacks
        String firstName = dataRow.getOrDefault(RosterHeader.FIRST_NAME.camperRosterName,
                          dataRow.getOrDefault(RosterHeader.FIRST_NAME.activityRosterName, UNKNOWN));
        String prefName = dataRow.getOrDefault(RosterHeader.PREFERRED_NAME.camperRosterName,
                         dataRow.getOrDefault(RosterHeader.PREFERRED_NAME.activityRosterName, firstName));
        String lastName = dataRow.getOrDefault(RosterHeader.LAST_NAME.camperRosterName,
                         dataRow.getOrDefault(RosterHeader.LAST_NAME.activityRosterName, UNKNOWN));

        // If we couldn't find any name fields, try to use another field as identifier
        if (firstName.equals(UNKNOWN) && lastName.equals(UNKNOWN)) {
            return "Data row " + dataRow.hashCode();
        }

        // Normal case - format the name based on whether first name equals preferred name
        return firstName.equals(prefName) ?
               String.format("%s %s", prefName, lastName) :
               String.format("%s '%s' %s", firstName, prefName, lastName);
    }


}


