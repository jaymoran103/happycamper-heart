package com.echo.ui.dialog.selector;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.ui.selector.CheckBoxSelector;

/**
 * Tests for the CheckBoxSelector class.
 */
public class CheckBoxSelectorTest {
    
    private CheckBoxSelector selector;
    private final String TITLE = "Test CheckBox Selector";
    private final Map<String, Boolean> OPTIONS = new LinkedHashMap<>();
    private final boolean REQUIRE_SELECTION = true;
    
    @BeforeEach
    public void setUp() {
        // Initialize options
        OPTIONS.put("Option 1", true);
        OPTIONS.put("Option 2", false);
        OPTIONS.put("Option 3", true);
        
        selector = new CheckBoxSelector(TITLE, OPTIONS, REQUIRE_SELECTION);
    }
    
    @Test
    @DisplayName("Test selector creation with title and options")
    public void testSelectorCreationWithTitleAndOptions() {
        // Create the panel
        JPanel panel = selector.createPanel();
        
        // Verify the panel was created
        assertNotNull(panel);
        
        // Find all checkboxes
        for (String option : OPTIONS.keySet()) {
            JCheckBox checkBox = findCheckBox(panel, option);
            assertNotNull(checkBox, "Checkbox for option '" + option + "' should exist");
            
            // Verify the initial selection state
            assertEquals(OPTIONS.get(option), checkBox.isSelected());
        }
    }
    
    @Test
    @DisplayName("Test getValue and setValue")
    public void testGetValueAndSetValue() {
        // Create the panel to initialize the checkboxes
        selector.createPanel();
        
        // Initial value should match the options
        Map<String, Boolean> value = selector.getValue();
        assertEquals(OPTIONS.size(), value.size());
        for (Map.Entry<String, Boolean> entry : OPTIONS.entrySet()) {
            assertEquals(entry.getValue(), value.get(entry.getKey()));
        }
        
        // Create a new value map
        Map<String, Boolean> newValue = new LinkedHashMap<>();
        newValue.put("Option 1", false);
        newValue.put("Option 2", true);
        newValue.put("Option 3", false);
        
        // Set the new value
        selector.setValue(newValue);
        
        // Verify the value was set
        value = selector.getValue();
        assertEquals(OPTIONS.size(), value.size());
        for (Map.Entry<String, Boolean> entry : newValue.entrySet()) {
            assertEquals(entry.getValue(), value.get(entry.getKey()));
        }
    }
    
    @Test
    @DisplayName("Test hasSelection with required selection")
    public void testHasSelectionWithRequiredSelection() {
        // Create a selector that requires a selection
        CheckBoxSelector requiredSelector = new CheckBoxSelector(TITLE, OPTIONS, true);
        
        // Initially at least one option is selected, so hasSelection should be true
        assertTrue(requiredSelector.hasSelection());
        
        // Create a map with all options unselected
        Map<String, Boolean> allUnselected = new LinkedHashMap<>();
        for (String option : OPTIONS.keySet()) {
            allUnselected.put(option, false);
        }
        
        // Set all options to unselected
        requiredSelector.setValue(allUnselected);
        
        // Now hasSelection should be false
        assertFalse(requiredSelector.hasSelection());
    }
    
    @Test
    @DisplayName("Test hasSelection without required selection")
    public void testHasSelectionWithoutRequiredSelection() {
        // Create a selector that doesn't require a selection
        CheckBoxSelector optionalSelector = new CheckBoxSelector(TITLE, OPTIONS, false);
        
        // hasSelection should always be true
        assertTrue(optionalSelector.hasSelection());
        
        // Create a map with all options unselected
        Map<String, Boolean> allUnselected = new LinkedHashMap<>();
        for (String option : OPTIONS.keySet()) {
            allUnselected.put(option, false);
        }
        
        // Set all options to unselected
        optionalSelector.setValue(allUnselected);
        
        // hasSelection should still be true
        assertTrue(optionalSelector.hasSelection());
    }
    
    @Test
    @DisplayName("Test checkbox interaction")
    public void testCheckboxInteraction() {
        // Create the panel to initialize the checkboxes
        JPanel panel = selector.createPanel();
        
        // Find all checkboxes
        Map<String, JCheckBox> checkBoxes = new LinkedHashMap<>();
        for (String option : OPTIONS.keySet()) {
            JCheckBox checkBox = findCheckBox(panel, option);
            assertNotNull(checkBox);
            checkBoxes.put(option, checkBox);
        }
        
        // Verify initial states
        for (Map.Entry<String, Boolean> entry : OPTIONS.entrySet()) {
            assertEquals(entry.getValue(), checkBoxes.get(entry.getKey()).isSelected());
        }
        
        // Toggle each checkbox
        for (Map.Entry<String, JCheckBox> entry : checkBoxes.entrySet()) {
            JCheckBox checkBox = entry.getValue();
            boolean initialState = checkBox.isSelected();
            
            // Toggle the checkbox
            checkBox.setSelected(!initialState);
            checkBox.getActionListeners()[0].actionPerformed(null);
            
            // Verify the value changed
            Map<String, Boolean> value = selector.getValue();
            assertEquals(!initialState, value.get(entry.getKey()));
        }
    }
    
    @Test
    @DisplayName("Test select all checkbox")
    public void testSelectAllCheckbox() {
        // Create a selector with select all option
        CheckBoxSelector selectAllSelector = new CheckBoxSelector(TITLE, OPTIONS, REQUIRE_SELECTION, true);
        
        // Create the panel to initialize the checkboxes
        JPanel panel = selectAllSelector.createPanel();
        
        // Find the select all checkbox
        JCheckBox selectAllCheckBox = findCheckBox(panel, "(Select All)");
        assertNotNull(selectAllCheckBox);
        
        // Initially the select all checkbox should not be selected
        assertFalse(selectAllCheckBox.isSelected());
        
        // Select the select all checkbox
        selectAllCheckBox.setSelected(true);
        selectAllCheckBox.getActionListeners()[0].actionPerformed(null);
        
        // Verify all options are selected
        Map<String, Boolean> value = selectAllSelector.getValue();
        for (Boolean selected : value.values()) {
            assertTrue(selected);
        }
        
        // Deselect the select all checkbox
        selectAllCheckBox.setSelected(false);
        selectAllCheckBox.getActionListeners()[0].actionPerformed(null);
        
        // Verify all options are deselected
        value = selectAllSelector.getValue();
        for (Boolean selected : value.values()) {
            assertFalse(selected);
        }
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
        
        // Create the panel to initialize the checkboxes
        JPanel panel = selector.createPanel();
        
        // Find a checkbox
        JCheckBox checkBox = findCheckBox(panel, "Option 1");
        assertNotNull(checkBox);
        
        // Toggle the checkbox
        checkBox.setSelected(!checkBox.isSelected());
        checkBox.getActionListeners()[0].actionPerformed(null);
        
        // Verify the callback was called
        assertTrue(callbackCalled[0]);
    }
    
    /**
     * Helper method to find a checkbox with specific text.
     */
    private JCheckBox findCheckBox(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) component;
                if (checkBox.getText().equals(text)) {
                    return checkBox;
                }
            } else if (component instanceof Container) {
                JCheckBox checkBox = findCheckBox((Container) component, text);
                if (checkBox != null) {
                    return checkBox;
                }
            }
        }
        return null;
    }
}
