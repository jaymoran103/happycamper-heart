package com.echo.ui.dialog.selector;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.ui.selector.NumberInputSelector;

/**
 * Tests for the NumberInputSelector class.
 */
public class NumberInputSelectorTest {

    private NumberInputSelector selector;
    private final String TITLE = "Test Number Input Selector";
    private final int DEFAULT_VALUE = 5;
    private final int MIN_VALUE = 1;
    private final int MAX_VALUE = 10;

    @BeforeEach
    public void setUp() {
        selector = new NumberInputSelector(TITLE, DEFAULT_VALUE, MIN_VALUE, MAX_VALUE);
    }

    @Test
    @DisplayName("Test selector creation with title and default value")
    public void testSelectorCreationWithTitleAndDefaultValue() {
        // Create the panel
        JPanel panel = selector.createPanel();

        // Verify the panel was created
        assertNotNull(panel);

        // Verify the initial value
        assertEquals(DEFAULT_VALUE, selector.getValue());

        // Find the text field
        JTextField textField = findTextField(panel);
        assertNotNull(textField);

        // Verify the text field contains the default value
        assertEquals(String.valueOf(DEFAULT_VALUE), textField.getText());
    }

    @Test
    @DisplayName("Test getValue and setValue")
    public void testGetValueAndSetValue() {
        // Initial value should be the default
        assertEquals(DEFAULT_VALUE, selector.getValue());

        // Set to a different value
        int newValue = 7;
        selector.setValue(newValue);

        // Verify the value was set
        assertEquals(newValue, selector.getValue());

        // Create the panel to initialize the text field
        JPanel panel = selector.createPanel();

        // Find the text field
        JTextField textField = findTextField(panel);
        assertNotNull(textField);

        // Verify the text field was updated
        assertEquals(String.valueOf(newValue), textField.getText());
    }

    @Test
    @DisplayName("Test setValue with invalid values")
    public void testSetValueWithInvalidValues() {
        // Set to a value below the minimum
        selector.setValue(MIN_VALUE - 1);

        // Value should not change
        assertEquals(DEFAULT_VALUE, selector.getValue());

        // Set to a value above the maximum
        selector.setValue(MAX_VALUE + 1);

        // Value should not change
        assertEquals(DEFAULT_VALUE, selector.getValue());

        // Set to null
        selector.setValue(null);

        // Value should not change
        assertEquals(DEFAULT_VALUE, selector.getValue());
    }

    @Test
    @DisplayName("Test hasSelection")
    public void testHasSelection() {
        // Create the panel to initialize the text field
        JPanel panel = selector.createPanel();

        // Initially has a valid selection
        assertTrue(selector.hasSelection());

        // Find the text field
        JTextField textField = findTextField(panel);
        assertNotNull(textField);

        // Set to an invalid value
        textField.setText("invalid");

        // Should not have a valid selection
        assertFalse(selector.hasSelection());

        // Set to a value below the minimum
        textField.setText(String.valueOf(MIN_VALUE - 1));

        // Should not have a valid selection
        assertFalse(selector.hasSelection());

        // Set to a value above the maximum
        textField.setText(String.valueOf(MAX_VALUE + 1));

        // Should not have a valid selection
        assertFalse(selector.hasSelection());

        // Set to a valid value
        textField.setText(String.valueOf(DEFAULT_VALUE));

        // Should have a valid selection
        assertTrue(selector.hasSelection());
    }

    @Test
    @DisplayName("Test text field interaction")
    public void testTextFieldInteraction() {
        // Create the panel to initialize the text field
        JPanel panel = selector.createPanel();

        // Find the text field
        JTextField textField = findTextField(panel);
        assertNotNull(textField);

        // Set a new value in the text field
        int newValue = 8;
        textField.setText(String.valueOf(newValue));

        // Simulate pressing Enter
        textField.postActionEvent();

        // Verify the value was updated
        assertEquals(newValue, selector.getValue());
    }

    @Test
    @DisplayName("Test update callback")
    public void testUpdateCallback() {
        // Create a flag to track if the callback was called
        boolean[] callbackCalled = new boolean[1];

        // Set the update callback
        selector.setUpdateCallback(() -> callbackCalled[0] = true);

        // Initially the callback should not have been called
        assertFalse(callbackCalled[0]);

        // Create the panel to initialize the text field
        JPanel panel = selector.createPanel();

        // Find the text field
        JTextField textField = findTextField(panel);
        assertNotNull(textField);

        // Set a new value in the text field
        textField.setText("8");

        // Simulate pressing Enter
        textField.postActionEvent();

        // Verify the callback was called
        assertTrue(callbackCalled[0]);
    }

    @Test
    @DisplayName("Test setVisible method")
    public void testSetVisibleMethod() {
        // Create the panel to initialize the text field
        JPanel panel = selector.createPanel();

        // Initially the panel should be visible
        assertTrue(panel.isVisible());

        // Create a new selector for this test to avoid affecting other tests
        NumberInputSelector testSelector = new NumberInputSelector(TITLE, DEFAULT_VALUE, MIN_VALUE, MAX_VALUE);
        JPanel testPanel = testSelector.createPanel();

        // Initially the panel should be visible
        assertTrue(testPanel.isVisible());

        // Hide the selector
        testSelector.setVisible(false);

        // Verify the panel is hidden - this might be the parent panel
        // The implementation uses inputField.getParent().getParent().setVisible(visible)
        // So we need to check if any parent panel is hidden
        boolean anyParentHidden = !testPanel.isVisible() ||
                                 !findTextField(testPanel).getParent().isVisible() ||
                                 !findTextField(testPanel).getParent().getParent().isVisible();

        assertTrue(anyParentHidden, "At least one parent panel should be hidden");

        // Show the selector again
        testSelector.setVisible(true);

        // Verify the panel is visible again
        assertTrue(testPanel.isVisible());
    }

    /**
     * Helper method to find a text field in a panel.
     */
    private JTextField findTextField(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTextField) {
                return (JTextField) component;
            } else if (component instanceof Container) {
                JTextField textField = findTextField((Container) component);
                if (textField != null) {
                    return textField;
                }
            }
        }
        return null;
    }
}
