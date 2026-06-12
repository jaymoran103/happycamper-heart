package com.echo.filter.option;

/**
 * Enum for program filter options.
 */
public enum ProgramFilterOption implements FilterOption {


    // Camper options for inconsistent programs
    SHOW_MISSING_CAMPERS("Missing assignments", true),
    SHOW_COMPLETE_CAMPERS("Complete assignments", true);
    
    private final String label;
    private final boolean defaultState;
    
    ProgramFilterOption(String label, boolean defaultState) {
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
