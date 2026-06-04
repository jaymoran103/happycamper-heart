package com.echo.ui.filter;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.echo.domain.EnhancedRoster;
import com.echo.feature.ProgramFeature;
import com.echo.filter.FilterManager;
import com.echo.filter.SortedProgramFilter;
import com.echo.ui.elements.HoverCheckBox;

/**
 * Builder class for creating program filter UI components.
 * This class separates UI construction logic from filter business logic.
 */
public class ProgramFilterBuilder {

    private static final int SECTION_SPACER_HEIGHT = 10;
    private static final int SECTION_INDENT_WIDTH = 15;
    private static final int CONTENT_PADDING = 2;

    /**
     * Creates a filter panel for the SortedProgramFilter.
     *
     * @param filter The SortedProgramFilter instance
     * @param roster The roster containing program data
     * @param filterName The name of the filter
     * @param programCheckboxesByRoundCount Map to store checkboxes by round count
     * @param sectionHeaderCheckboxes Map to store section header checkboxes
     * @param updateCallback Callback to invoke when filter settings change
     * @return A CollapsibleFilterPanel containing program filter controls
     */
    public static CollapsibleFilterPanel createFilterPanel(
            SortedProgramFilter filter,
            EnhancedRoster roster,
            String filterName,
            Map<Integer, Map<String, JCheckBox>> programCheckboxesByRoundCount,
            Map<Integer, JCheckBox> sectionHeaderCheckboxes,
            Runnable updateCallback) {

        // Clear existing UI component references
        programCheckboxesByRoundCount.clear();
        sectionHeaderCheckboxes.clear();

        // Create the main filter panel
        CollapsibleFilterPanel filterPanel = new CollapsibleFilterPanel(filterName);

        // Create content panel with vertical layout that aligns components to the top
        JPanel contentPanel = createContentPanel();

        if (roster != null) {
            // Get and sort programs by round count
            Map<Integer, List<String>> programsByRoundCount = ProgramFeature.getProgramsByRoundCount(roster);
            List<Integer> sortedRoundCounts = getSortedRoundCounts(programsByRoundCount);

            // Add program sections to the panel
            addProgramSections(contentPanel, programsByRoundCount, sortedRoundCounts,
                              programCheckboxesByRoundCount, sectionHeaderCheckboxes,
                              filter, updateCallback);
        }

        filterPanel.addContent(contentPanel);
        return filterPanel;
    }

    /**
     * Creates a content panel with vertical layout.
     *
     * @return A JPanel with vertical BoxLayout
     */
    private static JPanel createContentPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        // contentPanel.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);
        contentPanel.setBackground(null);
        
        contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.setBorder(new EmptyBorder(CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING));
        return contentPanel;
    }


    /**
     * Adds program sections to the content panel.
     *
     * @param contentPanel The panel to add sections to
     * @param programsByRoundCount Map of programs grouped by round count
     * @param sortedRoundCounts Sorted list of round counts
     * @param programCheckboxesByRoundCount Map to store program checkboxes
     * @param sectionHeaderCheckboxes Map to store section header checkboxes
     * @param filter The SortedProgramFilter instance
     * @param updateCallback Callback to invoke when filter settings change
     */
    private static void addProgramSections(
            JPanel contentPanel,
            Map<Integer, List<String>> programsByRoundCount,
            List<Integer> sortedRoundCounts,
            Map<Integer, Map<String, JCheckBox>> programCheckboxesByRoundCount,
            Map<Integer, JCheckBox> sectionHeaderCheckboxes,
            SortedProgramFilter filter,
            Runnable updateCallback) {

        // Add sections for each round count
        for (Integer roundCount : sortedRoundCounts) {
            List<String> programs = programsByRoundCount.get(roundCount);
            if (programs != null && !programs.isEmpty()) {
                addProgramSection(contentPanel, roundCount, programs,
                                 programCheckboxesByRoundCount, sectionHeaderCheckboxes,
                                 filter, updateCallback);
                contentPanel.add(Box.createRigidArea(new Dimension(0, SECTION_SPACER_HEIGHT
                )));
            }

        }
    }

    /**
     * Adds a section for programs with a specific round count.
     *
     * @param contentPanel The panel to add the section to
     * @param roundCount The round count for this section
     * @param programs List of programs in this section
     * @param programCheckboxesByRoundCount Map to store program checkboxes
     * @param sectionHeaderCheckboxes Map to store section header checkboxes
     * @param filter The SortedProgramFilter instance
     * @param updateCallback Callback to invoke when filter settings change
     */
    private static void addProgramSection(
            JPanel contentPanel,
            Integer roundCount,
            List<String> programs,
            Map<Integer, Map<String, JCheckBox>> programCheckboxesByRoundCount,
            Map<Integer, JCheckBox> sectionHeaderCheckboxes,
            SortedProgramFilter filter,
            Runnable updateCallback) {

        // Create section header
        String sectionTitle = getSectionName(roundCount);
        JCheckBox sectionCheckbox = createSectionHeader(contentPanel, sectionTitle);
        sectionHeaderCheckboxes.put(roundCount, sectionCheckbox);

        // Create program panel for this section
        JPanel programPanel = new JPanel();
        programPanel.setBackground(null);
        programPanel.setLayout(new BoxLayout(programPanel, BoxLayout.Y_AXIS));
        programPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        programPanel.setBorder(BorderFactory.createEmptyBorder(0, SECTION_INDENT_WIDTH, 0, 0));

        // Create a map to store checkboxes for this round count
        Map<String, JCheckBox> checkboxMap = new HashMap<>();
        programCheckboxesByRoundCount.put(roundCount, checkboxMap);

        // Add checkboxes for each program
        for (String program : programs) {
            JCheckBox checkbox = new HoverCheckBox(program);
            checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Set initial state based on filter
            checkbox.setSelected(filter.isProgramEnabled(program));

            // Add listener to update filter when checkbox state changes
            checkbox.addItemListener(e -> {
                filter.setProgramEnabled(program, e.getStateChange() == ItemEvent.SELECTED);
                updateSectionHeaderState(sectionCheckbox, checkboxMap);
                // Notify FilterManager to update the table
                FilterManager.updateTable(checkbox);
                if (updateCallback != null) {
                    updateCallback.run();
                }
            });

            programPanel.add(checkbox);
            checkboxMap.put(program, checkbox);
        }

        // Set section header state based on program checkboxes
        updateSectionHeaderState(sectionCheckbox, checkboxMap);

        // Add section header listener
        sectionCheckbox.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;

            // Update all checkboxes in this section
            for (Map.Entry<String, JCheckBox> entry : checkboxMap.entrySet()) {
                JCheckBox checkbox = entry.getValue();
                String program = entry.getKey();

                // Only update if state is different to avoid event loops
                if (checkbox.isSelected() != selected) {
                    checkbox.setSelected(selected);
                    filter.setProgramEnabled(program, selected);
                }
            }

            // Notify FilterManager to update the table
            FilterManager.updateTable(sectionCheckbox);

            if (updateCallback != null) {
                updateCallback.run();
            }
        });

        contentPanel.add(programPanel);
    }

    /**
     * Creates a section header with a checkbox.
     *
     * @param contentPanel The panel to add the header to
     * @param title The title for the section
     * @return The checkbox used as the section header
     */
    private static JCheckBox createSectionHeader(JPanel contentPanel, String title) {
        JCheckBox headerCheckbox = new HoverCheckBox(title);
        headerCheckbox.setFont(headerCheckbox.getFont().deriveFont(Font.BOLD));
        headerCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);


        contentPanel.add(headerCheckbox);

        return headerCheckbox;
    }

    /**
     * Updates the state of a section header checkbox based on its child checkboxes.
     *
     * @param sectionCheckbox The section header checkbox
     * @param checkboxMap Map of program checkboxes in this section
     */
    private static void updateSectionHeaderState(JCheckBox sectionCheckbox, Map<String, JCheckBox> checkboxMap) {
        // Count selected checkboxes
        int selectedCount = 0;
        for (JCheckBox checkbox : checkboxMap.values()) {
            if (checkbox.isSelected()) {
                selectedCount++;
            }
        }

        // Update section header state without triggering events
        ItemListener[] listeners = sectionCheckbox.getItemListeners();

        // Only try to remove listeners if there are any
        if (listeners.length > 0) {
            for (ItemListener listener : listeners) {
                sectionCheckbox.removeItemListener(listener);
            }
        }

        if (selectedCount == 0) {
            sectionCheckbox.setSelected(false);
        } else if (selectedCount == checkboxMap.size()) {
            sectionCheckbox.setSelected(true);
        } else {
            // If some but not all checkboxes are selected, set the section header to indeterminate
            sectionCheckbox.setSelected(true);
        }

        // Re-add the item listeners
        if (listeners.length > 0) {
            for (ItemListener listener : listeners) {
                sectionCheckbox.addItemListener(listener);
            }
        }
    }


    /**
     * Gets round counts sorted in the desired order.
     * @param programsByRoundCount The map of programs by round count
     */
    private static List<Integer> getSortedRoundCounts(Map<Integer, List<String>> programsByRoundCount) {
        List<Integer> sortedRoundCounts = new ArrayList<>(programsByRoundCount.keySet());
        sortedRoundCounts.sort((a, b) -> {
            // Special case: -1 (mixed) should be first
            if (a == -1) return -1;
            if (b == -1) return 1;
            // Otherwise sort in descending order
            return b.compareTo(a);
        });
        return sortedRoundCounts;
    }

    /**
     * Determines the section name for a given round count.
     *
     * @param roundCount The round count
     * @return The section name
     */
    private static String getSectionName(int roundCount){
        switch (roundCount){
            case -1 -> {return "Mixed Rounds";}
            case 0 -> {return "No Rounds";}
            case 1 -> {return "1 Round";}
            default -> {return roundCount + " Rounds";}
        }
    }
}


