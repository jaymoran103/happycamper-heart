package com.echo.ui.dialog.selector;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.ui.selector.BooleanSelector;

/**
 * Tests for the BooleanSelector class.
 */
public class BooleanSelectorTest {
    
    private BooleanSelector selector;
    private final String TITLE = "Test Boolean Selector";
    private final String TRUE_TEXT = "Yes";
    private final String FALSE_TEXT = "No";
    private final boolean DEFAULT_VALUE = true;
    
    @BeforeEach
    public void setUp() {
        selector = new BooleanSelector(DEFAULT_VALUE, TITLE, TRUE_TEXT, FALSE_TEXT);
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
        
        // Find the radio buttons
        JRadioButton trueButton = findRadioButton(panel, TRUE_TEXT);
        JRadioButton falseButton = findRadioButton(panel, FALSE_TEXT);
        
        assertNotNull(trueButton);
        assertNotNull(falseButton);
        
        // Verify the initial selection state
        assertEquals(DEFAULT_VALUE, trueButton.isSelected());
        assertEquals(!DEFAULT_VALUE, falseButton.isSelected());
    }
    
    @Test
    @DisplayName("Test getValue and setValue")
    public void testGetValueAndSetValue() {
        // Initial value should be the default
        assertEquals(DEFAULT_VALUE, selector.getValue());
        
        // Set to false
        selector.setValue(false);
        
        // Verify the value was set
        assertFalse(selector.getValue());
        
        // Set back to true
        selector.setValue(true);
        
        // Verify the value was set
        assertTrue(selector.getValue());
    }
    
    @Test
    @DisplayName("Test hasSelection")
    public void testHasSelection() {
        // BooleanSelector always has a selection
        assertTrue(selector.hasSelection());
        
        // Set to false
        selector.setValue(false);
        
        // Still has a selection
        assertTrue(selector.hasSelection());
        
        // Set to null (should be ignored)
        selector.setValue(null);
        
        // Still has a selection
        assertTrue(selector.hasSelection());
    }
    
    @Test
    @DisplayName("Test radio button interaction")
    public void testRadioButtonInteraction() {
        // Create the panel to initialize the radio buttons
        JPanel panel = selector.createPanel();
        
        // Find the radio buttons
        JRadioButton trueButton = findRadioButton(panel, TRUE_TEXT);
        JRadioButton falseButton = findRadioButton(panel, FALSE_TEXT);
        
        assertNotNull(trueButton);
        assertNotNull(falseButton);
        
        // Initially true button should be selected
        assertTrue(trueButton.isSelected());
        assertFalse(falseButton.isSelected());
        
        // Simulate clicking the false button
        falseButton.setSelected(true);
        falseButton.getActionListeners()[0].actionPerformed(null);
        
        // Verify the value changed
        assertFalse(selector.getValue());
        
        // Verify button states
        assertFalse(trueButton.isSelected());
        assertTrue(falseButton.isSelected());
        
        // Simulate clicking the true button
        trueButton.setSelected(true);
        trueButton.getActionListeners()[0].actionPerformed(null);
        
        // Verify the value changed
        assertTrue(selector.getValue());
        
        // Verify button states
        assertTrue(trueButton.isSelected());
        assertFalse(falseButton.isSelected());
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
        
        // Find the false radio button
        JRadioButton falseButton = findRadioButton(panel, FALSE_TEXT);
        assertNotNull(falseButton);
        
        // Simulate clicking the false button
        falseButton.setSelected(true);
        falseButton.getActionListeners()[0].actionPerformed(null);
        
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
