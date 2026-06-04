package com.echo.ui.dialog;

import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JTable;

import com.echo.domain.EnhancedRoster;
import com.echo.service.ColumnSettings;
import com.echo.ui.selector.ActionButtonSelector;
import com.echo.ui.selector.CheckBoxSelector;
import com.echo.ui.selector.InputSelector;
import com.echo.ui.selector.NumberInputSelector;
import com.echo.ui.selector.RadioButtonSelector;

/**
 * Dialog for selecting which columns to display and configuring column properties.
 */
public class ColumnVisibilityDialog extends InputsDialog {
    // Static cache for remembering settings between dialog instances
    private static final ColumnSettings cachedSettings = new ColumnSettings();

    private final EnhancedRoster roster;
    private final JTable table;

    private CheckBoxSelector columnVisibilitySelector;
    private RadioButtonSelector<ColumnSizingOption> columnSizingSelector;
    private NumberInputSelector customWidthSelector;
    private ActionButtonSelector actionButtonSelector;

    private static final int COLUMN_DIALOG_WIDTH = 400;


    /**
     * Creates a new ColumnVisibilityDialog.
     *
     * @param parent The parent window
     * @param roster The roster containing the column data
     * @param table The table to apply column settings to
     */
    public ColumnVisibilityDialog(Window parent, EnhancedRoster roster, JTable table) {
        super(parent, true, "Column Settings", createSelectors(roster, table), COLUMN_DIALOG_WIDTH, "Apply");
        this.roster = roster;
        this.table = table;

        // Store references to selectors
        actionButtonSelector = (ActionButtonSelector) selectors[0];
        columnVisibilitySelector = (CheckBoxSelector) selectors[1];
        columnSizingSelector = (RadioButtonSelector<ColumnSizingOption>) selectors[2];
        customWidthSelector = (NumberInputSelector) selectors[3];

        // Set up conditional enabling for custom width input
        columnSizingSelector.setUpdateCallback(() -> {
            boolean enableCustomWidth = columnSizingSelector.getValue() == ColumnSizingOption.CUSTOM_WIDTH;
            customWidthSelector.setEnabled(enableCustomWidth);
            updateContinueButton();
        });

        // Initial update
        boolean enableCustomWidth = columnSizingSelector.getValue() == ColumnSizingOption.CUSTOM_WIDTH;
        customWidthSelector.setEnabled(enableCustomWidth);
    }

    /**
     * Creates the selectors for the dialog.
     *
     * @param roster The roster containing the column data
     * @param table The table to apply column settings to
     * @return Array of selectors
     */
    private static InputSelector<?>[] createSelectors(EnhancedRoster roster, JTable table) {
        // Update cached settings from roster if column visibility map is empty
        if (cachedSettings.getColumnVisibility().isEmpty()) {
            cachedSettings.updateFromRoster(roster);
        }

        // Create column visibility selector using cached settings or current roster state
        Map<String, Boolean> columnMap;
        if (!cachedSettings.getColumnVisibility().isEmpty()) {
            // Use cached settings
            columnMap = cachedSettings.getColumnVisibility();

            // Add any new columns from the roster that aren't in the cached settings
            for (String header : roster.getAllHeaders()) {
                if (!columnMap.containsKey(header)) {
                    columnMap.put(header, roster.isHeaderVisible(header));
                }
            }
        } else {
            // Use current roster state
            columnMap = new LinkedHashMap<>();
            for (String header : roster.getAllHeaders()) {
                columnMap.put(header, roster.isHeaderVisible(header));
            }
        }

        CheckBoxSelector visibilitySelector = new CheckBoxSelector("Column Visibility", columnMap, false);

        // Create action buttons for column visibility
        ActionButtonSelector actionSelector = new ActionButtonSelector(
            "Visibility Controls",
            new String[] {"Show All", "Hide All", "Reset to Default"},
            new Runnable[] {
                () -> visibility_showAll(roster, visibilitySelector),
                () -> visibility_hideAll(roster, visibilitySelector),
                () -> visibility_resetToDefault(roster, visibilitySelector)
            }
        );

        // Create column sizing selector with cached option
        RadioButtonSelector<ColumnSizingOption> sizingSelector = RadioButtonSelector.forColumnSizing(
            "Column Sizing",
            cachedSettings.getSizingOption()
        );

        // Create custom width input with cached value
        NumberInputSelector widthSelector = new NumberInputSelector(
            "Custom Width",
            cachedSettings.getCustomWidth(), // Use cached value
            50,  // min value
            500, // max value
            "px" // unit label
        );



        return new InputSelector<?>[] {
            actionSelector,
            visibilitySelector,
            sizingSelector,
            widthSelector
        };
    }


    private static void visibility_showAll(EnhancedRoster roster, CheckBoxSelector visibilitySelector){
        Map<String, Boolean> allVisibleMap = new LinkedHashMap<>();
        for (String header : roster.getAllHeaders()) {
            allVisibleMap.put(header, true);
        }
        visibilitySelector.setValue(allVisibleMap);
    }

