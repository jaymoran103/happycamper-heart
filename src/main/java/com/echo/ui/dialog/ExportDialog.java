package com.echo.ui.dialog;

import java.awt.Window;
import java.io.File;

import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.filter.FilterManager;
import com.echo.service.ExportSettings;
import com.echo.service.RosterService;
import com.echo.ui.selector.BooleanSelector;
import com.echo.ui.selector.FileSelector;
import com.echo.ui.selector.FileSelector.SelectionMode;
import com.echo.ui.selector.InputSelector;
import com.echo.validation.ValidationResult;

/**
 * Dialog for selecting export options and file path.
 * Allows the user to choose what data to export and where to save it.
 */
public class ExportDialog extends InputsDialog {
    // Static cache for remembering settings between dialog instances
    private static final ExportSettings cachedSettings = new ExportSettings();

    private final RosterService rosterService;
    private final EnhancedRoster roster;
    private final FilterManager filterManager;

    private BooleanSelector showAllColumnsSelector;
    private BooleanSelector showAllRowsSelector;
    private BooleanSelector emptyValueSelector;
    private FileSelector fileSelector;

    private static final int EXPORT_DIALOG_WIDTH = 400;

    //TODO replace with enum?
    private int dataVisibilityChoice = 0; // 0 = All data, 1 = Visible columns, 2 = Visible columns and rows
    private boolean placeholderChoice = false;

    /**
     * Creates a new ExportDialog.
     *
     * @param parent The parent window
     * @param rosterService The roster service to use for exporting
     * @param roster The roster to export
     * @param filterManager The filter manager to apply
     */
    public ExportDialog(Window parent, RosterService rosterService, EnhancedRoster roster, FilterManager filterManager) {
        super(parent, true, "Export Options", createSelectors(),EXPORT_DIALOG_WIDTH,"Export");
        this.rosterService = rosterService;
        this.roster = roster;
        this.filterManager = filterManager;

        // Store references to the selectors for easier access
        showAllColumnsSelector = (BooleanSelector) selectors[0];
        showAllRowsSelector = (BooleanSelector) selectors[1];
        emptyValueSelector = (BooleanSelector) selectors[2];
        fileSelector = (FileSelector) selectors[3];

        // Override the update callback for the file selector
        fileSelector.setUpdateCallback(this::updateContinueButton);

        // Initial validation
        updateContinueButton();
    }

    /**
     * Creates the selectors for the dialog.
     *
     * @return Array of selectors
     */
    private static InputSelector<?>[] createSelectors() {
        // Create selectors with cached values
        boolean showAllColumns = cachedSettings.getShowAllColumns();
        boolean showAllRows = cachedSettings.getShowAllRows();

        BooleanSelector showAllColumnsSelector = new BooleanSelector(showAllColumns, "Select Columns to Export", "All Columns", "Visible Columns Only");
        BooleanSelector showAllRowsSelector = new BooleanSelector(showAllRows, "Select Rows to Export", "All Rows", "Visible Rows Only");
        BooleanSelector emptyValueSelector = new BooleanSelector(cachedSettings.getUseEmptyPlaceholder(), "Empty Cell Representation", "Use Placeholder: \"" + DataConstants.DISPLAY_NO_DATA + "\"", "Leave Cells Empty");

        // Use cached file if available
        File cachedFile = cachedSettings.getDestinationFile();
        FileSelector fileSelector = new FileSelector("Export File", SelectionMode.SAVE, new String[]{"csv","txt"}, "CSV Files");
        if (cachedFile != null) {
            fileSelector.setValue(cachedFile);
        }

        return new InputSelector<?>[] {
            showAllColumnsSelector,
            showAllRowsSelector,
            emptyValueSelector,
            fileSelector
        };
    }

    /**
     * Checks if all inputs are valid.
     * Overrides the parent method to add file validation.
     *
     * @return true if all selectors have valid selections and the file is valid, false otherwise
     */
    @Override
    protected boolean areInputsValid() {
        // First check if all selectors have valid selections
        if (!super.areInputsValid()) {
            return false;
        }

        // Then check if the file is valid
        if (fileSelector != null) {
            ValidationResult<File> result = fileSelector.getValidationResult();
            return result != null && result.isValid();
        }

        return false;
    }

    /**
     * Updates the selections in the cached settings.
     * Called when the user confirms the dialog.
     */
    @Override
    protected void updateSelections() {
        // Determine export option based on selector values

        // Save current selections to cache
        placeholderChoice = emptyValueSelector.getValue();
        cachedSettings.setShowAllColumns(showAllColumnsSelector.getValue())
                      .setShowAllRows(showAllRowsSelector.getValue())
                      .setUseEmptyPlaceholder(placeholderChoice)
                      .setDestinationFile(fileSelector.getValue());
    }

    /**
     * Checks if the export was confirmed.
     *
     * @return true if the export was confirmed, false otherwise
     */
    public boolean isExportConfirmed() {
        return isInputConfirmed();
    }

    /**
     * Gets the selected file.
     *
     * @return The selected file
     */
    public File getSelectedFile() {
        return fileSelector != null ? fileSelector.getValue() : null;
    }

    /**
     * Gets the export option.
     *
     * @return 0 for all data, 1 for visible columns only, 2 for visible columns and rows
     */
    public int getDataVisibilityChoice() {
        return dataVisibilityChoice;
    }

    /**
     * Gets whether to use a placeholder for empty values.
     *
     * @return true if empty values should be replaced with a placeholder, false to leave them empty
     */
    public boolean getUsePlaceholder() {
        return placeholderChoice;
    }

    /**
     * Performs the export using the selected options.
     *
     * @throws Exception if an error occurs during export
     */
    public void performExport() throws Exception {
        File selectedFile = getSelectedFile();
        if (!isInputConfirmed() || selectedFile == null) {
            return;
        }

        // Create export settings from the dialog selections
        ExportSettings settings = ExportSettings.fromSelectors(
            showAllColumnsSelector.getValue(),
            showAllRowsSelector.getValue(),
            selectedFile,
            emptyValueSelector.getValue()
        );

        // Perform export with the settings
        rosterService.exportRoster(roster, filterManager, settings);
    }
}
