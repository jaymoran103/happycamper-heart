package com.echo.logging;


/**
 * DetailedRosterException extends RosterException to provide a table based display for RosterExceptions
 *
 * The class is used when an error occurs that benefits from showing multiple related items
 * or structured data, such as missing headers, duplicate entries, or validation failures
 * across multiple records.
 */
public class DetailedRosterException extends RosterException {

    /** Column headers for the table display */
    private final String[] tableHeaders;

    /** Two-dimensional array containing the table data (rows and columns) */
    private final String[][] tableData;

    /**
     * Constructor for creating a detailed exception with tabular data.
     * This constructor is used when the exception is created directly, not as a result
     * of catching another exception.
     *
     * @param type The error type categorizing this exception
     * @param summary A concise summary of the exception's cause
     * @param explanation A detailed explanation or context for this exception
     * @param tableHeaders Array of column headers for the table display
     * @param tableData Two-dimensional array of table data (rows and columns)
     */
    protected DetailedRosterException(ErrorType type, String summary, String explanation,String[] tableHeaders, String[][] tableData) {
        super(type, summary, explanation);
        this.tableHeaders = tableHeaders;
        this.tableData = tableData;
    }

    /**
     * Constructor for creating a detailed exception with tabular data from another exception.
     * This constructor is used when wrapping another exception while adding tabular context data.
     *
     * @param type The error type categorizing this exception
     * @param e The original exception being wrapped
     * @param message The error message
     * @param summary A concise summary of the exception's cause
     * @param headerArray Array of column headers for the table display
     * @param data Two-dimensional array of table data (rows and columns)
     */
    protected DetailedRosterException(ErrorType type, Exception e, String message, String summary, String[] headerArray, String[][] data) {
        super(type, e, message, summary);
        this.tableHeaders = headerArray;
        this.tableData = data;
    }

    /**
     * Gets the table headers for display in the UI.
     * These headers define the columns of the table that will be shown to the user.
     *
     * @return Array of table column headers
     */
    public String[] getTableHeaders() {
        return tableHeaders;
    }

    /**
     * Gets the table data for display in the UI.
     * This data represents the rows and columns of information that will be shown
     * to the user to help understand the error context.
     *
     * @return Two-dimensional array of table data (rows and columns)
     */
    public String[][] getTableData() {
        return tableData;
    }

    /**
     * Checks if this exception has valid table data to display.
     * This method verifies that both headers and data are present and non-empty.
     * It's used by the UI to determine whether to show the table view.
     *
     * @return true if this exception has valid table data to display, false otherwise
     */
    public boolean hasTableData() {
        return tableHeaders != null && tableHeaders.length > 0 &&
               tableData != null && tableData.length > 0;
    }
}