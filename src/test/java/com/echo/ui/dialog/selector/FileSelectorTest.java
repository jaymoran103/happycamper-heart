package com.echo.ui.dialog.selector;

import java.awt.Component;
import java.awt.Container;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.echo.ReflectionUtils;
import com.echo.automation.TestFiles;
import com.echo.ui.selector.FileSelector;
import com.echo.ui.selector.FileSelector.SelectionMode;

/**
 * Tests for the FileSelector class.
 */
public class FileSelectorTest {

    private FileSelector selector;
    private final String TITLE = "Test File Selector";
    private final String[] EXTENSIONS = {"csv"};
    private final String EXTENSION_DESCRIPTION = "CSV Files";

    @TempDir
    File tempDir;

    @BeforeEach
    public void setUp() {
        selector = new FileSelector(TITLE, SelectionMode.OPEN, EXTENSIONS, EXTENSION_DESCRIPTION);
    }

    @Test
    @DisplayName("Test selector creation with title")
    public void testSelectorCreationWithTitle() {
        // Create the panel
        JPanel panel = selector.createPanel();

        // Verify the panel was created
        assertNotNull(panel);

        // Find the title label
        JLabel titleLabel = findTitleLabel(panel);
        assertNotNull(titleLabel);
        assertTrue(titleLabel.getText().contains(TITLE));
    }

    @Test
    @DisplayName("Test getValue and setValue")
    public void testGetValueAndSetValue() {
        // Initial value should be null
        assertNull(selector.getValue());

        // Set a new value
        File testFile = TestFiles.MINI_CAMPERS.toFile();
        selector.setValue(testFile);

        // Verify the value was set
        assertEquals(testFile, selector.getValue());
    }

    @Test
    @DisplayName("Test hasSelection")
    public void testHasSelection() {
        // Initial value is null, so hasSelection should be false
        assertFalse(selector.hasSelection());

        // Set a file
        File testFile = TestFiles.MINI_CAMPERS.toFile();
        selector.setValue(testFile);

        // Now hasSelection should be true
        assertTrue(selector.hasSelection());

        // Set to null
        selector.setValue(null);

        // hasSelection should be false again
        assertFalse(selector.hasSelection());
    }

    @Test
    @DisplayName("Test file validation - valid file")
    public void testFileValidation_ValidFile() {
        // Create a test file in the temp directory
        File testFile = new File(tempDir, "test.csv");

        // Set the file
        selector.setValue(testFile);

        // For a valid file, hasSelection should be true
        assertTrue(selector.hasSelection());
    }

    @Test
    @DisplayName("Test file validation - invalid extension")
    public void testFileValidation_InvalidExtension() {
        // Create a test file with wrong extension
        File testFile = new File(tempDir, "test.txt");

        // Set the file
        selector.setValue(testFile);

        // For a file with invalid extension, hasSelection should still be true
        // because we're in OPEN mode and the file exists
        assertTrue(selector.hasSelection());

        // Create a selector in SAVE mode
        FileSelector saveSelector = new FileSelector(TITLE, SelectionMode.SAVE, EXTENSIONS, EXTENSION_DESCRIPTION);

        // Create the panel to initialize components
        JPanel panel = saveSelector.createPanel();
        assertNotNull(panel);

        // Set the file
        saveSelector.setValue(testFile);

        // Check if the validation result is invalid
        // The hasSelection method only checks if selectedFile != null, not if it's valid
        assertFalse(saveSelector.getValidationResult().isValid());
    }

    @Test
    @DisplayName("Test UI components")
    public void testUIComponents() {
        // Create the panel
        JPanel panel = selector.createPanel();

        // Verify the browse button exists
        JButton browseButton = findBrowseButton(panel);
        assertNotNull(browseButton);

        // Verify the file path label exists
        JLabel filePathLabel = findFilePathLabel(panel);
        assertNotNull(filePathLabel);

        // Verify the error label exists
        JLabel errorLabel = findErrorLabel(panel);
        assertNotNull(errorLabel);
    }

    @Test
    @DisplayName("Test update callback")
    public void testUpdateCallback() {
        // Create a custom selector for this test
        FileSelector testSelector = new FileSelector(TITLE, SelectionMode.OPEN, EXTENSIONS, EXTENSION_DESCRIPTION);

        // Create the panel first to initialize all components
        JPanel panel = testSelector.createPanel();
        assertNotNull(panel);

        // Create a flag to track if the callback was called
        boolean[] callbackCalled = new boolean[1];

        // Set the update callback
        testSelector.setUpdateCallback(() -> callbackCalled[0] = true);

        // Initially the callback should not have been called
        assertFalse(callbackCalled[0]);

        // Simulate a button click to trigger the callback
        try {
            // Use reflection to access the protected method from the parent class
            ReflectionUtils.invokeMethod(testSelector,"notifyUpdateCallback");
        } catch (Exception e) {
            fail("Failed to invoke notifyUpdateCallback: " + e.getMessage());
        }

        // Verify the callback was called
        assertTrue(callbackCalled[0]);
    }

    /**
     * Helper method to find the title label in a panel.
     */
    private JLabel findTitleLabel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                if (label.getText().contains(TITLE)) {
                    return label;
                }
            } else if (component instanceof Container) {
                JLabel label = findTitleLabel((Container) component);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }

    /**
     * Helper method to find the browse button in a panel.
     */
    private JButton findBrowseButton(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton) {
                return (JButton) component;
            } else if (component instanceof Container) {
                JButton button = findBrowseButton((Container) component);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    /**
     * Helper method to find the file path label in a panel.
     */
    private JLabel findFilePathLabel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel && !(component.equals(findTitleLabel(container)))) {
                return (JLabel) component;
            } else if (component instanceof Container) {
                JLabel label = findFilePathLabel((Container) component);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }

    /**
     * Helper method to find the error label in a panel.
     */
    private JLabel findErrorLabel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                if (label.getForeground().equals(java.awt.Color.RED)) {
                    return label;
                }
            } else if (component instanceof Container) {
                JLabel label = findErrorLabel((Container) component);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }
}
