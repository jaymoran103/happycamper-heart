package com.echo.filter.option;

/**
 * Enum for assignment filter options.
 * FUTURE - Add support for more than 3 rounds
 */
public enum AssignmentFilterOption implements FilterOption {
    SHOW_NO_ACTIVITIES("Campers with 0 activities", true),
    SHOW_ONE_ACTIVITY("Campers with 1 activity", true),
    SHOW_TWO_ACTIVITIES("Campers with 2 activities", true),
    SHOW_ALL_ACTIVITIES("Campers with 3 activities", true);
    
    private final String label;
    private final boolean defaultState;
    private final int roundCount;
    
    AssignmentFilterOption(String label, boolean defaultState) {
        this.label = label;
        this.defaultState = defaultState;
        this.roundCount = ordinal(); // Maps enum order to round count (0, 1, 2, 3)
    }
    
    @Override
    public String getLabel() {
        return label;
    }
    
    @Override
    public boolean getDefaultState() {
        return defaultState;
    }
    
    /**
     * Gets the round count associated with this option.
     * 
     * @return The round count (0-3)
     */
    public int getRoundCount() {
        return roundCount;
    }
    
    /**
     * Gets the option for a specific round count.
     * 
     * @param roundCount The round count (0-3)
     * @return The corresponding option, or null if not found
     */
    public static AssignmentFilterOption forRoundCount(int roundCount) {
        for (AssignmentFilterOption option : values()) {
            if (option.getRoundCount() == roundCount) {
                return option;
            }
        }
        return null;
    }
}
