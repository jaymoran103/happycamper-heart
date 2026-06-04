package com.echo.ui.dialog;

import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import com.echo.domain.EnhancedRoster;
import com.echo.feature.RosterFeature;
import com.echo.logging.WarningManager;
import com.echo.service.ImportSettings;
import com.echo.service.RosterService;
import com.echo.ui.help.PageContentBuilder.HelpPage;
import com.echo.ui.selector.CheckBoxSelector;
import com.echo.ui.selector.FileSelector;
import com.echo.ui.selector.InputSelector;
import com.echo.ui.selector.FileSelector.SelectionMode;

/**
 * Dialog for importing roster data.
 * This dialog extends InputsDialog for a consistent UI experience with other dialogs.
 */
public class ImportDialog extends InputsDialog {
    // Static cache for remembering settings between dialog instances
    private static final ImportSettings cachedSettings = new ImportSettings();

    private final RosterService rosterService;

    private FileSelector camperFileSelector;
    private FileSelector activityFileSelector;
    private CheckBoxSelector featureSelector;

    private boolean importSuccessful = false;
    private EnhancedRoster importedRoster;

    private static final int IMPORT_DIALOG_WIDTH = 500;

    /**
     * Creates a new ImportDialog.
     *
     * @param parent The parent window
     * @param rosterService The roster service to use for importing
     */
    public ImportDialog(Window parent, RosterService rosterService) {
        super(parent, true, "Import Roster", createSelectors(rosterService), IMPORT_DIALOG_WIDTH, "Import");
        this.rosterService = rosterService;

        // Store references to selectors for easier access
        camperFileSelector = (FileSelector) selectors[0];
        activityFileSelector = (FileSelector) selectors[1];
        featureSelector = (CheckBoxSelector) selectors[2];

        //Trigger initial validation
        updateContinueButton();
    }

    /**
     * Creates the selectors for the dialog.
     *
     * @param rosterService The roster service to get available features from
     * @return Array of selectors
     */
    private static InputSelector<?>[] createSelectors(RosterService rosterService) {
        // Create file selectors with cached values
        FileSelector camperFileSelector = new FileSelector("Camper File", SelectionMode.OPEN, new String[]{"csv"}, "CSV Files");
        camperFileSelector.linkHelpPage(HelpPage.CAMPER_FILE);
        if (cachedSettings.getCamperFile() != null) {
            camperFileSelector.setValue(cachedSettings.getCamperFile());
        }

        FileSelector activityFileSelector = new FileSelector("Activity File", SelectionMode.OPEN, new String[]{"csv"}, "CSV Files");
        activityFileSelector.linkHelpPage(HelpPage.ACTIVITY_FILE);
        if (cachedSettings.getActivityFile() != null) {
            activityFileSelector.setValue(cachedSettings.getActivityFile());
        }

        // Create feature selector with available features
        Map<String, Boolean> featureMap = new LinkedHashMap<>();
        List<String> cachedFeatures = cachedSettings.getEnabledFeatureIds();

        // Add all available features to the map
        for (RosterFeature feature : rosterService.getAvailableFeatures()) {
            // Skip activity feature as it's always enabled
            if (!feature.getFeatureId().equals("activity")) {
                // Use cached value if available, otherwise default to true
                boolean isEnabled = cachedFeatures.isEmpty() || cachedFeatures.contains(feature.getFeatureId());
                featureMap.put(feature.getFeatureName(), isEnabled);
            }
        }

        // Create the feature selector with a descriptive title and include "Select All" option
        CheckBoxSelector featureSelector = new CheckBoxSelector("Optional Features", featureMap, false);
        featureSelector.linkHelpPage(HelpPage.FEATURES_OVERVIEW);

        return new InputSelector<?>[] {
            camperFileSelector,
            activityFileSelector,
            featureSelector
        };
    }

    /**
     * Checks if all inputs are valid.
     * Overrides the parent method to add file validation.
     *
     * @return true if all selectors have valid selections, false otherwise
     */
    @Override
    protected boolean areInputsValid() {
        // First check if all selectors have valid selections
        if (!super.areInputsValid()) {
            return false;
        }

        // Check if selectors are initialized
        if (camperFileSelector == null || activityFileSelector == null) {
            return false;
        }

        // Then check if both files are valid
        return camperFileSelector.getValidationResult().isValid() && activityFileSelector.getValidationResult().isValid();
    }

