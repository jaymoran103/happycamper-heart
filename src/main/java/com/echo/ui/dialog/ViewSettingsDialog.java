package com.echo.ui.dialog;

import java.awt.Window;

import com.echo.domain.DataConstants;
import com.echo.service.ViewSettings;
import com.echo.ui.selector.BooleanSelector;
import com.echo.ui.selector.InputSelector;

/**
 * Dialog for selecting view options.
 * Allows the user to customize how data is displayed in tables.
 */
public class ViewSettingsDialog extends InputsDialog {
    private final ViewSettings cachedSettings;

    private BooleanSelector placeholderSelector;
    private BooleanSelector rowContrastSelector;
    private BooleanSelector highlightEmptySelector;

    private static final int VIEW_SETTINGS_DIALOG_WIDTH = 400;

    /**
     * Creates a new ViewSettingsDialog.
     *
     * @param parent The parent window
     * @param settings The view settings to modify
     */
    public ViewSettingsDialog(Window parent, ViewSettings settings) {
        super(parent, true, "View Settings", createSelectors(settings),VIEW_SETTINGS_DIALOG_WIDTH,"Apply");
        this.cachedSettings = settings;

        // Store references to the selectors for easier access
        placeholderSelector = (BooleanSelector) selectors[0];
        rowContrastSelector = (BooleanSelector) selectors[1];
        highlightEmptySelector = (BooleanSelector) selectors[2];
    }

    /**
     * Creates the selectors for the dialog.
     *
     * @param settings The view settings to use for default values
     * @return Array of selectors
     */
    private static InputSelector<?>[] createSelectors(ViewSettings settings) {
        // Create boolean selectors for each option
        BooleanSelector placeholderSelector = new BooleanSelector(
            settings.isUseDisplayPlaceholder(),
            "Empty Cell Placeholder",
            "Use \"" + DataConstants.DISPLAY_NO_DATA + "\"",
            "Use \"" + DataConstants.DISPLAY_EMPTY + "\""
        );

        BooleanSelector rowContrastSelector = new BooleanSelector(
            settings.isUseRowContrast(),
            "Row Contrast",
            "Enable Alternating Row Colors",
            "Use Uniform Row Colors"
        );

        BooleanSelector highlightEmptySelector = new BooleanSelector(
            settings.isHighlightEmptyData(),
            "Empty Cell Highlighting",
            "Highlight Missing Data",
            "Don't Highlight Missing Data"
        );

        return new InputSelector<?>[] {
            placeholderSelector,
            rowContrastSelector,
            highlightEmptySelector
        };
    }

    /**
     * Updates the selections in the cached settings.
     * Called when the user confirms the dialog.
     */
    @Override
    protected void updateSelections() {
        // Save current selections to cache
        cachedSettings.setUseDisplayPlaceholder(placeholderSelector.getValue())
                      .setUseRowContrast(rowContrastSelector.getValue())
                      .setHighlightEmptyData(highlightEmptySelector.getValue());

        // Apply settings immediately
        cachedSettings.apply();
    }

    /**
     * Checks if the settings were confirmed.
     *
     * @return true if the user clicked Apply, false otherwise
     */
    public boolean isSettingsConfirmed() {
        return isInputConfirmed();
    }

    /**
     * Gets the current view settings.
     *
     * @return The current view settings
     */
    public ViewSettings getSettings() {
        return cachedSettings;
    }
}
