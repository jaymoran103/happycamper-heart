package com.echo.ui.dialog.selector;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.ui.selector.InputSelector;

/**
 * Tests for the InputSelector abstract class.
 * Uses a concrete implementation for testing.
 */
public class InputSelectorTest {
    
    /**
     * Simple concrete implementation of InputSelector for testing.
     */
    private static class TestSelector extends InputSelector<String> {
        private String value;
        private boolean buildCalled = false;
        
        public TestSelector(String title) {
            super(title);
            this.value = "";
        }
        
        @Override
        protected void buildSelectorPanel(JPanel panel) {
            buildCalled = true;
            panel.add(new JLabel("Test Selector"));
        }
        
        @Override
        public String getValue() {
            return value;
        }
        
        @Override
        public void setValue(String value) {
            this.value = value;
            notifyUpdateCallback();
        }
        
        @Override
        public boolean hasSelection() {
            return value != null && !value.isEmpty();
        }
        
        public boolean wasBuildCalled() {
            return buildCalled;
        }
    }
    
    @Test
    @DisplayName("Test selector creation with title")
    public void testSelectorCreationWithTitle() {
        String title = "Test Title";
        TestSelector selector = new TestSelector(title);
        
        // Create the panel to trigger buildSelectorPanel
        JPanel panel = selector.createPanel();
        
        // Verify the panel was created
        assertNotNull(panel);
        
        // Verify buildSelectorPanel was called
        assertTrue(selector.wasBuildCalled());
        
        // Find the title label
        JLabel titleLabel = findTitleLabel(panel);
        assertNotNull(titleLabel);
        assertTrue(titleLabel.getText().contains(title));
    }
    
    @Test
    @DisplayName("Test getValue and setValue")
    public void testGetValueAndSetValue() {
        TestSelector selector = new TestSelector("Test");
        
        // Initial value should be empty
        assertEquals("", selector.getValue());
        
        // Set a new value
        String newValue = "New Value";
        selector.setValue(newValue);
        
        // Verify the value was set
        assertEquals(newValue, selector.getValue());
    }
    
    @Test
    @DisplayName("Test hasSelection")
    public void testHasSelection() {
        TestSelector selector = new TestSelector("Test");
        
        // Initial value is empty, so hasSelection should be false
        assertFalse(selector.hasSelection());
        
        // Set a non-empty value
        selector.setValue("Value");
        
        // Now hasSelection should be true
        assertTrue(selector.hasSelection());
        
        // Set to null
        selector.setValue(null);
        
        // hasSelection should be false again
        assertFalse(selector.hasSelection());
    }
    
    @Test
    @DisplayName("Test update callback")
    public void testUpdateCallback() {
        TestSelector selector = new TestSelector("Test");
        
        // Create a flag to track if the callback was called
        boolean[] callbackCalled = new boolean[1];
        
        // Set the update callback
        selector.setUpdateCallback(() -> callbackCalled[0] = true);
        
        // Initially the callback should not have been called
        assertFalse(callbackCalled[0]);
        
        // Set a value to trigger the callback
        selector.setValue("New Value");
        
        // Verify the callback was called
        assertTrue(callbackCalled[0]);
    }
    
    @Test
    @DisplayName("Test enabled/disabled state")
    public void testEnabledDisabledState() {
        TestSelector selector = new TestSelector("Test");
        
        // Create the panel
        JPanel panel = selector.createPanel();
        
        // Initially the panel should be enabled
        assertTrue(isEnabled(panel));
        
        // Disable the selector
        selector.setEnabled(false);
        
        // Verify the panel is disabled
        assertFalse(isEnabled(panel));
        
        // Enable the selector again
        selector.setEnabled(true);
        
        // Verify the panel is enabled again
        assertTrue(isEnabled(panel));
    }
    
    /**
     * Helper method to find the title label in a panel.
     */
    private JLabel findTitleLabel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                return (JLabel) component;
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
     * Helper method to check if a container and all its components are enabled.
     */
    private boolean isEnabled(Container container) {
        if (!container.isEnabled()) {
            return false;
        }
        
        for (Component component : container.getComponents()) {
            if (!component.isEnabled()) {
                return false;
            }
            
            if (component instanceof Container) {
                if (!isEnabled((Container) component)) {
                    return false;
                }
            }
        }
        
        return true;
    }
}
