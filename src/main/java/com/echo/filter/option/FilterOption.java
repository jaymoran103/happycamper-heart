package com.echo.filter.option;

/**
 * Interface for filter option enums.
 * All filter option enums should implement this interface.
 * 
 * Facilitates consistent building of filter panels
 */
public interface FilterOption {
    /**
     * Gets the display label for this option.
     * 
     * @return The label to display in the UI
     */
    String getLabel();
    
    /**
     * Gets the default state for this option.
     * 
     * @return true if the option should be enabled by default
     */
    boolean getDefaultState();
}
