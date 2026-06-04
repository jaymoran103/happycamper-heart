package com.echo.ui.filter;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.echo.ui.elements.HoverCheckBox;
import com.echo.filter.option.FilterOption;

/**
 * Factory class for creating collapsible filter panels.
 */
public class FilterPanelFactory {

    /**
     * Creates a filter panel with boolean toggle options.
     * Each option is represented by a checkbox that toggles a boolean state.
     *
     * @param title The title of the filter panel
     * @param options Map of option labels to their current boolean states
     * @param callbacks Map of option labels to callbacks that are called when the option is toggled
     * @return A collapsible filter panel with the specified options
     */
    public static CollapsibleFilterPanel createBooleanOptionsPanel(String title,
                                                                  Map<String, Boolean> options,
                                                                  Map<String, Consumer<Boolean>> callbacks) {
        ArrayList<String> labels = new ArrayList<>(options.keySet());
        ArrayList<Boolean> states = new ArrayList<>();
        ArrayList<Consumer<Boolean>> onClickMethods = new ArrayList<>();

        for (String label : labels) {
            states.add(options.get(label));
            onClickMethods.add(callbacks.get(label));
        }

        return createGenericPanel(title, labels, states, onClickMethods);
    }

    /**
     * Creates a filter panel with integer-keyed toggle options.
     * Each option is represented by a checkbox that toggles visibility for a specific integer value.
     *
     * @param title The title of the filter panel
     * @param options Map of integer values to their visibility states
     * @param labelProvider Function to convert integer values to display labels
     * @param toggleCallback Callback that is called when an option is toggled, with the integer value and new state
     * @return A collapsible filter panel with the specified options
     */
    public static CollapsibleFilterPanel createIntegerOptionsPanel(String title,
                                                                  Map<Integer, Boolean> options,
                                                                  Function<Integer, String> labelProvider,
                                                                  BiConsumer<Integer, Boolean> toggleCallback) {
        CollapsibleFilterPanel panel = new CollapsibleFilterPanel(title);
        // Create a panel with BorderLayout to push components to the top
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);

