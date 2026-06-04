package com.echo.ui.selector;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.echo.service.FileHandler;
import com.echo.ui.dialog.DialogConstants;
import com.echo.ui.dialog.DialogUtils;
import com.echo.ui.elements.HoverButton;
import com.echo.validation.ExportFileValidator;
import com.echo.validation.ImportFileValidator;
import com.echo.validation.ValidationResult;

/**
 * FileSelector facilitates selecting files in an input dialog.
 * This class stores a file reference and builds a panel that allows the user to select a file.
 */
public class FileSelector extends InputSelector<File> {
    /**
     * Enum for file selection mode.
     */
    public enum SelectionMode {
        OPEN,
        SAVE
    }

    private File selectedFile;
    private ValidationResult<File> validationResult;

    private final SelectionMode mode;
    private final String[] extensions;
    private final String extensionDescription;

    private JLabel filePathLabel;
    private JLabel errorLabel;

    private static final String BUTTON_TEXT = "Browse";
    private static final String DEFAULT_LABEL_TEXT = "No file selected";
    private static final String DEFAULT_ERRORLABEL_TEXT = " ";


    /**
     * Constructor for file selector with specified mode and extensions.
     *
     * @param title Label text at the top of selector box
     * @param mode enum indicating selection mode (OPEN or SAVE)
     * @param extensions File extensions to filter (e.g., "csv", "txt")
     * @param extensionDescription Description for the file filter
     */
    public FileSelector(String title, SelectionMode mode, String[] extensions, String extensionDescription) {
        super(title);
        this.mode = mode;
        this.extensions = extensions;
        this.extensionDescription = extensionDescription;
    }

    /**
     * Constructor for file selector with specified mode and no extension filter.
     *
     * @param title Label text at the top of selector box
     * @param mode The selection mode (OPEN or SAVE)
     */
    public FileSelector(String title, SelectionMode mode) {
        this(title, mode, null, null);
    }

    @Override
    protected void buildSelectorPanel(JPanel panel) {
        // Use the helper method to create a properly aligned panel with BorderLayout
        JPanel fileSelectionPanel = DialogUtils.createAlignedFlowPanel();

        // Create the browse button
        JButton browseButton = new HoverButton(BUTTON_TEXT);
        browseButton.addActionListener(this::handleSelection);

        // Create the file path label
        filePathLabel = new JLabel(DEFAULT_LABEL_TEXT);

        fileSelectionPanel.add(browseButton);
        fileSelectionPanel.add(filePathLabel);

        // Create error label for validation messages
        errorLabel = new JLabel(DEFAULT_ERRORLABEL_TEXT);
        errorLabel.setForeground(DialogConstants.TEXT_COLOR_ERROR);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add components to the main panel
        panel.add(fileSelectionPanel);
        DialogUtils.addVerticalSpacing(panel, 5);
        panel.add(errorLabel);

        // Would set the component height here, but the default is perfect
        // this.componentHeight = 80;

        // Update label if file is already selected
        updateFilePathLabel();

        // Validate the file if one is already selected
        if (selectedFile != null) {
            validateFile();
        }
    }


    private void handleSelection(ActionEvent e) {
        File selection = selectedFile;

        if (mode == SelectionMode.OPEN) {
            selection = FileHandler.getLoadFile(null, selectedFile);
        } else {
            selection = FileHandler.getSaveFile(null, selectedFile);
        }

        if (selection == null) {
            return;
        }

        // // Ensure file has the correct extension for save mode
        // if (mode == SelectionMode.SAVE && extensions != null && extensions.length > 0) {
        //     selection = ExportFileValidator.ensureExtension(selection, extensions);
        // }

        selectedFile = selection;
        updateFilePathLabel();

        // Validate the file and update error message
        validateFile();

        notifyUpdateCallback();
    }




    /**
     * Updates the file path label with the selected file path.
     */
    private void updateFilePathLabel() {
        if (filePathLabel != null) {
            filePathLabel.setText(getShortenedPath(selectedFile));
        }
    }


    /**
     * Validates the selected file and updates the error label.
     * For save mode, checks that the file can be written to and has a valid extension.
     * For open mode, checks that the file exists and can be read.
     *
     * @return true if the file is valid, false otherwise
     */
    private boolean validateFile() {
        if (errorLabel == null) {
            return false;
        }

        // Get validation result
        doValidation();

        if (!validationResult.isValid()) {
            errorLabel.setText(validationResult.getErrorMessage());
            return false;
        }

        // Clear error message
        errorLabel.setText(DEFAULT_ERRORLABEL_TEXT);
        return true;
    }

    @Override
    public File getValue() {
        return selectedFile;
    }

    @Override
    public void setValue(File value) {
        this.selectedFile = value;
        updateFilePathLabel();
        validateFile();
    }

    @Override
    public boolean hasSelection() {
        return selectedFile != null;
    }

    /**
     * Gets the validation result for the currently selected file.
     * This can be used by parent components to display validation errors.
     *
     * @return The validation result, or a failure result if no file is selected
     */
    public ValidationResult<File> getValidationResult() {
        return validationResult;
    }


    private void doValidation(){
        ValidationResult<File> result;
        if (selectedFile==null){
            result = ValidationResult.failure("No file selected", "Please select a file");
        } else if (mode == SelectionMode.SAVE && extensions != null && extensions.length > 0) {
            result = validateChosenPath(selectedFile);
        } else {
            result = ValidationResult.success(selectedFile);
        }
        this.validationResult = result;
    }

    private String getShortenedPath(File f){
        if (f==null){
            return DEFAULT_LABEL_TEXT;
        }

        String parent = f.getParent().split(File.separator)[0];
        String name = f.getName();

        if (parent.equals("")){
            return name;
        }
        return parent + File.separator + name;
    }

    private ValidationResult<File> validateChosenPath(File selectedFile){
        return switch (mode) {
            case OPEN -> ImportFileValidator.<File>validateImportFile(selectedFile);
            case SAVE -> ExportFileValidator.validateExportFile(selectedFile, extensions);
        };
    }
    
    
    
}
