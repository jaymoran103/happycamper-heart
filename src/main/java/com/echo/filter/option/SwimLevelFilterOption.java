package com.echo.filter.option;

/**
 * Enum for swim level filter options.
 */
public enum SwimLevelFilterOption implements FilterOption {
    SHOW_COMPATIBLE("Valid swim level", true),
    SHOW_INCOMPATIBLE("Invalid swim level", true);
    
    private final String label;
    private final boolean defaultState;
    
    SwimLevelFilterOption(String label, boolean defaultState) {
        this.label = label;
        this.defaultState = defaultState;
    }
    
    @Override
    public String getLabel() {
        return label;
    }
    
    @Override
    public boolean getDefaultState() {
        return defaultState;
    }
}
