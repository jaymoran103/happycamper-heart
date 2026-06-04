package com.echo.ui.selector;

import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.echo.ui.dialog.DialogUtils;
import com.echo.ui.elements.HoverCheckBox;


/**
 * InputSelector for selecting multiple boolean options.
 * Uses checkboxes to represent the options.
 */
public class CheckBoxSelector extends InputSelector<Map<String, Boolean>> {
    private final Map<String, Boolean> options;
    private Map<String, JCheckBox> checkBoxes;
    private final boolean requireSelection;
    private final boolean includeSelectAll;

    /**
     * Creates a new CheckBoxSelector with the specified options.
     *
     * @param title The title for this selector
     * @param options The available options and their initial states
     * @param requireSelection Whether at least one option must be selected
     */
    public CheckBoxSelector(String title, Map<String, Boolean> options, boolean requireSelection) {
        this(title, options, requireSelection, false);
    }

    /**
     * Creates a new CheckBoxSelector with the specified options.
     *
     * @param title The title for this selector
     * @param options The available options and their initial states
     * @param requireSelection Whether at least one option must be selected
     * @param includeSelectAll Whether to include a "Select All" checkbox
     */
    public CheckBoxSelector(String title, Map<String, Boolean> options, boolean requireSelection, boolean includeSelectAll) {
        super(title);
        this.options = new LinkedHashMap<>(options); // Copy to preserve order
        this.requireSelection = requireSelection;
        this.includeSelectAll = includeSelectAll;
    }

    @Override
    protected void buildSelectorPanel(JPanel panel) {
        // Create a panel for the checkboxes using our helper method
        JPanel checkBoxPanel = DialogUtils.createAlignedBoxPanel();

        // Create checkboxes
        checkBoxes = new LinkedHashMap<>();

        // Add "Select All" checkbox if requested
        if (includeSelectAll) {
            JCheckBox selectAllCheckBox = new HoverCheckBox("(Select All)");
            selectAllCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Add action listener to toggle all checkboxes
            selectAllCheckBox.addActionListener(_ -> {
                boolean selected = selectAllCheckBox.isSelected();

                // Update all regular checkboxes
                for (Map.Entry<String, Boolean> entry : options.entrySet()) {
                    options.put(entry.getKey(), selected);
                }

                // Update UI after all options are set
                if (checkBoxes != null) {
                    for (JCheckBox cb : checkBoxes.values()) {
                        cb.setSelected(selected);
                    }
                }

                notifyUpdateCallback();
            });

            checkBoxPanel.add(selectAllCheckBox, 0);
        }

        // Create regular option checkboxes
        for (Map.Entry<String, Boolean> entry : options.entrySet()) {
            JCheckBox checkBox = new HoverCheckBox(entry.getKey(), entry.getValue());
            checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Add action listener to update selection
            checkBox.addActionListener(_ -> {
                options.put(entry.getKey(), checkBox.isSelected());
                notifyUpdateCallback();
            });

            checkBoxes.put(entry.getKey(), checkBox);
            checkBoxPanel.add(checkBox);
        }

        // Set preferred size based on number of options
        int checkBoxHeight = 24; // Height of each checkbox
        this.componentHeight = options.size() * checkBoxHeight + 30; // checkbox heights + extra padding
        if (includeSelectAll) {
            this.componentHeight += checkBoxHeight; // Add space for "Select All" checkbox
        }

        panel.add(checkBoxPanel);
    }

    @Override
    public Map<String, Boolean> getValue() {
        return new LinkedHashMap<>(options); // Return a copy
    }

    @Override
    public void setValue(Map<String, Boolean> value) {
        if (value != null) {
            for (Map.Entry<String, Boolean> entry : value.entrySet()) {
                if (options.containsKey(entry.getKey())) {
                    options.put(entry.getKey(), entry.getValue());

                    if (checkBoxes != null && checkBoxes.containsKey(entry.getKey())) {
                        checkBoxes.get(entry.getKey()).setSelected(entry.getValue());
                    }
                }
            }

            notifyUpdateCallback();
        }
    }

    @Override
    public boolean hasSelection() {
        if (!requireSelection) {
            return true;
        }

        // Check if at least one option is selected
        for (Boolean selected : options.values()) {
            if (selected) {
                return true;
            }
        }

        return false;
    }


}
