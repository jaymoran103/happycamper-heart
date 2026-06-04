package com.echo.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.echo.ui.dialog.ColumnSizingOption;

/**
 * Settings for column visibility and sizing.
 * This class encapsulates all settings related to how columns are displayed in tables.
 */
public class ColumnSettings {

    private Map<String, Boolean> columnVisibility = new LinkedHashMap<>();
    private ColumnSizingOption sizingOption = ColumnSizingOption.AUTO_SIZE;
    private int customWidth = 100; // Default custom width in pixels

    /**
     * Creates a new ColumnSettings instance with default values.
     */
    public ColumnSettings() {
        // Default constructor
    }

    /**
     * Gets the column visibility map.
     *
     * @return A map of column names to visibility status
     */
    public Map<String, Boolean> getColumnVisibility() {
        return new LinkedHashMap<>(columnVisibility); // Return a copy to prevent modification
    }

    /**
     * Sets the column visibility map.
     *
     * @param columnVisibility A map of column names to visibility status
     * @return This ColumnSettings instance for method chaining
     */
    public ColumnSettings setColumnVisibility(Map<String, Boolean> columnVisibility) {
        this.columnVisibility = new LinkedHashMap<>(columnVisibility); // Store a copy to prevent modification
        return this;
    }

    /**
     * Gets the column sizing option.
     *
     * @return The current column sizing option
     */
    public ColumnSizingOption getSizingOption() {
        return sizingOption;
    }

    /**
     * Sets the column sizing option.
     *
     * @param sizingOption The new column sizing option
     * @return This ColumnSettings instance for method chaining
     */
    public ColumnSettings setSizingOption(ColumnSizingOption sizingOption) {
        this.sizingOption = sizingOption;
        return this;
    }

    /**
     * Gets the custom column width.
     *
     * @return The custom column width in pixels
     */
    public int getCustomWidth() {
        return customWidth;
    }

    /**
     * Sets the custom column width.
     *
     * @param customWidth The new custom column width in pixels
     * @return This ColumnSettings instance for method chaining
     */
    public ColumnSettings setCustomWidth(int customWidth) {
        this.customWidth = customWidth;
        return this;
    }

    /**
     * Applies these column settings to a roster.
     *
     * @param roster The roster to apply the settings to
     */
    public void applyToRoster(com.echo.domain.EnhancedRoster roster) {
        // Apply column visibility settings
        for (Map.Entry<String, Boolean> entry : columnVisibility.entrySet()) {
            roster.setHeaderVisibility(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Updates this settings object from a roster's current state.
     *
     * @param roster The roster to get settings from
     * @return This ColumnSettings instance for method chaining
     */
    public ColumnSettings updateFromRoster(com.echo.domain.EnhancedRoster roster) {
        // Update column visibility from roster
        Map<String, Boolean> newVisibility = new LinkedHashMap<>();
        for (String header : roster.getAllHeaders()) {
            newVisibility.put(header, roster.isHeaderVisible(header));
        }
        this.columnVisibility = newVisibility;
        return this;
    }

    /**
     * Clears all column visibility settings.
     * This should be called when a new roster is loaded to ensure stale settings are not applied.
     *
     * @return This ColumnSettings instance for method chaining
     */
    public ColumnSettings clearColumnVisibility() {
        this.columnVisibility.clear();
        return this;
    }
}
