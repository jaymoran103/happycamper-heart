package com.echo.ui.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.echo.logging.DetailedRosterException;
import com.echo.ui.component.TableColors;

/**
 * ErrorPanel displays a set of RosterExceptions corresponding to a single ErrorType, using a JTable to display context for each error instance
 */
public class DetailedErrorPanel extends ErrorPanel {

    private final String[] tableHeaders;
    private final String[][] tableData;

    // Constants for table sizing
    private static final int ROW_HEIGHT = 18;
    private static final int HEADER_HEIGHT = 25;
    private static final int MIN_TABLE_HEIGHT = ROW_HEIGHT+HEADER_HEIGHT;
    private static final int MAX_TABLE_HEIGHT = DialogConstants.WARNINGTABLE_HEIGHT;
    private static final int PADDING = 10;

    /**
     * Constructor sets the text and table fields, then builds the panel.
     *
     * @param parent Parent dialog that displays this panel
     * @param errors List of errors displayed in this panel
     */
    public DetailedErrorPanel(ErrorDialog parent, DetailedRosterException displayedExeption) {
        super(parent, displayedExeption);
        this.tableHeaders = displayedExeption.getTableHeaders();
        this.tableData = displayedExeption.getTableData();
    }

    /**
     * Sets up each panel element and adds to a vertical boxlayout.
     */
    @Override
    protected void buildPanel() {
        setBackground(DialogConstants.DIALOG_COLOR_MAIN);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Explanation text at top
        JLabel explanationLabel = createWrappingLabel(generalExplanationText);
        add(explanationLabel);

        // Secondary explanation above table
        JLabel secondaryExplanationLabel = createWrappingLabel(secondaryExplanationText);
        add(secondaryExplanationLabel);

        // Table displaying details for each error instance
        if (displayedExeption instanceof DetailedRosterException detailedRosterException) {
            JComponent tableContainer = createTableComponent(detailedRosterException);
            add(tableContainer);
        }
    }

    /**
     * Creates a JTable to show details specific to each error instance.
     *
     * @return Container holding the JTable
     */
    private JComponent createTableComponent(DetailedRosterException detailedRosterException) {
        // Table Model - holds data
        DefaultTableModel tableModel = new DefaultTableModel() {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        //Model - set up using data getters in DetailedRosterException
        String[] dataHeaders = detailedRosterException.getTableHeaders();
        String[][] dataRows = detailedRosterException.getTableData();
        tableModel.setDataVector(dataRows, dataHeaders);

        // Table - displays model
        JTable table = new JTable(tableModel);
        table.setShowGrid(true);
        table.setGridColor(tableGridColor);
        table.getTableHeader().setBackground(tableHeaderColor);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(ROW_HEIGHT);

        // Disable focus for the table
        table.getTableHeader().setFocusable(false);
        table.setFocusable(false);

        // Default renderer - applies some aesthetic settings including alternating row colors, selection color, and disables selection border
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
                Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);

                // Disable cell selection border
                if (cell instanceof JLabel) {
                    ((JLabel) cell).setBorder(null);
                }

                if (isSelected) {
                    cell.setBackground(rowIndex % 2 == 0 ? TableColors.getSelectedEvenColor() : TableColors.getSelectedOddColor());
                    cell.setForeground(Color.BLACK);
                } else {
                    cell.setBackground(rowIndex % 2 == 0 ? tableEvenColor : tableOddColor);
                }
                return cell;
            }
        });

        // Calculate appropriate table height based on content
        int tableHeight = calculateTableHeight(dataRows.length);

        // Create a dimension with standard width but dynamic height
        Dimension tableDimension = new Dimension(DialogConstants.WARNINGTABLE_WIDTH_MINIMUM, tableHeight);

        // Scroll Pane - allows the table to scroll
        JScrollPane tableScroller = new JScrollPane(table);
        tableScroller.setPreferredSize(tableDimension);
        tableScroller.setMaximumSize(tableDimension);
        tableScroller.setMinimumSize(tableDimension);
        tableScroller.getVerticalScrollBar().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Container - centers the table because boxlayout aligning is nonsense
        JPanel tableContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        tableContainer.add(tableScroller);
        tableContainer.setAlignmentX(LEFT_ALIGNMENT);

        return tableContainer;
    }

    /**
     * Calculates an appropriate table height based on the number of rows.
     *
     * @param rowCount The number of rows in the table
     * @return The calculated height in pixels
     */
    private int calculateTableHeight(int rowCount) {
        // Calculate height based on row count, header, and padding
        int contentHeight = (rowCount * ROW_HEIGHT) + HEADER_HEIGHT + PADDING;

        // Ensure height is within min/max bounds
        return Math.max(MIN_TABLE_HEIGHT, Math.min(contentHeight, MAX_TABLE_HEIGHT));
    }

    /**
     * Calculates the preferred height for this panel based on its content.
     * For DetailedErrorPanel, we add the table height to the text height.
     *
     * @return The preferred height in pixels
     */
    @Override
    public int calculatePreferredHeight() {
        // Calculate text height using the parent method
        int textHeight = super.calculatePreferredHeight();

        // Calculate table height based on data
        int tableHeight = calculateTableHeight(tableData.length);

        // Add some padding between text and table
        int totalHeight = textHeight + tableHeight + 20;

        return totalHeight;
    }
}
