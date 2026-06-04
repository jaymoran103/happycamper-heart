package com.echo.filter;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.RosterHeader;
import com.echo.filter.option.PreferenceFilterOption;
import com.echo.ui.filter.CollapsibleFilterPanel;
import com.echo.ui.filter.FilterPanelFactory;

/**
 * Filter for preference-based filtering.
 * Allows filtering campers by whether they have unrequested activity assignments.
 */
public class PreferenceFilter implements RosterFilter {
    private static final String FILTER_ID = "preference";
    public static final String FILTER_NAME = "Preference Filter";

    private boolean showCampersWithUnrequestedActivities = true;
    private boolean showCampersWithoutUnrequestedActivities = true;

    /**
     * Creates a new PreferenceFilter.
     * By default, all campers are visible.
     */
    public PreferenceFilter() {
        // Empty constructor
    }

    @Override
    public boolean apply(Camper camper) {
        //System.out.println("PreferenceFilter.apply: Checking camper " + camper.getId());
        String relevantField = camper.getValue(RosterHeader.UNREQUESTED_ACTIVITIES.standardName);
        if (relevantField == null) {
            return true;
        }
        if (DataConstants.DISPLAY_EMPTY.equals(relevantField) || DataConstants.isEmpty(relevantField)) {
            return showCampersWithoutUnrequestedActivities;
        }
        return showCampersWithUnrequestedActivities;
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
    public void setShowCampersWithUnrequestedActivities(boolean show) {
        this.showCampersWithUnrequestedActivities = show;
    }

    /**
     * Sets whether to show campers without unrequested activities.
     *
     * @param show true to show, false to hide
     */
    public void setShowCampersWithoutUnrequestedActivities(boolean show) {
        this.showCampersWithoutUnrequestedActivities = show;
    }

    /**
     * Checks if campers with unrequested activities are shown.
     *
     * @return true if shown, false if hidden
     */
    public boolean isShowCampersWithUnrequestedActivities() {
        return showCampersWithUnrequestedActivities;
    }

    /**
     * Checks if campers without unrequested activities are shown.
     *
     * @return true if shown, false if hidden
     */
    public boolean isShowCampersWithoutUnrequestedActivities() {
        return showCampersWithoutUnrequestedActivities;
    }

    @Override
    public CollapsibleFilterPanel createFilterPanel() {
        // Create a map of enum options to their current states
        Map<PreferenceFilterOption, Boolean> optionStates = new EnumMap<>(PreferenceFilterOption.class);
        optionStates.put(PreferenceFilterOption.SHOW_WITH_UNREQUESTED, showCampersWithUnrequestedActivities);
        optionStates.put(PreferenceFilterOption.SHOW_WITHOUT_UNREQUESTED, showCampersWithoutUnrequestedActivities);

        // Create a callback for when options are toggled
        BiConsumer<PreferenceFilterOption, Boolean> callback = (option, state) -> {
            switch (option) {
                case SHOW_WITH_UNREQUESTED -> setShowCampersWithUnrequestedActivities(state);
                case SHOW_WITHOUT_UNREQUESTED -> setShowCampersWithoutUnrequestedActivities(state);
            }
        };

        // Use the factory to create the panel
        return FilterPanelFactory.createEnumPanel(FILTER_NAME, optionStates, callback);
    }
}
