package com.echo.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.logging.RosterWarning;
import com.echo.logging.WarningManager;

/**
 * Feature that extracts and processes program information from enrollment data.
 * - Determines the current session based on all campers' Enrolled Sessions/Programs (ESP) fields.
 * - Analyzes each ESP field to determine which program each camper is enrolled in for the current session.
 * - Adds a dedicated Program column to the roster for easier access and filtering.
 *
 * Multiple enrollments are separated by " and " in the ESP field.
 */
public class ProgramFeature implements RosterFeature {
    /** Unique identifier for this feature */
    private static final String FEATURE_ID = "program";

    /** Display name for this feature */
    public static final String FEATURE_NAME = "Program Information";

    /**
     * Headers required by this feature.
     * The ProgramFeature requires the ESP field to extract program information.
     */
    private static final List<String> REQUIRED_HEADERS = Arrays.asList(
        RosterHeader.ESP.camperRosterName  // Enrolled Sessions/Programs field
    );

    /**
     * Headers added by this feature to the enhanced roster.
     * The ProgramFeature adds a dedicated Program column for easier access.
     */
    private static final List<String> ADDED_HEADERS = Arrays.asList(
        RosterHeader.PROGRAM.standardName  // Program field
    );

    /**
     * Required formats for data validation.
     * The ProgramFeature uses regex patterns from RosterRegexBuilder to validate
     * the format of ESP fields and program names.
     */
    private static final Map<String, String> REQUIRED_FORMATS;

    static {
        // Convert Pattern objects to String representations for the interface
        Map<String, Pattern> patternMap = com.echo.validation.RosterRegexBuilder.buildProgramFormats();
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
     * Validates that the roster has all prerequisites for this feature.
     * This method checks that the required ESP header is present in the roster.
     * The actual header presence check is handled by the RosterService, so this
     * method currently always returns true.
     *
     * @param roster The roster to validate
     * @param warningManager The warning manager to use for logging issues
     * @return Always returns true as there are no additional prerequisites to check
     */
    @Override
    public boolean preValidate(EnhancedRoster roster, WarningManager warningManager) {
        // Basic validation is just checking that the required header exists
        // This is already handled by the RosterService when it checks required headers

        // Future enhancement: Check that each camper has a valid ESP value
        return true;
    }

    /**
     * Validates that the feature was applied correctly.
     * Currently, this method always returns true as there are no specific
     * post-validation checks implemented for the ProgramFeature.
     *
     * @param roster The roster to validate
     * @param warningManager The warning manager to use for logging issues
     * @return Always returns true as there are no post-validation checks implemented
     */
    @Override
    public boolean postValidate(EnhancedRoster roster, WarningManager warningManager) {
        // No specific post-validation needed for this feature
        return true;
    }

    /**
     * Applies this feature to the roster by extracting program information.
     * This method performs the following steps:
     * - Adds the Program header to the roster
     * - Determines the current session based on all campers' ESP fields
     * - Extracts the program for each camper based on the current session
     * - Logs warnings for any campers whose program couldn't be extracted
     * - Enables the feature in the roster
     *
     * @param roster The roster to enhance with program information
     * @param warningManager The warning manager to use for logging issues
     */
    @Override
    public void applyFeature(EnhancedRoster roster, WarningManager warningManager) {
        // Add the new header
        for (String header : getAddedHeaders()) {
            roster.addHeader(header);
        }

        // Determine the current session based on all campers
        Integer currentSession = determineCurrentSession(roster.getCampers());

        // Process each camper to extract program information
        for (Camper camper : roster.getCampers()) {
            String espValue = camper.getValue(RosterHeader.ESP.camperRosterName);
            String programValue = extractProgramFromESP(espValue, currentSession, warningManager);
            // If program extraction failed, use the original ESP value and log a warning
            // System.out.println(espValue+"->"+programValue);
            if (programValue == null) {
                programValue = espValue;
                warningManager.logWarning(RosterWarning.create_programParsingFailure(
                    camper.getData(),
                    currentSession != null ? currentSession.toString() : "unknown"
                ));
            }
            camper.setValue(RosterHeader.PROGRAM.standardName, programValue);
        }

        // Enable this feature
        roster.enableFeature(FEATURE_ID);
    }

    /**
     * This method parses the ESP field to find the program associated with the current session.
     *
     * The ESP field typically follows the format: "Session X[A/B]/ProgramName" where X is the
     * session number, [A/B] is an optional session variant, and ProgramName is the program.
     *
     * Multiple enrollments are separated by " and " in the ESP field.
     *
     * @param espValue The value of the ESP field to parse
     * @param currentSession The current session number determined from all campers
     * @param warningManager The warning manager to use for logging issues
     * @return The extracted program value, or null if extraction failed
     */
    private String extractProgramFromESP(String espValue, Integer currentSession, WarningManager warningManager) {
        if (espValue == null || espValue.isEmpty()) {
            return "";
        }

        // If we couldn't determine a current session, just take the first program
        // This is a fallback strategy when there's no clear current session
        if (currentSession == null) {
            String[] programs = espValue.split(" and ");
            if (programs.length > 0) {
                String[] parts = programs[0].split("/");
                return parts.length > 1 ? parts[1].trim() : "";
            }
            return "";
        }

        // Look for the program associated with the current session
        // We search through all session/program pairs to find the one matching the current session
        String sessionPrefix = "Session " + currentSession;
        String[] programsAndSessions = espValue.split(" and ");

        for (String programAndSession : programsAndSessions) {
            // Split by "/" to separate session from program
            String[] parts = programAndSession.split("/");
            if (parts.length < 2) {
                // Invalid format, skip this entry
                continue;
            }

            String sessionPart = parts[0].trim();
            String programPart = parts[1].trim();

            // Check if this is the current session (ignoring any letter suffix like A or B)
            if (sessionPart.startsWith(sessionPrefix)) {
                return programPart;
            }
        }

        // If no matching session found, return empty string
        return null;
    }

    /**
     * Gets the program for a specific camper.
     *
     * @param roster The enhanced roster containing the camper
     * @param camperId The unique ID of the camper
     * @return The program value, or empty string if not available
     * @throws UnsupportedOperationException if the ProgramFeature is not enabled in the roster
     */
    public static String getProgramForCamper(EnhancedRoster roster, String camperId) {
        if (!roster.hasFeature(FEATURE_ID)) {
            throw new UnsupportedOperationException("Program feature not enabled");
        }

        String value = roster.getValue(camperId, RosterHeader.PROGRAM.standardName);
        return value != null ? value : "";
    }

    /**
     * Determines the current session based on the ESP fields of all campers.
     *
     * This method analyzes all campers' ESP fields to find the most common session number,
     * which is assumed to be the "current" session.
     *
     * @param campers List of campers to analyze
     * @return The most common session number, or null if no valid session found
     */
    private static Integer determineCurrentSession(List<Camper> campers) {
        if (campers == null || campers.isEmpty()) {
            return null;
        }

        // Map to count occurrences of each session number
        Map<Integer, Integer> sessionCounts = new HashMap<>();

        // Regular expression to extract session numbers
        // Matches "Session" followed by a number, optionally followed by a letter (A or B)
        // The capturing group (\d+) extracts just the numeric part of the session
        Pattern sessionPattern = Pattern.compile("Session\\s+(\\d+)[A-Za-z]?");

        for (Camper camper : campers) {
            String espValue = camper.getValue(RosterHeader.ESP.camperRosterName);
            if (espValue == null || espValue.isEmpty()) {
                continue;
            }

            // Split by "and" to get individual session/program pairs
            String[] programsAndSessions = espValue.split(" and ");

            for (String programAndSession : programsAndSessions) {
                // Split by "/" to separate session from program
                String[] parts = programAndSession.split("/");
                if (parts.length < 2) {
                    // Invalid format, skip this entry
                    continue;
                }

                String sessionPart = parts[0].trim();

                // Extract the session number using regex
                Matcher matcher = sessionPattern.matcher(sessionPart);
                if (matcher.find()) {
                    try {
                        // Get the captured group (the session number) and increment its count in the map
                        // The group(1) refers to the first capturing group in the regex pattern (\d+)
                        int sessionNumber = Integer.parseInt(matcher.group(1));
                        sessionCounts.put(sessionNumber, sessionCounts.getOrDefault(sessionNumber, 0) + 1);
                    } catch (NumberFormatException e) {
                        // Skip if the number can't be parsed
                    }
                }
            }
        }

        // Find the session with the highest count
        // This returns the session number that appears most frequently in the ESP fields
        // If there's a tie, it returns the first one encountered (which is fine for our purposes)
        // If no valid sessions were found, it returns null
        return sessionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())  // Find the entry with the highest count
                .map(Map.Entry::getKey)             // Extract just the session number
                .orElse(null);                      // Return null if no sessions were found
    }



