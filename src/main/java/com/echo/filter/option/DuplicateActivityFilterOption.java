package com.echo.filter.option;

public enum DuplicateActivityFilterOption implements FilterOption {
    SHOW_VALID("No duplicates", true),
    SHOW_FLAGGED("Has duplicates", true);

    private final String label;
    private final boolean defaultState;

    DuplicateActivityFilterOption(String label, boolean defaultState) {
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
