package com.echo.logging;

import java.io.File;
import java.util.List;

import com.echo.HappyCamper;

/**
 * Exception class for roster-related errors with user-friendly information.
 *
 * RosterException extends the standard Exception class to include additional information
 * that can be displayed to the user in a meaningful way. Each instance stores a summary
 * and detailed explanation that can be used in the UI to show error labels and popups.
 *
 * The class uses factory methods to create specific types of exceptions with consistent
 * formatting and appropriate context information.
 */
public class RosterException extends Exception {

    /**
     * Enumeration of error types for categorization and display purposes.
     * Each error type represents a different class of issues that can occur during roster processing.
     */
    public enum ErrorType {
        MALFORMED,
        MISSING_DATA,
        HEADER,
        FILE,
        INTERNAL,
        WRAPPER;
    }

    private final String summary;
    private final String explanation;
    private final ErrorType errorType;
    private final boolean isWrapper;

    /**
     * Primary constructor calls super and stores information to represent this exception in the GUI
     *
     * @param type ErrorType categorizing this exception
     * @param summary String providing a summary of the exception's cause, used as text for an error label
     * @param explanation String providing explanation or context for this exception, used as text for a popup
     */
    protected RosterException(ErrorType type, String summary, String explanation) {
        super(summary);
        this.errorType = type;
        this.summary = summary;
        this.explanation = explanation;
        this.isWrapper = false;
    }

    /**
     * Alternate constructor wraps a standard exception as a RosterException, enabling the GUI to report it like any other issue.
     *
     * @param type ErrorType categorizing this exception
     * @param e standard exception to wrap
     * @param summary String providing a summary of the exception's cause, used as text for an error label
     * @param explanation String providing explanation or context for this exception, used as text for a popup
     */
    protected RosterException(ErrorType type, Exception e, String summary, String explanation) {
        super(summary, e);
        this.errorType = type;
        this.summary = summary;
        this.explanation = explanation;
        this.isWrapper = true;
    }

    /**
     * Factory method builds an exception to report a file-related issue
     *
     * @param summary The summary of the issue
     * @param explanation The detailed explanation of the issue
     * @return RosterException instance representing this issue
     */
    public static RosterException fileException(String summary, String explanation) {
        return new RosterException(ErrorType.FILE, summary, explanation);
    }

    /**
     * Factory method builds an exception to report a malformed CSV row
     * Replaced by create_malformedRowException, which returns a DetailedRosterException for a more readable result
     *
     * @param fileName String name of the file containing a malformed row
     * @param headerCount int representing size of the header row
     * @param cellCount int representing the size of the data row
     * @param rowNumber row's number in the file
     * @return RosterException instance representing this issue
     */
    @Deprecated
    public static RosterException malformedRow_stringReport(String fileName, int headerCount, int cellCount, int rowNumber) {
        String comparisonWord = (cellCount > headerCount) ? "more" : "fewer";
        String errorSummary = "Malformed data detected";
        String errorExplanation = "A row in file '" + fileName + "' has " + comparisonWord + " columns than the header row.\n\n" +
                            "Row: " + rowNumber + "\n" +
                            "Items in row:  " + cellCount + "\n" +
                            "Total headers: " + headerCount + "\n\n" +
                            "Double check file contents before selecting.";
        return new RosterException(ErrorType.MALFORMED, errorSummary, errorExplanation);
    }


    /**
     * Factory method builds an exception to report a malformed CSV row
     * Returns a DetailedRosterException so information can be reported in a table GUI
     * FUTURE - look into making the dialog's table nicer by shrinking columns to match given data, or mandate widths in an integer[][] argument.
     *
     * @param fileName String name of the file containing a malformed row
     * @param headerCount int representing size of the header row
     * @param cellCount int representing the size of the data row
     * @param rowNumber row's number in the file
     * @return RosterException instance representing this issue
     */
    public static RosterException create_malformedRowException(String fileName, int headerCount, int cellCount, int rowNumber){
        String comparisonWord = (cellCount > headerCount) ? "more" : "fewer";
        String errorSummary = "Malformed data detected - double check file contents before selecting";
        String errorExplanation = "A row in file '" + fileName + "' has " + comparisonWord + " columns than the header row.\n";

        String[] headers = new String[]{" "," "};
        String[][] data = new String[3][2];
        data[0] = new String[]{"Malformed Row Index",Integer.toString(rowNumber)};
        data[1] = new String[]{"Items in row",Integer.toString(cellCount)};
        data[2] = new String[]{"Number of headers (rows should match this)",Integer.toString(headerCount)};

        return new DetailedRosterException(ErrorType.MALFORMED, errorSummary, errorExplanation,headers,data);

    }