    private static void visibility_hideAll(EnhancedRoster roster, CheckBoxSelector visibilitySelector){
        Map<String, Boolean> allHiddenMap = new LinkedHashMap<>();
        for (String header : roster.getAllHeaders()) {
            allHiddenMap.put(header, false);
        }
        visibilitySelector.setValue(allHiddenMap);
    }


    private static void visibility_resetToDefault(EnhancedRoster roster, CheckBoxSelector visibilitySelector){
        Map<String, Boolean> defaultMap = new LinkedHashMap<>();
        for (String header : roster.getAllHeaders()) {
            // Get default visibility from RosterHeader enum if possible
            com.echo.domain.RosterHeader rosterHeader = com.echo.domain.RosterHeader.determineHeaderType(header);
            boolean defaultVisibility = true; // Default to visible for custom headers

            if (rosterHeader != null) {
                defaultVisibility = rosterHeader.defaultVisibility;
            }

            defaultMap.put(header, defaultVisibility);
        }
        visibilitySelector.setValue(defaultMap);
    }

    /**
     * Sizes all columns to fit their content.
     *
     * @param table The table to resize columns for
     */
    public static void sizeColumnsToFitContent(JTable table) {
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 50; // Minimum width

            // Get width of column header
            javax.swing.table.TableColumn tableColumn = table.getColumnModel().getColumn(column);
            javax.swing.table.TableCellRenderer headerRenderer = tableColumn.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = table.getTableHeader().getDefaultRenderer();
            }

            java.awt.Component headerComp = headerRenderer.getTableCellRendererComponent(
                table, tableColumn.getHeaderValue(), false, false, 0, column);

            width = Math.max(width, headerComp.getPreferredSize().width);

            // Get maximum width of cells in the column
            for (int row = 0; row < table.getRowCount(); row++) {
                javax.swing.table.TableCellRenderer renderer = table.getCellRenderer(row, column);
                java.awt.Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(width, comp.getPreferredSize().width);
            }

            // Add some padding
            width += 10;

            // Set the width
            tableColumn.setPreferredWidth(width);
        }
    }



    /**
     * Gets the selected column sizing option.
     *
     * @return The selected column sizing option, or null if no option was selected
     */
    public ColumnSizingOption getColumnSizingOption() {
        return columnSizingSelector.getValue();
    }

    /**
     * Gets the custom width value.
     *
     * @return The custom width value
     */
    public int getCustomWidth() {
        return customWidthSelector.getValue();
    }

    /**
     * Updates the selections in the roster and table.
     * Called when the user confirms the dialog.
     */
    @Override
    protected void updateSelections() {
        // Update column visibility
        Map<String, Boolean> columnMap = columnVisibilitySelector.getValue();
        for (Map.Entry<String, Boolean> entry : columnMap.entrySet()) {
            roster.setHeaderVisibility(entry.getKey(), entry.getValue());
        }

        // Update column sizing
        ColumnSizingOption sizingOption = getColumnSizingOption();
        int customWidth = customWidthSelector.getValue();

        // Save settings to cache
        cachedSettings.setColumnVisibility(columnMap)
                      .setSizingOption(sizingOption)
                      .setCustomWidth(customWidth);

        // Apply sizing based on selected option
        applySelectedSizing(table);

        // Force the table to update its column widths
        table.doLayout();
        table.getTableHeader().resizeAndRepaint();
        table.repaint();
    }

    /**
     * Applies the currently cached column sizing settings to a table.
     * This can be called without creating a dialog instance.
     *
     * @param table The table to apply settings to
     */
    public static void applySelectedSizing(JTable table) {
        if (table == null) {
            return;
        }

        ColumnSizingOption sizingOption = cachedSettings.getSizingOption();
        int customWidth = cachedSettings.getCustomWidth();

        if (sizingOption != null) {
            switch (sizingOption) {
                case AUTO_SIZE -> sizeColumnsToFitContent(table);
                case EQUAL_WIDTH -> {
                    for (int column = 0; column < table.getColumnModel().getColumnCount(); column++) {
                        table.getColumnModel().getColumn(column).setPreferredWidth(100);
                    }
                }
                case CUSTOM_WIDTH -> {
                    for (int column = 0; column < table.getColumnModel().getColumnCount(); column++) {
                        table.getColumnModel().getColumn(column).setPreferredWidth(customWidth);
                    }
                }
            }
        } else {
            // Default to auto-size if no option is set
            sizeColumnsToFitContent(table);
        }
    }

    /**
     * Resets the cached column settings.
     * This should be called when a new roster is loaded to ensure stale settings are not applied.
     */
    public static void resetCachedSettings() {
        cachedSettings.clearColumnVisibility();
    }
}
