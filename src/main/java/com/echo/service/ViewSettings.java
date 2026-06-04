package com.echo.service;

import com.echo.domain.DataConstants;
import com.echo.ui.component.TableColors;

/**
 * Settings for the table view display.
 * This class encapsulates all settings related to how data is displayed in tables.
 */
public class ViewSettings {

    /**
     * Enum for view options.
     */
    public enum ViewOption {
        USE_DISPLAY_PLACEHOLDER(false),    // Use "No Data" placeholder for empty cells
        USE_ROW_CONTRAST(true),           // Darken alternating rows
        HIGHLIGHT_EMPTY_DATA(true);       // Highlight empty cell

        private final boolean defaultValue;

        ViewOption(boolean defaultValue) {
            this.defaultValue = defaultValue;
        }

        public boolean getDefaultValue() {
            return defaultValue;
        }

    }

    private boolean useDisplayPlaceholder = ViewOption.USE_DISPLAY_PLACEHOLDER.getDefaultValue();
    private boolean useRowContrast = ViewOption.USE_ROW_CONTRAST.getDefaultValue();
    private boolean highlightEmptyData = ViewOption.HIGHLIGHT_EMPTY_DATA.getDefaultValue();

    // Constructor with default values
    public ViewSettings() {
        apply();
    }

    // Getters and setters with fluent interface
    public boolean isUseDisplayPlaceholder() {
        return useDisplayPlaceholder;
    }

    public ViewSettings setUseDisplayPlaceholder(boolean useDisplayPlaceholder) {
        this.useDisplayPlaceholder = useDisplayPlaceholder;
        return this;
    }

    public boolean isUseRowContrast() {
        return useRowContrast;
    }

    public ViewSettings setUseRowContrast(boolean useRowContrast) {
        this.useRowContrast = useRowContrast;
        return this;
    }

    public boolean isHighlightEmptyData() {
        return highlightEmptyData;
    }

    public ViewSettings setHighlightEmptyData(boolean highlightEmptyData) {
        this.highlightEmptyData = highlightEmptyData;
        return this;
    }

    /**
     * Applies these view settings to the relevant components.
     */
    public void apply() {
        // Update DataConstants placeholder setting
        DataConstants.updateUseDisplayPlaceholder(useDisplayPlaceholder);

        // Update TableColors row contrast setting
        TableColors.setAlternateShades(useRowContrast);

        // Update TableColors empty data highlighting setting
        TableColors.setHighlightEmptyData(highlightEmptyData);
    }
}