    /**
     * Factory method builds an exception to report a file that lacks headers or rows
     *
     * @param fileName String name of the problem file. Accepts null gracefully
     * @param hasHeaders boolean indicating whether the file has headers
     * @return RosterException instance representing this issue
     */
    public static RosterException noData(String fileName, Boolean hasHeaders) {
        String fileReference = fileName != null ? "CSV file '" + fileName + "'" : "Provided file";
        String missingContent = hasHeaders ? " contains headers but no rows." : " contains no data rows.";

        String errorSummary = "Missing data detected";
        String errorExplanation = fileReference + missingContent
                                + "\nDouble check file contents before selecting.";

        return new RosterException(ErrorType.MISSING_DATA, errorSummary, errorExplanation);
    }

    /**
     * Factory method builds an exception to report a file that lacks required headers.
     *
     * @param headers List of outstanding headers to display
     * @return RosterException instance representing this issue
     */
    public static RosterException missingHeaders_Basic(List<String> headers) {
        String errorSummary = "File lacks required headers";
        StringBuilder errorExplanation = new StringBuilder("File is missing the following headers:\n");

        String lineMarker = " - ";
        for (String s : headers) {
            errorExplanation.append(lineMarker).append(s).append("\n");
        }

        errorExplanation.append("\nFor more information, click the info button above the file selector.");

        return new RosterException(ErrorType.HEADER, errorSummary, errorExplanation.toString());
    }


    /**
     * Factory method builds an exception to report a file that lacks required headers.
     *
     * @param f File object for the file in question
     * @param headers List of outstanding headers to display
     * @return DetailedRosterException instance representing this issue
     */
    public static DetailedRosterException missingHeaders(File f,List<String> headers) {
        String errorSummary = "File "+f.getName()+" lacks required headers";
        String errorExplanation = "Check the user guide for more context";
        String[] headerArray = new String[]{"Missing Header", "Required For"};
        String[][] data = new String[headers.size()][2];

        for (int i = 0; i < headers.size(); i++) {
            data[i][0] = headers.get(i);
            data[i][1] = "Basic Setup";
        }
        return new DetailedRosterException(ErrorType.HEADER, errorSummary, errorExplanation, headerArray, data);
    }



    /**
     * Factory method builds an exception that wraps a standard exception with detailed stack trace information.
     *
     * @param summary Brief summary of the issue
     * @param e The exception to wrap
     * @return DetailedRosterException instance with stack trace information
     */
    public static RosterException create_normalWrapper(String summary, Exception e) {

        //Prints stack trace if boolean is true in HappyCamper. 
        //Allows developer to toggle printing for all unanticipated exception, while still handling elegantly by wrapping as RosterExceptions
        conditionalPrint(e);

        String basicExplanation = summary;
        String detailedExplanation = "If this keeps happening, try using a different file, or let Jay know.";
        StackTraceElement[] stackTrace = e.getStackTrace();
        String[] headerArray = new String[]{"Stack Trace"};

        String[][] data = new String[stackTrace.length][1];
        for (int i = 0; i < stackTrace.length; i++) {
            data[i][0] = stackTrace[i].toString();
        }

        return new DetailedRosterException(ErrorType.WRAPPER,e,basicExplanation,detailedExplanation,headerArray,data);
    }


    /**
     * Factory method builds an exception for header-related issues.
     *
     * @param e The exception that caused the header issue
     * @return RosterException instance representing this issue
     */
    public static RosterException createHeaderIssue(Exception e) {
        return new RosterException(ErrorType.HEADER, e, "Header Error", e.getMessage());
    }

    /**
     * Factory method builds an exception to report an internal error
     *
     * @param explanation explanation of issue
     * @return RosterException instance representing this issue
     */
    /**
     * Factory method builds an exception to report an internal error.
     *
     * @param summary Brief summary of the internal error
     * @param explanation Detailed explanation of the issue
     * @return RosterException instance representing this issue
     */
    public static RosterException internal(String summary, String explanation) {
        return new RosterException(ErrorType.INTERNAL, "Internal Error: " + summary, explanation);
    }

    /**
     * Gets the type of this error.
     *
     * @return The error type
     */
    public ErrorType getType() {
        return errorType;
    }

    /**
     * Gets the summary of the error.
     *
     * @return The summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Gets the explanation of the error.
     *
     * @return The explanation
     */
    public String getExplanation() {
        return explanation;
    }

    /**
     * Checks whether this exception is a wrapper for a standard exception
     * @return boolean indicating whether this exception is a wrapper
     */
    public boolean isWrapper(){
        return isWrapper;
    }


    /**
     * Prints stack trace if boolean is true in HappyCamper. 
     * Allows developer to toggle printing for all unanticipated exception, 
     * while still handling elegantly by wrapping as RosterExceptions
     * @param e
     */
    public static void conditionalPrint(Exception e){
        if (HappyCamper.PRINT_LOGGED_ERRORS){
            e.printStackTrace();
        }
    }
}