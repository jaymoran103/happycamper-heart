package com.echo.filter;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.ui.filter.CollapsibleFilterPanel;
import com.echo.ui.filter.ProgramFilterBuilder;

/**
 * Filter for program-specific visibility.
 * This filter organizes programs by their round count and allows toggling visibility
 * for individual programs, with section headers that can toggle all programs in a section.
 */
public class SortedProgramFilter implements RosterFilter {

    // Constants
    public static final String FILTER_NAME = "Programs Filter";
    private static final String FILTER_ID = "program-list";

    // Data model
    private final Map<String, Boolean> programVisibility = new HashMap<>();
    private EnhancedRoster roster;

    // UI component references - these are populated by the ProgramFilterBuilder
    private final Map<Integer, Map<String, JCheckBox>> programCheckboxesByRoundCount = new HashMap<>();
    private final Map<Integer, JCheckBox> sectionHeaderCheckboxes = new HashMap<>();

    /**
     * Creates a new program list filter.
     */
    public SortedProgramFilter() {
        // Default constructor
    }

    @Override
    public boolean apply(Camper camper) {
        String program = camper.getValue(RosterHeader.PROGRAM.standardName);

        // If the program is not in our map, default to visible
        if (!programVisibility.containsKey(program)) {
            programVisibility.put(program, true);
            return true;
        }

        return programVisibility.get(program);
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
    public CollapsibleFilterPanel createFilterPanel(EnhancedRoster roster) {
        this.roster = roster;
        return createFilterPanel();
    }

    @Override
    public CollapsibleFilterPanel createFilterPanel() {
        // Delegate panel creation to the ProgramFilterBuilder
        return ProgramFilterBuilder.createFilterPanel(
            this,
            roster,
            FILTER_NAME,
            programCheckboxesByRoundCount,
            sectionHeaderCheckboxes,
            () -> {} // Empty callback since FilterManager.updateTable is called directly. FUTURE - look for more elegant way to do this
        );
    }

    /**
     * Sets the visibility of a program.
     *
     * @param program The program to set visibility for
     * @param visible Whether the program should be visible
     */
    public void setProgramVisible(String program, boolean visible) {
        programVisibility.put(program, visible);
    }

    /**
     * Gets the visibility of a program.
     *
     * @param program The program to get visibility for
     * @return Whether the program is visible
     */
    public boolean isProgramVisible(String program) {
        return programVisibility.getOrDefault(program, true);
    }

    /**
     * Sets whether a program is enabled (visible).
     * This method is used by the ProgramFilterBuilder.
     *
     * @param program The program to set
     * @param enabled Whether the program should be enabled
     */
    public void setProgramEnabled(String program, boolean enabled) {
        setProgramVisible(program, enabled);
    }

    /**
     * Checks if a program is enabled (visible).
     * This method is used by the ProgramFilterBuilder.
     *
     * @param program The program to check
     * @return Whether the program is enabled
     */
    public boolean isProgramEnabled(String program) {
        return isProgramVisible(program);
    }


}