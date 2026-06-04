package com.echo.filter.option;

/**
 * Enum for medical filter options.
 */
public enum MedicalFilterOption implements FilterOption {
    SHOW_WITH_MEDICAL_NOTES("Campers with medical notes", true),
    SHOW_WITHOUT_MEDICAL_NOTES("Campers without medical notes", true);
    
    private final String label;
    private final boolean defaultState;
    
    MedicalFilterOption(String label, boolean defaultState) {
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
