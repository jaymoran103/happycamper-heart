package com.echo.filter;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.echo.domain.Camper;
import com.echo.domain.RosterHeader;
import com.echo.filter.option.AssignmentFilterOption;
import com.echo.ui.filter.CollapsibleFilterPanel;
import com.echo.ui.filter.FilterPanelFactory;

/**
 * Filter for assignment-based filtering.
 * Allows filtering campers by their activity assignment count.
 * FUTURE - Add support for more than 3 rounds
 */
public class AssignmentFilter implements RosterFilter {
    private static final String FILTER_ID = "assignment";
    private static final String FILTER_NAME = "Assignment Filter";
    private static final String ROUNDS_ASSIGNED_HEADER = RosterHeader.ROUND_COUNT.standardName;

    private final Map<Integer, Boolean> roundVisibility = new HashMap<>();

    /**
     * Creates a new AssignmentFilter.
     * By default, all round counts are visible.
     * FUTURE - Add support for more than 3 rounds - base ceiling on max_rounds field, not 3
     */
    public AssignmentFilter() {
        // Initialize round visibility (0-3 rounds)
        for (int i = 0; i <= 3; i++) {
            roundVisibility.put(i, true);
        }
    }

    @Override
    public boolean apply(Camper camper) {
        String value = camper.getValue(ROUNDS_ASSIGNED_HEADER);
        if (value == null) {
            return true; // Always show campers with no rounds assigned value
        }

        try {
            int assignmentCount = Integer.parseInt(value);
            boolean visible = roundVisibility.getOrDefault(assignmentCount, true);
            //System.out.println("AssignmentFilter: Camper " + camper.getId() + " with " + assignmentCount + " rounds is " + (visible ? "visible" : "hidden"));
            return visible;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    @Override
    public String getFilterId() {
        return FILTER_ID;
    }

    @Override
    public String getFilterName() {
        return FILTER_NAME;
    }

    /**
     * Sets the visibility of campers with a specific round count.
     *
     * @param roundCount The round count to set visibility for
     * @param visible true to show campers with this round count, false to hide them
     */
    public void setRoundVisible(int roundCount, boolean visible) {
        //System.out.println("AssignmentFilter.setRoundVisible: Setting round " + roundCount + " visibility to " + visible);
        roundVisibility.put(roundCount, visible);
    }

    /**
     * Checks if campers with a specific round count are visible.
     *
     * @param roundCount The round count to check visibility for
     * @return true if campers with this round count are visible
     */
    public boolean isRoundVisible(int roundCount) {
        return roundVisibility.getOrDefault(roundCount, true);
    }

    @Override
    public CollapsibleFilterPanel createFilterPanel() {
        // Create a map of enum options to their current states
        Map<AssignmentFilterOption, Boolean> optionStates = new EnumMap<>(AssignmentFilterOption.class);

        // Initialize the map with current visibility states
        for (AssignmentFilterOption option : AssignmentFilterOption.values()) {
            int roundCount = option.getRoundCount();
            optionStates.put(option, isRoundVisible(roundCount));
        }

        // Create a callback for when options are toggled
        BiConsumer<AssignmentFilterOption, Boolean> callback = (option, state) -> {
            setRoundVisible(option.getRoundCount(), state);
        };

        // Use the factory to create the panel
        return FilterPanelFactory.createEnumPanel(FILTER_NAME, optionStates, callback);
    }
}
