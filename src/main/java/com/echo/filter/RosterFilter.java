package com.echo.filter;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.ui.filter.CollapsibleFilterPanel;

/**
 * Interface for roster filters.
 * Defines methods that all filters must implement.
 */
public interface RosterFilter {
    /**
     * Applies the filter to a camper.
     *
     * @param camper The camper to filter
     * @return true if the camper passes the filter, false otherwise
     */
    boolean apply(Camper camper);

    /**
     * Gets the unique identifier for this filter.
     *
     * @return The filter ID
     */
    String getFilterId();

    /**
     * Gets the display name for this filter.
     *
     * @return The filter display name
     */
    String getFilterName();

    /**
     * Creates a UI panel for this filter.
     * Default implementation, delegates to the no-argument createFilterPanel method.
     * Implementations that expect EnhancedRoster as an argument should set it as a field themselves
     * @param roster The roster to filter
     * @return A panel for configuring this filter
     */
    default CollapsibleFilterPanel createFilterPanel(EnhancedRoster roster) {
        return createFilterPanel();
    }

    /**
     * Creates a UI panel for this filter without requiring a roster.
     * This method should be implemented by all filters to create their UI.
     *
     * @return A panel for configuring this filter
     */
    CollapsibleFilterPanel createFilterPanel();
}
