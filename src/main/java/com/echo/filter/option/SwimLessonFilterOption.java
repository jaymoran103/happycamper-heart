package com.echo.filter.option;

/**
 * Enum for swim lesson validity filter options.
 */
public enum SwimLessonFilterOption implements FilterOption {
    SHOW_VALID("Campers with valid swim lessons", true),
    SHOW_FLAGGED("Campers with swim lesson mismatches", true);

    private final String label;
    private final boolean defaultState;

    SwimLessonFilterOption(String label, boolean defaultState) {
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
