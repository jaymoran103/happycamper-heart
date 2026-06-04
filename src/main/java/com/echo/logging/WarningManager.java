package com.echo.logging;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import com.echo.logging.RosterException.ErrorType;
import com.echo.logging.RosterWarning.WarningType;

/**
 * Central manager for collecting and organizing warnings and errors during roster processing.
 *
 * This centralized log makes it easy to check if the process is safe to continue, 
 * and giving the user context about non-fatal issues that might impact the results they see
 *
 * Components throughout the application (mainly RosterFeatures) inject and use this manager to log issues
 * without needing to immediately handle any errors, UI display, or even know how their issues will be displayed
 */
public class WarningManager {
    /** Collection of warnings organized by warning type */
    private final Map<WarningType, ArrayList<RosterWarning>> warningLog = new EnumMap<>(WarningType.class);

    /** Collection of errors organized by error type */
    private final Map<ErrorType, ArrayList<RosterException>> errorLog = new EnumMap<>(ErrorType.class);

    /**
     * Logs a warning message in the appropriate collection based on its type.
     * Warnings are logged in separate ArrayLists based on their type, and keyed as such in a Map.
     *
     * @param warning The RosterWarning instance to log
     */
    public void logWarning(RosterWarning warning) {
        warningLog.computeIfAbsent(warning.getType(), _ -> new ArrayList<>()).add(warning);
    }

    /**
     * Logs a given RosterException in the appropriate collection based on its type.
     * This method adds the exception to a list of errors of the same type, creating
     * the list if it doesn't already exist. Unlike warnings, errors typically indicate
     * critical issues that may prevent successful completion of the process.
     *
     * @param exception The RosterException instance to log
     */
    public void logError(RosterException exception) {
        // Store by type for the ErrorDialog
        errorLog.computeIfAbsent(exception.getType(), err -> new ArrayList<>()).add(exception);
    }

    /**
     * Checks if any warnings have been logged during processing.
     * Used to determine if there are non-critical issues that should be presented to the user.
     *
     * @return boolean indicating the presence of warning(s) in the warning log
     */
    public boolean hasWarnings() {
        return !warningLog.isEmpty();
    }

    /**
     * Used to determine if critical issues occurred that may prevent successful completion of the process.
     *
     * @return boolean indicating the presence of error(s) in the error log
     */
    public boolean hasErrors() {
        return !errorLog.isEmpty();
    }

    /**
     * Getter provides access to the full Map of warnings.
     *
     * @return Map of RosterWarning lists, organized by their WarningTypes
     */
    public Map<WarningType, ArrayList<RosterWarning>> getWarningLog() {
        return warningLog;
    }

    /**
     * Getter provides access to the full Map of errors, organized by type.
     *
     * @return Map of RosterException lists, organized by their ErrorTypes
     */
    public Map<ErrorType, ArrayList<RosterException>> getErrorLog() {
        return errorLog;
    }
}