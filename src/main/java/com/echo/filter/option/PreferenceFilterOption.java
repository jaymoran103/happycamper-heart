package com.echo.filter.option;

/**
 * Enum for preference filter options.
 */
public enum PreferenceFilterOption implements FilterOption {
    SHOW_WITHOUT_UNREQUESTED("Campers with requests met", true),
    SHOW_WITH_UNREQUESTED("Campers with unrequested activities", true);
    
    private final String label;
    private final boolean defaultState;
    
    PreferenceFilterOption(String label, boolean defaultState) {
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