    /**
     * Updates the selections in the cached settings.
     * Called when the user confirms the dialog.
     */
    @Override
    protected void updateSelections() {
        // Get selected files
        File camperFile = camperFileSelector.getValue();
        File activityFile = activityFileSelector.getValue();

        // Get selected features
        Map<String, Boolean> featureMap = featureSelector.getValue();
        List<String> enabledFeatureIds = new ArrayList<>();
        enabledFeatureIds.add("activity"); // Activity is always enabled

        // Add other selected features
        for (RosterFeature feature : rosterService.getAvailableFeatures()) {
            if (!feature.getFeatureId().equals("activity") &&
                featureMap.containsKey(feature.getFeatureName()) &&
                featureMap.get(feature.getFeatureName())) {
                enabledFeatureIds.add(feature.getFeatureId());
            }
        }

        // Save to cache
        cachedSettings.setCamperFile(camperFile)
                      .setActivityFile(activityFile)
                      .setEnabledFeatureIds(enabledFeatureIds);

        // Perform the import
        performImport();
    }

    /**
     * Performs the import using the selected options.
     */
    private void performImport() {
        try {
            // Import roster
            importedRoster = rosterService.createEnhancedRoster(
                cachedSettings.getCamperFile(),
                cachedSettings.getActivityFile(),
                cachedSettings.getEnabledFeatureIds()
            );

            WarningManager warningManager = rosterService.getWarningManager();

            // Display errors if present in warningManager (returns since roster is presumed unusable)
            if (warningManager.hasErrors()) {
                displayErrors(warningManager);
                importSuccessful = false;
                return;
            }

            // Display warnings if no errors occurred (return if user chooses not to continue)
            else if (warningManager.hasWarnings()) {
                WarningDialog warningDialog = new WarningDialog(this, warningManager);
                warningDialog.showDialog();
                if (!warningDialog.checkSelectedContinue()) {
                    importSuccessful = false;
                    return;
                }
            }

            // If roster is null, display error
            if (importedRoster == null) {
                String debugCatch = "Import failed: The roster could not be created";
                JOptionPane.showMessageDialog(this, debugCatch, "Import Error", JOptionPane.ERROR_MESSAGE);
                importSuccessful = false;
                return;
            }

            importSuccessful = true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "An unexpected error occurred during import: " + e.getMessage(),
                "Import Error",
                JOptionPane.ERROR_MESSAGE);
            importSuccessful = false;
        }
    }

    /**
     * Displays errors from the warning manager.
     *
     * @param warningManager The warning manager used to provide errors for the import process
     */
    private void displayErrors(WarningManager warningManager) {
        ErrorDialog errorDialog = new ErrorDialog(this, warningManager);
        errorDialog.showDialog();
    }

    /**
     * Checks if the import was successful.
     *
     * @return true if the import was successful
     */
    public boolean isImportSuccessful() {
        return importSuccessful;
    }

    /**
     * Gets the imported roster.
     *
     * @return The imported roster
     */
    public EnhancedRoster getImportedRoster() {
        return importedRoster;
    }

    /**
     * Automates the selection of files and features for testing.
     *
     * @param camperFile The camper file to use
     * @param activityFile The activity file to use
     * @param features Array of feature IDs to enable
     */
    public void automateSelection(File camperFile, File activityFile, String[] features) {
        // Set file selectors
        camperFileSelector.setValue(camperFile);
        activityFileSelector.setValue(activityFile);

        // Set feature selector
        if (features != null && features.length > 0) {
            updateFeatureSelection(features);
        }

        // Trigger import if valid
        if (areInputsValid()) {
            onContinueClicked();
        }
    }

    /**
     * Updates the feature checkboxes based on the provided feature IDs.
     *
     * @param featureIds Array of feature IDs to enable
     */
    private void updateFeatureSelection(String[] featureIds) {
        // Create a map of feature ID to feature object for quick lookup
        Map<String, RosterFeature> featureMap = new LinkedHashMap<>();
        for (RosterFeature feature : rosterService.getAvailableFeatures()) {
            featureMap.put(feature.getFeatureId(), feature);
        }

        // Create a map of feature names to boolean values
        Map<String, Boolean> selectionMap = new LinkedHashMap<>();
        for (Map.Entry<String, Boolean> entry : featureSelector.getValue().entrySet()) {
            selectionMap.put(entry.getKey(), false); // Start with all unchecked
        }

        // Enable selected features
        for (String featureId : featureIds) {
            // Skip activity as it's always enabled
            if (featureId.equals("activity")) {
                continue;
            }

            RosterFeature feature = featureMap.get(featureId);
            if (feature != null) {
                selectionMap.put(feature.getFeatureName(), true);
            }
        }

        // Update the selector
        featureSelector.setValue(selectionMap);
    }
}
