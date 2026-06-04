package com.echo.ui.dialog.selector;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.ui.selector.ActionButtonSelector;

/**
 * Tests for the ActionButtonSelector class.
 */
public class ActionButtonSelectorTest {
    
    private ActionButtonSelector selector;
    private final String TITLE = "Test Action Button Selector";
    private final String[] BUTTON_LABELS = {"Button 1", "Button 2", "Button 3"};
    private final boolean[] BUTTON_ACTIONS_CALLED = {false, false, false};
    private final Runnable[] BUTTON_ACTIONS = {
        () -> BUTTON_ACTIONS_CALLED[0] = true,
        () -> BUTTON_ACTIONS_CALLED[1] = true,
        () -> BUTTON_ACTIONS_CALLED[2] = true
    };
    
    @BeforeEach
    public void setUp() {
        selector = new ActionButtonSelector(TITLE, BUTTON_LABELS, BUTTON_ACTIONS);
        // Reset action called flags
        for (int i = 0; i < BUTTON_ACTIONS_CALLED.length; i++) {
            BUTTON_ACTIONS_CALLED[i] = false;
        }
    }
    
    @Test
    @DisplayName("Test selector creation with title")
    public void testSelectorCreationWithTitle() {
        // Create the panel
        JPanel panel = selector.createPanel();
        
        // Verify the panel was created
        assertNotNull(panel);
        
        // Find all buttons
        for (String label : BUTTON_LABELS) {
            JButton button = findButton(panel, label);
            assertNotNull(button, "Button with label '" + label + "' should exist");
        }
    }
    
    @Test
    @DisplayName("Test button actions")
    public void testButtonActions() {
        // Create the panel to initialize the buttons
        JPanel panel = selector.createPanel();
        
        // Find all buttons
        JButton[] buttons = new JButton[BUTTON_LABELS.length];
        for (int i = 0; i < BUTTON_LABELS.length; i++) {
            buttons[i] = findButton(panel, BUTTON_LABELS[i]);
            assertNotNull(buttons[i], "Button with label '" + BUTTON_LABELS[i] + "' should exist");
        }
        
        // Initially no actions should have been called
        for (boolean called : BUTTON_ACTIONS_CALLED) {
            assertFalse(called);
        }
        
        // Simulate clicking each button
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].doClick();
            
            // Verify only the corresponding action was called
            for (int j = 0; j < BUTTON_ACTIONS_CALLED.length; j++) {
                assertEquals(i == j, BUTTON_ACTIONS_CALLED[j], 
                    "Action " + j + " should " + (i == j ? "" : "not ") + "be called when button " + i + " is clicked");
            }
            
            // Reset for next test
            BUTTON_ACTIONS_CALLED[i] = false;
        }
    }
    
    @Test
    @DisplayName("Test getValue and setValue")
    public void testGetValueAndSetValue() {
        // ActionButtonSelector doesn't store a value, so getValue should always return null
        assertNull(selector.getValue());
        
        // setValue should do nothing
        selector.setValue(null);
        assertNull(selector.getValue());
    }
    
    @Test
    @DisplayName("Test hasSelection")
    public void testHasSelection() {
        // ActionButtonSelector doesn't have a selection, so hasSelection should always return true
        assertTrue(selector.hasSelection());
    }
    
    @Test
    @DisplayName("Test enabled/disabled state")
    public void testEnabledDisabledState() {
        // Create the panel
        JPanel panel = selector.createPanel();
        
        // Find all buttons
        JButton[] buttons = new JButton[BUTTON_LABELS.length];
        for (int i = 0; i < BUTTON_LABELS.length; i++) {
            buttons[i] = findButton(panel, BUTTON_LABELS[i]);
            assertNotNull(buttons[i]);
            
            // Initially all buttons should be enabled
            assertTrue(buttons[i].isEnabled());
        }
        
        // Disable the selector
        selector.setEnabled(false);
        
        // Verify all buttons are disabled
        for (JButton button : buttons) {
            assertFalse(button.isEnabled());
        }
        
        // Enable the selector again
        selector.setEnabled(true);
        
        // Verify all buttons are enabled again
        for (JButton button : buttons) {
            assertTrue(button.isEnabled());
        }
    }
    
    /**
     * Helper method to find a button with specific text.
     */
    private JButton findButton(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;
                if (button.getText().equals(text)) {
                    return button;
                }
            } else if (component instanceof Container) {
                JButton button = findButton((Container) component, text);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }
}
