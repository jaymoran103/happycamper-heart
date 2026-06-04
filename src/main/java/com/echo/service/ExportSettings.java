package com.echo.service;

import java.io.File;

/**
 * Settings for the export process.
 * Equivalent to the ExportSettings enum from v1
 *
 * This class encapsulates all settings related to exporting data,
 * making it easier to pass around and modify export options.
 */
public class ExportSettings {
    

    private boolean showAllColumns = true;
    private boolean showAllRows = true;
    private boolean useEmptyPlaceholder = true;
    private File destinationFile;
    
    /**
     * Creates a new ExportSettings instance with default values.
     */
    public ExportSettings() {
    }
    
    /**
     * Creates a new ExportSettings instance with the specified destination file.
     * 
     * @param destinationFile The file to export to
     */
    public ExportSettings(File destinationFile) {
        this.destinationFile = destinationFile;
    }
    
    /**
     * Gets whether to use a placeholder for empty values.
     * 
     * @return true if empty values should be replaced with a placeholder, false to leave them empty
     */
    public boolean getUseEmptyPlaceholder() {
        return useEmptyPlaceholder;
    }

    /**
     * Gets whether to show all columns.
     * 
     * @return true if all columns should be shown, false to show only visible columns
     */
    public boolean getShowAllColumns() {
        return showAllColumns;
    }

    /**
     * Gets whether to show all rows.
     * 
     * @return true if all rows should be shown, false to show only visible rows
     */
    public boolean getShowAllRows() {
        return showAllRows;
    }
    
    /**
     * Sets whether to use a placeholder for empty values.
     * 
     * @param useEmptyPlaceholder true to replace empty values with a placeholder, false to leave them empty
     * @return This ExportSettings instance for method chaining
     */
    public ExportSettings setUseEmptyPlaceholder(boolean useEmptyPlaceholder) {
        this.useEmptyPlaceholder = useEmptyPlaceholder;
        return this;
    }

    /**
     * Sets whether to show all columns.
     * 
     * @param showAllColumns true to show all columns, false to show only visible columns
     */
    public ExportSettings setShowAllColumns(boolean showAllColumns) {
        this.showAllColumns = showAllColumns;
        return this;
    }
    
    /**
     * Sets whether to show all rows.
     * 
     * @param showAllRows true to show all rows, false to show only visible rows
     */
    public ExportSettings setShowAllRows(boolean showAllRows) {
        this.showAllRows = showAllRows;
        return this;
    }

    
    /**
     * Gets the destination file.
     * 
     * @return The file to export to
     */
    public File getDestinationFile() {
        return destinationFile;
    }
    
    /**
     * Sets the destination file.
     * 
     * @param destinationFile The file to export to
     * @return This ExportSettings instance for method chaining
     */
    public ExportSettings setDestinationFile(File destinationFile) {
        this.destinationFile = destinationFile;
        return this;
    }
    
    /**
     * Creates an ExportSettings instance from boolean selectors.
     * This is a convenience method for creating settings from the UI selectors.
     * 
     * @param showAllColumns Whether to show all columns
     * @param showAllRows Whether to show all rows
     * @param destinationFile The file to export to
     * @param useEmptyPlaceholder Whether to use a placeholder for empty values
     * @return A new ExportSettings instance
     */
    public static ExportSettings fromSelectors(boolean showAllColumns, boolean showAllRows, File destinationFile, boolean useEmptyPlaceholder) {
        // Create settings based on selector values
        ExportSettings settings = new ExportSettings(destinationFile);

        // Apply settings based on selector values, and return
        return settings.setUseEmptyPlaceholder(useEmptyPlaceholder)
                        .setShowAllColumns(showAllColumns)
                        .setShowAllRows(showAllRows)
                        .setDestinationFile(destinationFile);
        
    }
}