    /**
     * Gets all programs in the roster organized by round count.
     */
    public static Map<Integer, List<String>> getProgramsByRoundCount(EnhancedRoster roster) {
        Map<Integer, List<String>> programsByRoundCount = new LinkedHashMap<>();
        Map<String, Integer> programRoundCounts = new HashMap<>();

        // Initialize the map with empty lists for each possible round count
        programsByRoundCount.put(-1, new ArrayList<>()); // Mixed
        programsByRoundCount.put(3, new ArrayList<>());
        programsByRoundCount.put(2, new ArrayList<>());
        programsByRoundCount.put(1, new ArrayList<>());
        programsByRoundCount.put(0, new ArrayList<>());

        // Count rounds for each program
        for (Camper camper : roster.getCampers()) {
            String program = camper.getValue(RosterHeader.PROGRAM.standardName);
            if (program == null || program.isEmpty()) {
                continue;
            }

            // Get the rounds assigned value
            String roundsValue = camper.getValue(RosterHeader.ROUND_COUNT.standardName);
            if (roundsValue != null) {
                try {
                    int roundCount = Integer.parseInt(roundsValue);

                    // Update the round count for this program
                    if (!programRoundCounts.containsKey(program)) {
                        programRoundCounts.put(program, roundCount);
                    } else {
                        int currentCount = programRoundCounts.get(program);
                        if (currentCount != roundCount) {
                            // If we find inconsistent counts, mark as mixed (-1)
                            programRoundCounts.put(program, -1);
                        }
                    }
                } catch (NumberFormatException e) {
                    // If parsing fails, default to 0
                    if (!programRoundCounts.containsKey(program)) {
                        programRoundCounts.put(program, 0);
                    }
                }
            } else {
                // If no rounds value, default to 0
                if (!programRoundCounts.containsKey(program)) {
                    programRoundCounts.put(program, 0);
                }
            }
        }

        // Organize programs by round count
        for (Map.Entry<String, Integer> entry : programRoundCounts.entrySet()) {
            String program = entry.getKey();
            int roundCount = entry.getValue();
            programsByRoundCount.get(roundCount).add(program);
        }

        // Sort programs alphabetically within each round count
        for (List<String> programs : programsByRoundCount.values()) {
            programs.sort(String::compareTo);
        }

        return programsByRoundCount;
    }



}
