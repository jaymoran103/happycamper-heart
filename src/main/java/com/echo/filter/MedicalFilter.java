package com.echo.filter;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.RosterHeader;
import com.echo.filter.option.MedicalFilterOption;
import com.echo.ui.filter.CollapsibleFilterPanel;
import com.echo.ui.filter.FilterPanelFactory;

/**
 * Filter for medical notes-based filtering.
 * Allows filtering campers by whether they have medical notes.
 */
public class MedicalFilter implements RosterFilter {
    private static final String FILTER_ID = "medical";
    public static final String FILTER_NAME = "Medical Notes Filter";

    private boolean showCampersWithMedicalNotes = true;
    private boolean showCampersWithoutMedicalNotes = true;

    /**
     * Creates a new MedicalFilter.
     * By default, all campers are visible.
     */
    public MedicalFilter() {
        // Empty constructor
    }

    @Override
    public boolean apply(Camper camper) {
        String medicalNotes = camper.getValue(RosterHeader.MEDICAL_NOTES.standardName);
        if (medicalNotes == null) {
            return true; // Always show campers with no medical notes field
        }
        
        if (DataConstants.isEmpty(medicalNotes)) {
            return showCampersWithoutMedicalNotes;
        }
        return showCampersWithMedicalNotes;
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
     * Sets whether to show campers with medical notes.
     *
     * @param show Whether to show campers with medical notes
     */
    public void setShowCampersWithMedicalNotes(boolean show) {
        this.showCampersWithMedicalNotes = show;
    }

    /**
     * Gets whether campers with medical notes are shown.
     *
     * @return Whether campers with medical notes are shown
     */
    public boolean isShowCampersWithMedicalNotes() {
        return showCampersWithMedicalNotes;
    }

    /**
     * Sets whether to show campers without medical notes.
     *
     * @param show Whether to show campers without medical notes
     */
    public void setShowCampersWithoutMedicalNotes(boolean show) {
        this.showCampersWithoutMedicalNotes = show;
    }

    /**
     * Gets whether campers without medical notes are shown.
     *
     * @return Whether campers without medical notes are shown
     */
    public boolean isShowCampersWithoutMedicalNotes() {
        return showCampersWithoutMedicalNotes;
    }

    @Override
    public CollapsibleFilterPanel createFilterPanel() {
        // Create a map of enum options to their current states
        Map<MedicalFilterOption, Boolean> optionStates = new EnumMap<>(MedicalFilterOption.class);
        optionStates.put(MedicalFilterOption.SHOW_WITH_MEDICAL_NOTES, showCampersWithMedicalNotes);
        optionStates.put(MedicalFilterOption.SHOW_WITHOUT_MEDICAL_NOTES, showCampersWithoutMedicalNotes);

        // Create a callback for when options are toggled
        BiConsumer<MedicalFilterOption, Boolean> callback = (option, state) -> {
            switch (option) {
                case SHOW_WITH_MEDICAL_NOTES -> setShowCampersWithMedicalNotes(state);
                case SHOW_WITHOUT_MEDICAL_NOTES -> setShowCampersWithoutMedicalNotes(state);
            }
        };

        // Use the factory to create the panel
        return FilterPanelFactory.createEnumPanel(FILTER_NAME, optionStates, callback);
    }
}
