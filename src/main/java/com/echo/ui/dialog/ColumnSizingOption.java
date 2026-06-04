package com.echo.ui.dialog;

/**
 * Enum representing the available column sizing options.
 * This provides a type-safe way to handle column sizing options throughout the application.
 */
public enum ColumnSizingOption {
    AUTO_SIZE("Auto-size"),
    EQUAL_WIDTH("Equal Width"),
    CUSTOM_WIDTH("Custom Width");
    
    private final String displayName;
    
    /**
     * Creates a new ColumnSizingOption with the specified display name.
     * 
     * @param displayName The display name for this option
     */
    ColumnSizingOption(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Gets the display name for this option.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the ColumnSizingOption from its display name.
     * 
     * @param displayName The display name to look up
     * @return The corresponding ColumnSizingOption, or null if not found
     */
    public static ColumnSizingOption fromDisplayName(String displayName) {
        for (ColumnSizingOption option : values()) {
            if (option.displayName.equals(displayName)) {
                return option;
            }
        }
        return null;
    }
    
    /**
     * Gets an array of all display names.
     * 
     * @return Array of display names
     */
    public static String[] getAllDisplayNames() {
        ColumnSizingOption[] options = values();
        String[] displayNames = new String[options.length];
        
        for (int i = 0; i < options.length; i++) {
            displayNames[i] = options[i].displayName;
        }
        
        return displayNames;
    }
}
