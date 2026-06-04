package com.echo.filter;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.filter.option.ProgramFilterOption;
import com.echo.ui.filter.CollapsibleFilterPanel;
import com.echo.ui.filter.FilterPanelFactory;

/**
 * Filter for campers, based on number of rounds assigned.
 * Somewhat redundant, but does job of assignmet filter with half the boxes
 * FUTURE - Add support for more than 3 rounds
 */
public class CamperRoundsFilter implements RosterFilter {
    private static final String FILTER_ID = "camper-rounds";
    private static final String FILTER_NAME = "Camper Filter";
    private static final String ROUNDS_ASSIGNED_HEADER = RosterHeader.ROUND_COUNT.standardName;

    private boolean showMissingCampers = true;
    private boolean showCompleteCampers = true;


    @Override
    public boolean apply(Camper camper) {
        // Check camper completion status
        String roundsValue = camper.getValue(ROUNDS_ASSIGNED_HEADER);
        if (roundsValue != null) {
            try {
                int roundCount = Integer.parseInt(roundsValue);
                boolean isComplete = (roundCount == 3);// FUTURE - Add support for more than 3 rounds - base ceiling on max_rounds field, not 3

                if (isComplete && !showCompleteCampers) {
                    return false;
                }

                if (!isComplete && !showMissingCampers) {
                    return false;
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }

        return true;
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
     * Sets whether to show campers missing activities in inconsistent programs.
     *
     * @param show true to show, false to hide
     */
    public void setShowMissingCampers(boolean show) {
        showMissingCampers = show;
    }

    /**
     * Sets whether to show campers with all activities in inconsistent programs.
     *
     * @param show true to show, false to hide
     */
    public void setShowCompleteCampers(boolean show) {
        showCompleteCampers = show;
    }

    /**
     * Checks if campers missing activities in inconsistent programs are shown.
     *
     * @return true if shown, false if hidden
     */
    public boolean isShowingMissingCampers() {
        return showMissingCampers;
    }

    /**
     * Checks if campers with all activities in inconsistent programs are shown.
     *
     * @return true if shown, false if hidden
     */
    public boolean isShowingCompleteCampers() {
        return showCompleteCampers;
    }

    @Override
    public CollapsibleFilterPanel createFilterPanel(EnhancedRoster roster) {
        return createFilterPanel();
    }

    /**
     * Creates a filter panel for this filter.
     */
    @Override
    public CollapsibleFilterPanel createFilterPanel() {
        // Create a map of enum options to their current states
        Map<ProgramFilterOption, Boolean> optionStates = new EnumMap<>(ProgramFilterOption.class);

        // Add camper options
        optionStates.put(ProgramFilterOption.SHOW_MISSING_CAMPERS, showMissingCampers);
        optionStates.put(ProgramFilterOption.SHOW_COMPLETE_CAMPERS, showCompleteCampers);

        // Create a callback for when options are toggled
        BiConsumer<ProgramFilterOption, Boolean> callback = (option, state) -> {
            switch (option) {
                case SHOW_MISSING_CAMPERS -> setShowMissingCampers(state);
                case SHOW_COMPLETE_CAMPERS -> setShowCompleteCampers(state);
            }
        };

        // Create the main filter panel
        return FilterPanelFactory.createEnumPanel(FILTER_NAME, optionStates, callback);
    }


}