        // Create a panel for the checkboxes with vertical layout
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        checkboxPanel.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);

        // Add the checkbox panel to the NORTH position to push everything to the top
        contentPanel.add(checkboxPanel, BorderLayout.NORTH);

        List<Integer> sortedKeys = new ArrayList<>(options.keySet());
        sortedKeys.sort(null); // Natural order

        for (Integer value : sortedKeys) {
            String label = labelProvider.apply(value);
            boolean state = options.get(value);

            JCheckBox checkBox = new HoverCheckBox(label, state);
            final Integer finalValue = value; // For lambda capture

            checkBox.addActionListener(e -> {
                toggleCallback.accept(finalValue, checkBox.isSelected());
                // Notify that a change has occurred
                panel.notifyFilterChanged();
            });

            checkboxPanel.add(checkBox);
        }

        panel.addContent(contentPanel);
        return panel;
    }

    /**
     * Creates a filter panel with string-keyed toggle options.
     * Each option is represented by a checkbox that toggles visibility for a specific string value.
     *
     * @param title The title of the filter panel
     * @param options Map of string values to their visibility states
     * @param toggleCallback Callback that is called when an option is toggled, with the string value and new state
     * @return A collapsible filter panel with the specified options
     */
    public static CollapsibleFilterPanel createStringOptionsPanel(String title,
                                                                 Map<String, Boolean> options,
                                                                 BiConsumer<String, Boolean> toggleCallback) {
        CollapsibleFilterPanel panel = new CollapsibleFilterPanel(title);
        // Create a panel with BorderLayout to push components to the top
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);

        // Create a panel for the checkboxes with vertical layout
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        checkboxPanel.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);

        // Add the checkbox panel to the NORTH position to push everything to the top
        contentPanel.add(checkboxPanel, BorderLayout.NORTH);

        List<String> sortedKeys = new ArrayList<>(options.keySet());
        sortedKeys.sort(null); // Natural order

        for (String value : sortedKeys) {
            boolean state = options.get(value);

            JCheckBox checkBox = new HoverCheckBox(value, state);

            checkBox.addActionListener(e -> {
                toggleCallback.accept(value, checkBox.isSelected());
                // Notify that a change has occurred
                panel.notifyFilterChanged();
            });

            checkboxPanel.add(checkBox);
        }

        panel.addContent(contentPanel);
        return panel;
    }

    /**
     * Creates a generic filter panel with checkboxes.
     * Each checkbox corresponds to a filter option and is linked to the filter manager.
     *
     * @param title The title of the filter panel
     * @param buttonLabels The labels for each checkbox
     * @param buttonStates The initial states of each checkbox
     * @param onClickMethods The methods to call when each checkbox is clicked
     * @return CollapsibleFilterPanel containing checkboxes for each filter option
     */
    public static CollapsibleFilterPanel createGenericPanel(String title,
                                                            ArrayList<String> buttonLabels,
                                                            ArrayList<Boolean> buttonStates,
                                                            ArrayList<Consumer<Boolean>> onClickMethods){

        //Ensure arguments are valid
        if (!(buttonLabels.size() == buttonStates.size() && buttonLabels.size() == onClickMethods.size())){
            throw new IllegalArgumentException("FilterPanelFactory.createGenericPanel: all lists must be the same size");
        }
        if (buttonLabels.isEmpty()){
            throw new IllegalArgumentException("FilterPanelFactory.createGenericPanel: lists must not be empty");
        }

        CollapsibleFilterPanel panel = new CollapsibleFilterPanel(title);

        // Create a panel with BorderLayout to push components to the top
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);

        // Create a panel for the checkboxes with vertical layout
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        checkboxPanel.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);

        // Add the checkbox panel to the NORTH position to push everything to the top
        contentPanel.add(checkboxPanel, BorderLayout.NORTH);


        for (int i = 0; i < buttonLabels.size(); i++) {
            JCheckBox checkbox = new HoverCheckBox(buttonLabels.get(i),buttonStates.get(i));
            Consumer<Boolean> onClickConsumer = onClickMethods.get(i);

            checkbox.addActionListener(e -> {
                onClickConsumer.accept(checkbox.isSelected());
                // Notify that a filter has changed
                panel.notifyFilterChanged();
            });

            checkboxPanel.add(checkbox);
        }

        //Add constructed panel with checkboxes as content for the collapsible panel
        panel.addContent(contentPanel);

        return panel;
    }

    /**
     * Creates a filter panel with options defined by an enum.
     * Each enum value represents a filter option.
     *
     * @param <T> The enum type implementing FilterOption
     * @param title The title of the filter panel
     * @param optionStates Map of enum values to their current states
     * @param callback Callback that is called when an option is toggled
     * @return A collapsible filter panel with the specified options
     */
    public static <T extends Enum<T> & FilterOption> CollapsibleFilterPanel createEnumPanel(
            String title,
            Map<T, Boolean> optionStates,
            BiConsumer<T, Boolean> callback) {

        CollapsibleFilterPanel panel = new CollapsibleFilterPanel(title);
        // Create a panel with BorderLayout to push components to the top
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);

        // Create a panel for the checkboxes with vertical layout
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        checkboxPanel.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);

        // Add the checkbox panel to the NORTH position to push everything to the top
        contentPanel.add(checkboxPanel, BorderLayout.NORTH);

        for (Map.Entry<T, Boolean> entry : optionStates.entrySet()) {
            T option = entry.getKey();
            boolean state = entry.getValue();

            JCheckBox checkBox = new HoverCheckBox(option.getLabel(), state);
            checkBox.addActionListener(e -> {
                callback.accept(option, checkBox.isSelected());
                panel.notifyFilterChanged();
            });

            checkboxPanel.add(checkBox);
        }

        panel.addContent(contentPanel);
        return panel;
    }

    /**
     * Creates a filter panel with options defined by an enum, grouped by category.
     * Each enum value represents a filter option, and options are grouped by category.
     *
     * @param <T> The enum type implementing FilterOption
     * @param title The title of the filter panel
     * @param optionStates Map of enum values to their current states
     * @param callback Callback that is called when an option is toggled
     * @param categories Map of category names to predicates that determine which options belong to each category
     * @return A collapsible filter panel with the specified options grouped by category
     */
    public static <T extends Enum<T> & FilterOption> CollapsibleFilterPanel createGroupedEnumPanel(
            String title,
            Map<T, Boolean> optionStates,
            BiConsumer<T, Boolean> callback,
            Map<String, Predicate<T>> categories) {

        CollapsibleFilterPanel panel = new CollapsibleFilterPanel(title);

        // Create a panel for each category
        for (Map.Entry<String, Predicate<T>> category : categories.entrySet()) {
            String categoryName = category.getKey();
            Predicate<T> predicate = category.getValue();

            // Create a nested panel for this category
            CollapsibleFilterPanel categoryPanel = new CollapsibleFilterPanel(categoryName);
            // Create a panel with BorderLayout to push components to the top
            JPanel categoryContent = new JPanel(new BorderLayout());

            // Create a panel for the checkboxes with vertical layout
            JPanel categoryCheckboxPanel = new JPanel();
            categoryCheckboxPanel.setLayout(new BoxLayout(categoryCheckboxPanel, BoxLayout.Y_AXIS));

            // Add the checkbox panel to the NORTH position to push everything to the top
            categoryContent.add(categoryCheckboxPanel, BorderLayout.NORTH);

            // Add checkboxes for options in this category
            boolean hasOptions = false;
            for (Map.Entry<T, Boolean> entry : optionStates.entrySet()) {
                T option = entry.getKey();

                // Skip options that don't belong to this category
                if (!predicate.test(option)) {
                    continue;
                }

                hasOptions = true;
                boolean state = entry.getValue();

                JCheckBox checkBox = new HoverCheckBox(option.getLabel(), state);
                checkBox.addActionListener(e -> {
                    callback.accept(option, checkBox.isSelected());
                    panel.notifyFilterChanged();
                });

                categoryCheckboxPanel.add(checkBox);
            }

            // Only add the category panel if it has options
            if (hasOptions) {
                categoryPanel.addContent(categoryContent);
                panel.addContent(categoryPanel);
            }
        }

        return panel;
    }

    /**
     * Creates a filter panel with options defined by an enum, with default states.
     * Each enum value represents a filter option, and the default state is used if not specified.
     *
     * @param <T> The enum type implementing FilterOption
     * @param title The title of the filter panel
     * @param enumClass The enum class
     * @param callback Callback that is called when an option is toggled
     * @return A collapsible filter panel with the specified options
     */
    public static <T extends Enum<T> & FilterOption> CollapsibleFilterPanel createDefaultEnumPanel(
            String title,
            Class<T> enumClass,
            BiConsumer<T, Boolean> callback) {

        // Create a map with default states
        Map<T, Boolean> optionStates = new EnumMap<>(enumClass);
        for (T option : enumClass.getEnumConstants()) {
            optionStates.put(option, option.getDefaultState());
        }

        return createEnumPanel(title, optionStates, callback);
    }

    /**
     * Creates a filter panel with string-keyed program options.
     * This is a special case for program-specific options that are dynamically determined.
     *
     * @param title The title of the filter panel
     * @param programOptions Map of program names to their visibility states
     * @param programConsistency Map of program names to their consistency status
     * @param callback Callback that is called when an option is toggled
     * @return A collapsible filter panel with program options
     */
    public static CollapsibleFilterPanel createProgramOptionsPanel(
            String title,
            Map<String, Boolean> programOptions,
            Map<String, Boolean> programConsistency,
            BiConsumer<String, Boolean> callback) {

        CollapsibleFilterPanel panel = new CollapsibleFilterPanel(title);
        // Create a panel with BorderLayout to push components to the top
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);

        // Create a panel for the checkboxes with vertical layout
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        checkboxPanel.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);

        // Add the checkbox panel to the NORTH position to push everything to the top
        contentPanel.add(checkboxPanel, BorderLayout.NORTH);

        // Add a label for the programs
        JLabel programsLabel = new JLabel("Programs");
        programsLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        checkboxPanel.add(programsLabel);

        // Add checkboxes for each program
        for (Map.Entry<String, Boolean> entry : programOptions.entrySet()) {
            String program = entry.getKey();
            boolean state = entry.getValue();
            boolean isConsistent = programConsistency.getOrDefault(program, true);

            String label = program + (isConsistent ? " (consistent)" : " (inconsistent)");
            JCheckBox checkBox = new HoverCheckBox(label, state);

            checkBox.addActionListener(e -> {
                callback.accept(program, checkBox.isSelected());
                panel.notifyFilterChanged();
            });

            checkboxPanel.add(checkBox);
        }

        panel.addContent(contentPanel);
        return panel;
    }
}
