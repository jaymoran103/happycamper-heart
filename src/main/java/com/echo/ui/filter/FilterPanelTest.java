package com.echo.ui.filter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Simple test class to verify that the FilterPanelFactory works correctly.
 */
public class FilterPanelTest {
    
    public static void main(String[] args) {
        // Create a simple boolean options panel
        Map<String, Boolean> options = new LinkedHashMap<>();
        Map<String, Consumer<Boolean>> callbacks = new LinkedHashMap<>();
        
        options.put("Option 1", true);
        callbacks.put("Option 1", (value) -> System.out.println("Option 1: " + value));
        
        options.put("Option 2", false);
        callbacks.put("Option 2", (value) -> System.out.println("Option 2: " + value));
        
        CollapsibleFilterPanel panel = FilterPanelFactory.createBooleanOptionsPanel(
            "Test Panel", options, callbacks);
        
        System.out.println("Created panel: " + panel);
        System.out.println("Test completed successfully!");
    }
}
