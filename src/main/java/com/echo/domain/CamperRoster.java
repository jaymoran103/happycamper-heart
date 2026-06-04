package com.echo.domain;

import java.util.Arrays;
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
 * Specialized roster for campers enrolled in a session
 *
 * Extends the base roster to provide functionality specific to campminder-generated Camper User Reports
 *
 * Works with Camper Roster to provide a complete picture of camper activity assignments.
 * Data format provides the base for an EnhancedRoster

 */
public class CamperRoster extends Roster {
    /**
     * List of headers that must be present in a valid camper roster.
     * These headers are checked during validation to ensure the roster has all required data.
     */
    private static final List<String> REQUIRED_HEADERS = Arrays.asList(
        RosterHeader.FIRST_NAME.camperRosterName,
        RosterHeader.LAST_NAME.camperRosterName,
        RosterHeader.GRADE.camperRosterName,
        RosterHeader.ESP.camperRosterName,
        RosterHeader.PREFERRED_NAME.camperRosterName
    );

    /**
     * Patterns used to validate the format of specific fields in the camper roster.
     * Each entry maps a field name to a regular expression pattern that valid values must match.
     */
    private static final Map<String, Pattern> VALIDATION_PATTERNS = RosterRegexBuilder.buildCamperFormats();

    /**
     * Creates a new empty CamperRoster with no headers or campers.
     * Headers and campers must be added separately after creation.
     */
    public CamperRoster() {
        super();
    }

    /**
     * Validates that the roster has all required headers and that all camper data is valid.
     * This method checks both the roster structure (headers) and the content of each camper entry.
     * Issues are logged to the warning manager rather than throwing exceptions for non-critical problems.
     *
     * @param warningManager The warning manager to use for logging validation issues
     * @throws RosterException if critical validation fails (missing required headers)
     */
    public void validate(WarningManager warningManager) throws RosterException {
        // Check for required headers - should have been checked in import step but cost is minimal

        validateHeaders(getHeaderMap().keySet(), REQUIRED_HEADERS);

        // Validate data in each row
        for (Camper camper : getCampers()) {
            validateCamper(camper,warningManager);
        }
    }

    /**
     * Validates a single camper's data for required fields and correct data formats.
     * This method checks that the camper has all required fields and that the values
     * match the expected formats. Issues are logged to the warning manager.
     *
     * @param camper The camper entry to validate
     * @param warningManager The warning manager to log validation issues
     * @throws RosterException if critical validation fails (missing required fields)
     */
    private void validateCamper(Camper camper,WarningManager warningManager) throws RosterException {
        // Check for required fields
        validateHeaders(camper.getData().keySet(), REQUIRED_HEADERS);
        
        // Validate field formats
        for (Map.Entry<String, Pattern> entry : VALIDATION_PATTERNS.entrySet()) {
            String field = entry.getKey();
            Pattern pattern = entry.getValue();

            String value = camper.getValue(field);
            if (value != null && !pattern.matcher(value).matches()) {
                RosterWarning warning = RosterWarning.create_badDataFormat(camper.getData(), field, pattern.toString());
                warningManager.logWarning(warning);
            }
        }
    }



    /**
     * Gets the list of headers that must be present in a valid camper roster.
     * This method is used by external components to check if a file has the necessary
     * headers before attempting to load it as a camper roster.
     *
     * @return An unmodifiable list of required header names
     */
    public static List<String> getRequiredHeaders() {
        return REQUIRED_HEADERS;
    }




    /**
     * Normalizes program names by removing gender tags and standardizing leadership names
     */
    public void normalizePrograms() {
        ProgramNameAdjuster programAdjuster = new ProgramNameAdjuster(ProgramNameAdjuster.Mode.STANDARDIZE);
        for (Camper camper : getCampers()) {
            String value = camper.getValue(RosterHeader.ESP.camperRosterName);
            String newValue = programAdjuster.adjustIfPossible(value);
            if (!value.equals(newValue)){
                camper.setValue(RosterHeader.ESP.camperRosterName,newValue);
            }
        }
    }
}
