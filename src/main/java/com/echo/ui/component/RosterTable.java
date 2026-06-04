package com.echo.ui.component;

import java.awt.BorderLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.filter.FilterManager;
import com.echo.ui.component.CustomTableRowSorter.SortMode;
import com.echo.ui.dialog.ColumnSizingOption;
import com.echo.ui.dialog.ColumnVisibilityDialog;

/**
 * Component for displaying roster data in a table.
 */
public class RosterTable extends JPanel {
    private EnhancedRoster roster;
    private FilterManager filterManager;

    private final JTable table;
    private final RosterTableModel tableModel;
    private final CustomTableRowSorter<RosterTableModel> rowSorter;

    /**
     * Creates a new RosterTable.
     */
    public RosterTable() {
        setLayout(new BorderLayout());

        tableModel = new RosterTableModel();
        table = new JTable(tableModel);
        rowSorter = new CustomTableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);


    }

    /**
     * Sets the roster to display.
     *
     * @param roster The roster to display
     * @param filterManager The filter manager to apply
     */
    public void setRoster(EnhancedRoster roster, FilterManager filterManager) {
        this.roster = roster;
        this.filterManager = filterManager;

        tableModel.setRoster(roster, filterManager);
        tableModel.fireTableStructureChanged();

        // Set up column widths and disable auto-resize
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Only set default widths if this is the first time loading the table
        // This prevents overriding user-set column widths when filters change
        if (table.getColumnCount() > 0 && table.getColumnModel().getColumn(0).getPreferredWidth() <= 0) {
            for (int i = 0; i < table.getColumnCount(); i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(100);
            }
        }

        // Apply auto-sizing to columns by default
        SwingUtilities.invokeLater(() -> {
            ColumnVisibilityDialog.applySelectedSizing(table);
        });

        // Configure custom sorting for specific columns
        configureColumnSorting();
    }

    /**
     * Configures custom sorting for specific columns based on their content type.
     * This method sets appropriate sort modes for columns that should be sorted numerically.
     */
    private void configureColumnSorting() {
        if (roster == null) {
            return;
        }

        List<String> headers = tableModel.visibleHeaders;

        // Reset all column sorters to default string sorting
        for (int i = 0; i < headers.size(); i++) {
            rowSorter.setSortMode(i, SortMode.STRING);
        }

        // Set numeric sorting for specific columns
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);

            // Find the corresponding RosterHeader enum
            RosterHeader rosterHeader = RosterHeader.determineHeaderType(header);

            // Set the appropriate sort mode based on the header type
            if (rosterHeader != null && rosterHeader.doNumericSort()) {
                rowSorter.setSortMode(i, SortMode.NUMERIC);
            } else {
                rowSorter.setSortMode(i, SortMode.STRING);
            }
        }
    }

    /**
     * Updates the table to reflect filter changes.
     * This should be called whenever a filter is modified.
     */
    public void applyFilters() {
        //System.out.println("RosterTable.applyFilters called");
        if (roster == null || filterManager == null) {
            //System.out.println("RosterTable.applyFilters: roster or filterManager is null");
            return;
        }

        //System.out.println("RosterTable.applyFilters: Updating table model");

        // Force the model to reapply filters
        tableModel.updateModel();
        tableModel.fireTableDataChanged();

        // Ensure sorting is correctly configured
        configureColumnSorting();

        //System.out.println("RosterTable.applyFilters: Table model updated");
    }

    /**
     * Gets the table component.
     *
     * @return The JTable component
     */
    public JTable getTable() {
        return table;
    }

    /**
     * Gets the row sorter for the table.
     *
     * @return The CustomTableRowSorter
     */
    public CustomTableRowSorter<RosterTableModel> getRowSorter() {
        return rowSorter;
    }

    /**
     * Gets the visible headers in the table.
     *
     * @return List of visible header names
     */
    public List<String> getVisibleHeaders() {
        if (roster == null) {
            return new ArrayList<>();
        }
        return roster.getVisibleHeaders();
    }

    /**
     * Gets the visible headers in the table, ordered according to RosterHeader enum order.
     *
     * @return List of visible header names in the correct display order
     */
    public List<String> getOrderedVisibleHeaders() {
        if (roster == null) {
            return new ArrayList<>();
        }
        return roster.getOrderedVisibleHeaders();
    }





    /**
     * Shows a dialog for configuring column visibility.
     */
    public void showColumnVisibilityDialog() {
        if (roster == null) {
            return;
        }


        // Ensure the dialog uses the current roster's headers
        ColumnVisibilityDialog.resetCachedSettings();

        Window window = SwingUtilities.getWindowAncestor(this);
        ColumnVisibilityDialog dialog = new ColumnVisibilityDialog(window, roster, table);
        dialog.showDialog();

        if (dialog.isInputConfirmed()) {
            // Get the column sizing option and custom width before updating the table
            ColumnSizingOption sizingOption = dialog.getColumnSizingOption();
            int customWidth = dialog.getCustomWidth();

            // Update the roster's header visibility through the table model
            tableModel.fireTableStructureChanged();

            // Apply column sizing after the table structure is updated
            SwingUtilities.invokeLater(() -> {
                // Apply sizing based on selected option
                if (sizingOption != null) {
                    switch (sizingOption) {
                        case AUTO_SIZE -> {
                            // Auto-size all columns to fit their content
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
                        case EQUAL_WIDTH -> {
                            // Set all columns to the same width
                            for (int column = 0; column < table.getColumnCount(); column++) {
                                table.getColumnModel().getColumn(column).setPreferredWidth(100);
                            }
                        }
                        case CUSTOM_WIDTH -> {
                            // Set all columns to the custom width
                            for (int column = 0; column < table.getColumnCount(); column++) {
                                table.getColumnModel().getColumn(column).setPreferredWidth(customWidth);
                            }
                        }
                    }
                }

                // Force the table to update its layout
                table.doLayout();
                table.getTableHeader().resizeAndRepaint();
                table.repaint();
            });
        }
    }

    /**
     * Table model for displaying roster data.
     */
    private class RosterTableModel extends AbstractTableModel {
        private EnhancedRoster roster;
        private FilterManager filterManager;
        private List<String> visibleHeaders = new ArrayList<>();
        private List<Camper> filteredCampers = new ArrayList<>();

        /**
         * Creates a new RosterTableModel.
         */
        public RosterTableModel() {
            // Initialize with empty lists
        }

        /**
         * Sets the roster for this model.
         *
         * @param roster The roster to display
         * @param filterManager The filter manager to apply
         */
        public void setRoster(EnhancedRoster roster, FilterManager filterManager) {
            //System.out.println("RosterTableModel.setRoster: Setting roster and filter manager");
            this.roster = roster;
            this.filterManager = filterManager;

            // updateModel(); currently handled in fireTableStructureChanged
            // updateLook();  currently handled in fireTableStructureChanged

            fireTableStructureChanged();
            fireTableDataChanged();
        }

        /**
         * Updates the model data from the roster.
         */
        public void updateModel() {
            if (roster == null) {
                visibleHeaders = new ArrayList<>();
                filteredCampers = new ArrayList<>();
                return;
            }

            visibleHeaders = roster.getOrderedVisibleHeaders();

            // Apply filters
            filteredCampers = new ArrayList<>();

            for (Camper camper : roster.getCampers()) {
                boolean passes = filterManager == null || filterManager.applyFilters(camper);
                if (passes) {
                    filteredCampers.add(camper);
                }
            }
        }

        private void updateLook(){
            TableLook.doHeaderLook(table);
            TableLook.doCellLook(table);
        }

        @Override
        public int getRowCount() {
            return filteredCampers.size();
        }

        @Override
        public int getColumnCount() {
            return visibleHeaders.size();
        }

        @Override
        public String getColumnName(int column) {
            return visibleHeaders.get(column);
        }

        @Override
        public String getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= filteredCampers.size() ||
                    columnIndex < 0 || columnIndex >= visibleHeaders.size()) {
                return null;
            }

            Camper camper = filteredCampers.get(rowIndex);
            String header = visibleHeaders.get(columnIndex);

            String value = camper.getValue(header);
            return value;
        }

        @Override
        public void fireTableStructureChanged() {
            updateModel();
            updateLook();
            super.fireTableStructureChanged();

            // Reconfigure column sorting when table structure changes
            SwingUtilities.invokeLater(() -> configureColumnSorting());
        }
    }
}
