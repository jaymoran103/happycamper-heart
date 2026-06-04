package com.echo.ui.dialog.selector;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JPanel;
import javax.swing.JRadioButton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.ui.dialog.ColumnSizingOption;
import com.echo.ui.selector.RadioButtonSelector;

/**
 * Tests for the RadioButtonSelector class.
 */
public class RadioButtonSelectorTest {
    
    private RadioButtonSelector<ColumnSizingOption> selector;
    private final String TITLE = "Test Radio Button Selector";
    private final ColumnSizingOption DEFAULT_SELECTION = ColumnSizingOption.AUTO_SIZE;
    
    @BeforeEach
    public void setUp() {
        selector = new RadioButtonSelector<>(
            TITLE,
            ColumnSizingOption.values(),
            DEFAULT_SELECTION,
            ColumnSizingOption::getDisplayName
        );
    }
    
    @Test
    @DisplayName("Test selector creation with title and default value")
    public void testSelectorCreationWithTitleAndDefaultValue() {
        // Create the panel
        JPanel panel = selector.createPanel();
        
        // Verify the panel was created
        assertNotNull(panel);
        
        // Verify the initial value
        assertEquals(DEFAULT_SELECTION, selector.getValue());
        
        // Find the radio buttons
        for (ColumnSizingOption option : ColumnSizingOption.values()) {
            JRadioButton button = findRadioButton(panel, option.getDisplayName());
            assertNotNull(button, "Button for " + option + " should exist");
            
            // Verify the selection state
            assertEquals(option == DEFAULT_SELECTION, button.isSelected());
        }
    }
    
    @Test
    @DisplayName("Test getValue and setValue")
    public void testGetValueAndSetValue() {
        // Initial value should be the default
        assertEquals(DEFAULT_SELECTION, selector.getValue());
        
        // Set to a different value
        ColumnSizingOption newValue = ColumnSizingOption.EQUAL_WIDTH;
        selector.setValue(newValue);
        
        // Verify the value was set
        assertEquals(newValue, selector.getValue());
        
        // Set back to the default
        selector.setValue(DEFAULT_SELECTION);
        
        // Verify the value was set
        assertEquals(DEFAULT_SELECTION, selector.getValue());
    }
    
    @Test
    @DisplayName("Test hasSelection")
    public void testHasSelection() {
        // Initially has a selection
        assertTrue(selector.hasSelection());
        
        // Set to null
        selector.setValue(null);
        
        // Should still have the previous selection
        assertTrue(selector.hasSelection());
        assertEquals(DEFAULT_SELECTION, selector.getValue());
    }
    
    @Test
    @DisplayName("Test radio button interaction")
    public void testRadioButtonInteraction() {
        // Create the panel to initialize the radio buttons
        JPanel panel = selector.createPanel();
        
        // Find the radio buttons
        JRadioButton autoSizeButton = findRadioButton(panel, ColumnSizingOption.AUTO_SIZE.getDisplayName());
        JRadioButton fixedWidthButton = findRadioButton(panel, ColumnSizingOption.EQUAL_WIDTH.getDisplayName());
        
        assertNotNull(autoSizeButton);
        assertNotNull(fixedWidthButton);
        
        // Initially AUTO_SIZE button should be selected
        assertTrue(autoSizeButton.isSelected());
        assertFalse(fixedWidthButton.isSelected());
        
        // Simulate clicking the FIXED_WIDTH button
        fixedWidthButton.setSelected(true);
        fixedWidthButton.getActionListeners()[0].actionPerformed(null);
        
        // Verify the value changed
        assertEquals(ColumnSizingOption.EQUAL_WIDTH, selector.getValue());
        
        // Verify button states
        assertFalse(autoSizeButton.isSelected());
        assertTrue(fixedWidthButton.isSelected());
        
        // Simulate clicking the AUTO_SIZE button
        autoSizeButton.setSelected(true);
        autoSizeButton.getActionListeners()[0].actionPerformed(null);
        
        // Verify the value changed
        assertEquals(ColumnSizingOption.AUTO_SIZE, selector.getValue());
        
        // Verify button states
        assertTrue(autoSizeButton.isSelected());
        assertFalse(fixedWidthButton.isSelected());
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
        
        // Create the panel to initialize the radio buttons
        JPanel panel = selector.createPanel();
        
        // Find the FIXED_WIDTH radio button
        JRadioButton fixedWidthButton = findRadioButton(panel, ColumnSizingOption.EQUAL_WIDTH.getDisplayName());
        assertNotNull(fixedWidthButton);
        
        // Simulate clicking the FIXED_WIDTH button
        fixedWidthButton.setSelected(true);
        fixedWidthButton.getActionListeners()[0].actionPerformed(null);
        
        // Verify the callback was called
        assertTrue(callbackCalled[0]);
    }
    
    /**
     * Helper method to find a radio button with specific text.
     */
    private JRadioButton findRadioButton(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JRadioButton) {
                JRadioButton button = (JRadioButton) component;
                if (button.getText().equals(text)) {
                    return button;
                }
            } else if (component instanceof Container) {
                JRadioButton button = findRadioButton((Container) component, text);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }
}
