package com.echo.filter;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.RosterHeader;
import com.echo.filter.option.SwimLessonFilterOption;
import com.echo.ui.filter.CollapsibleFilterPanel;
import com.echo.ui.filter.FilterPanelFactory;

/**
 * Filter for swim-lesson validity (C1).
 *
 * Sibling to {@link SwimLevelFilter}: both are produced from the single {@code swimlevel} feature gate
 * but toggle independently. This one keys on the {@link RosterHeader#SWIMLESSON} column — empty means a
 * valid lesson assignment, any value means a flagged mismatch.
 */
public class SwimLessonFilter implements RosterFilter {
    private static final String FILTER_ID = "swimlesson";
    public static final String FILTER_NAME = "Swim Lesson Validity";

    private boolean showValidCampers = true;
    private boolean showFlaggedCampers = true;

    public SwimLessonFilter() {
        // Empty constructor
    }

    @Override
    public boolean apply(Camper camper) {
        String relevantField = camper.getValue(RosterHeader.SWIMLESSON.standardName);
        if (relevantField == null) {
            return true;
        }
        if (DataConstants.DISPLAY_EMPTY.equals(relevantField) || DataConstants.isEmpty(relevantField)) {
            return showValidCampers;
        }
        return showFlaggedCampers;
    }

    @Override
    public String getFilterId() {
        return FILTER_ID;
    }

    @Override
    public String getFilterName() {
        return FILTER_NAME;
    }

    public void setShowValidCampers(boolean show) {
        this.showValidCampers = show;
    }

    public void setShowFlaggedCampers(boolean show) {
        this.showFlaggedCampers = show;
    }

    public boolean isShowingValidCampers() {
        return showValidCampers;
    }

    public boolean isShowingFlaggedCampers() {
        return showFlaggedCampers;
    }

    @Override
    public CollapsibleFilterPanel createFilterPanel() {
        Map<SwimLessonFilterOption, Boolean> optionStates = new EnumMap<>(SwimLessonFilterOption.class);
        optionStates.put(SwimLessonFilterOption.SHOW_VALID, showValidCampers);
        optionStates.put(SwimLessonFilterOption.SHOW_FLAGGED, showFlaggedCampers);

        BiConsumer<SwimLessonFilterOption, Boolean> callback = (option, state) -> {
            switch (option) {
                case SHOW_VALID -> setShowValidCampers(state);
                case SHOW_FLAGGED -> setShowFlaggedCampers(state);
            }
        };

        return FilterPanelFactory.createEnumPanel(FILTER_NAME, optionStates, callback);
    }
}
