package com.echo.filter;

import java.awt.Component;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JSplitPane;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.ui.component.RosterTable;

/**
 * Manager for roster filters.
 * Handles applying multiple filters to campers.
 */
public class FilterManager {
    // LinkedHashMap so iteration (and thus sidebar display order) follows insertion order below.
    private final Map<String, RosterFilter> filters = new LinkedHashMap<>();
    private EnhancedRoster roster;

    /**
     * Adds a filter to the manager.
     *
     * @param filter The filter to add
     */
    public void addFilter(RosterFilter filter) {
        //System.out.println("FilterManager.addFilter: Adding filter " + filter.getFilterId() + " (" + filter.getFilterName() + ")");
        filters.put(filter.getFilterId(), filter);
        //System.out.println("FilterManager.addFilter: Filter count is now " + filters.size());
    }

    /**
     * Removes a filter from the manager.
     *
     * @param filterId The ID of the filter to remove
     */
    public void removeFilter(String filterId) {
        filters.remove(filterId);
    }

    /**
     * Gets a filter by ID.
     *
     * @param filterId The ID of the filter to get
     * @return The filter, or null if not found
     */
    public RosterFilter getFilter(String filterId) {
        return filters.get(filterId);
    }

    /**
     * Applies all filters to a camper.
     *
     * @param camper The camper to filter
     * @return true if the camper passes all filters, false otherwise
     */
    public boolean applyFilters(Camper camper) {
        //System.out.println("FilterManager.applyFilters: Applying " + filters.size() + " filters to camper " + camper.getId());

        // If there are no filters, always show the camper
        if (filters.isEmpty()) {
            //System.out.println("FilterManager.applyFilters: No filters, showing camper");
            return true;
        }

        // A camper passes if it passes ALL filters
        for (RosterFilter filter : filters.values()) {
            //System.out.println("FilterManager.applyFilters: Applying filter " + filter.getFilterId());
            boolean passes = filter.apply(camper);
            //System.out.println("FilterManager.applyFilters: Filter " + filter.getFilterId() + " returned " + passes);
            if (!passes) {
                //System.out.println("FilterManager.applyFilters: Camper " + camper.getId() + " failed filter " + filter.getFilterId());
                return false;
            }
        }

        //System.out.println("FilterManager.applyFilters: Camper " + camper.getId() + " passed all filters");
        return true;
    }

    /**
     * Checks if a filter is registered.
     *
     * @param filterId The ID of the filter to check
     * @return true if the filter is registered
     */
    public boolean hasFilter(String filterId) {
        return filters.containsKey(filterId);
    }

    /**
     * Gets the number of registered filters.
     *
     * @return The number of filters
     */
    public int getFilterCount() {
        return filters.size();
    }

    /**
     * Gets all registered filters.
     *
     * @return Collection of all filters
     */
    public Collection<RosterFilter> getAllFilters() {
        return filters.values();
    }

    /**
     * Sets the roster for this filter manager.
     *
     * @param roster The roster to filter
     */
    public void setRoster(EnhancedRoster roster) {
        this.roster = roster;
    }

    /**
     * Gets the roster for this filter manager.
     *
     * @return The roster
     */
    public EnhancedRoster getRoster() {
        return roster;
    }

    /**
     * Creates filters for enabled features in the roster.
     *
     * @param roster The roster to create filters for
     */
    public void createFiltersForRoster(EnhancedRoster roster) {
        //System.out.println("FilterManager.createFiltersForRoster: Creating filters for roster");
        setRoster(roster);
        filters.clear();

        // Create filters based on enabled features

        // Always add the assignment filter to show basic round counts
        addFilter(new AssignmentFilter());

        // B1: universal search is always-on (the only non-feature-gated filter; see docs/sprint-conventions.md)
        addFilter(new TextSearchFilter());

        // Sidebar display order follows insertion order (LinkedHashMap). The two swim filters sit
        // between the general filters and the Activity Selector, which is pinned to the bottom.
        if (roster.hasFeature("program")){
            addFilter(new SortedProgramFilter());
            // addFilter(new CamperRoundsFilter()); Disabling, redundant now
        }
        if (roster.hasFeature("preference")) {
            addFilter(new PreferenceFilter());
        }
        if (roster.hasFeature("swimlevel")) {
            // C1: one feature gate produces two independent filters/columns
            addFilter(new SwimLevelFilter());
            addFilter(new SwimLessonFilter());
        }
        if (roster.hasFeature("activity")) {
            addFilter(new ActivityFilter());
        }


    }

    /**
     * Updates the table to reflect filter changes.
     * This should be called whenever a filter is modified.
     *
     * @param component Any component in the UI hierarchy
     */
    public static void updateTable(Component component) {
        RosterTable table = findRosterTable(component);
        if (table != null) {
            table.applyFilters();
        }
    }

    /**
     * Locates the RosterTable in the same window as the given component.
     *
     * @param component any component in the UI hierarchy
     * @return the RosterTable, or null if not found
     */
    public static RosterTable findRosterTable(Component component) {
        // Find the root window
        Component root = component;
        while (root != null && !(root instanceof JFrame)) {
            root = root.getParent();
        }

        if (root instanceof JFrame frame) {
            for (Component c : frame.getContentPane().getComponents()) {
                if (c instanceof JSplitPane splitPane
                        && splitPane.getRightComponent() instanceof RosterTable rosterTable) {
                    return rosterTable;
                }
            }
        }
        return null;
    }
}
