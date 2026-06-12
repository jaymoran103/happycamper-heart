package com.echo.filter.option;

/**
 * Enum for swim lesson filter options.
 */
public enum SwimLessonFilterOption implements FilterOption {
    SHOW_VALID("Swim lessons valid", true),
    SHOW_FLAGGED("Swim lessons mismatch", true);

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
