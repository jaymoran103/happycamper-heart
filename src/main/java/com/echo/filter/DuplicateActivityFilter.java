package com.echo.filter;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.RosterHeader;
import com.echo.filter.option.DuplicateActivityFilterOption;
import com.echo.ui.filter.CollapsibleFilterPanel;
import com.echo.ui.filter.FilterPanelFactory;

public class DuplicateActivityFilter implements RosterFilter {

    private static final String FILTER_ID = "duplicateactivity";
    public static final String FILTER_NAME = "Duplicate Activities";

    private boolean showValidCampers = true;
    private boolean showFlaggedCampers = true;

    @Override
    public boolean apply(Camper camper) {
        String value = camper.getValue(RosterHeader.DUPLICATE_ACTIVITY.standardName);
        if (value == null) {
            return true;
        }
        if (DataConstants.DISPLAY_EMPTY.equals(value) || DataConstants.isEmpty(value)) {
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

    @Override
    public CollapsibleFilterPanel createFilterPanel() {
        Map<DuplicateActivityFilterOption, Boolean> optionStates = new EnumMap<>(DuplicateActivityFilterOption.class);
        optionStates.put(DuplicateActivityFilterOption.SHOW_VALID, showValidCampers);
        optionStates.put(DuplicateActivityFilterOption.SHOW_FLAGGED, showFlaggedCampers);

        BiConsumer<DuplicateActivityFilterOption, Boolean> callback = (option, state) -> {
            switch (option) {
                case SHOW_VALID -> showValidCampers = state;
                case SHOW_FLAGGED -> showFlaggedCampers = state;
            }
        };

        return FilterPanelFactory.createEnumPanel(FILTER_NAME, optionStates, callback);
    }
}
