package com.echo.filter;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.RosterHeader;
import com.echo.filter.option.SwimLevelFilterOption;
import com.echo.ui.filter.CollapsibleFilterPanel;
import com.echo.ui.filter.FilterPanelFactory;

/**
 * Filter for swim-compatibility-based filtering.
 * Allows filtering campers by whether they have activities that are compatible with their swim level or not.
 */
public class SwimLevelFilter implements RosterFilter {
    private static final String FILTER_ID = "swimlevel";
    public static final String FILTER_NAME = "Swim Level Compatibility";

    private boolean showCompatibleCampers = true;
    private boolean showIncompatibleCampers = true;

    /**
     * Creates a new SwimLevelFilter.
     * By default, all campers are visible.
     */
    public SwimLevelFilter() {
        // Empty constructor
    }

    @Override
    public boolean apply(Camper camper) {
        //System.out.println("PreferenceFilter.apply: Checking camper " + camper.getId());
        String relevantField = camper.getValue(RosterHeader.SWIMCONFLICTS.standardName);
        if (relevantField == null) {
            return true;
        }
        if (DataConstants.DISPLAY_EMPTY.equals(relevantField) || DataConstants.isEmpty(relevantField)) {
            return showCompatibleCampers;
        }
        return showIncompatibleCampers;
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
     * Sets whether to show campers with unrequested activities.
     *
     * @param show true to show, false to hide
     */
    public void setShowCompatibleCampers(boolean show) {
        this.showCompatibleCampers = show;
    }

    /**
     * Sets whether to show campers without unrequested activities.
     *
     * @param show true to show, false to hide
     */
    public void setShowIncompatibleCampers(boolean show) {
        this.showIncompatibleCampers = show;
    }

    /**
     * Checks if campers with unrequested activities are shown.
     *
     * @return true if shown, false if hidden
     */
    public boolean isShowingCompatibleCampers() {
        return showCompatibleCampers;
    }

    /**
     * Checks if campers without unrequested activities are shown.
     *
     * @return true if shown, false if hidden
     */
    public boolean isShowingIncompatibleCampers() {
        return showIncompatibleCampers;
    }

    @Override
    public CollapsibleFilterPanel createFilterPanel() {
        // Create a map of enum options to their current states
        Map<SwimLevelFilterOption, Boolean> optionStates = new EnumMap<>(SwimLevelFilterOption.class);
        optionStates.put(SwimLevelFilterOption.SHOW_COMPATIBLE, showCompatibleCampers);
        optionStates.put(SwimLevelFilterOption.SHOW_INCOMPATIBLE, showIncompatibleCampers);

        // Create a callback for when options are toggled
        BiConsumer<SwimLevelFilterOption, Boolean> callback = (option, state) -> {
            switch (option) {
                case SHOW_COMPATIBLE -> setShowCompatibleCampers(state);
                case SHOW_INCOMPATIBLE -> setShowIncompatibleCampers(state);
            }
        };

        // Use the factory to create the panel
        return FilterPanelFactory.createEnumPanel(FILTER_NAME, optionStates, callback);
    }
}
