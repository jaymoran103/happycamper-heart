package com.echo.ui.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import com.echo.domain.DataConstants;
import com.echo.domain.RosterHeader;

public class TableLook {

    // These settings are managed through TableColors and ViewSettings classes
    // This class provides utility methods for applying those settings to tables

   /**
     * Configures the appearance of table headers.
     * Sets up tooltips, cursor, and custom rendering.
     */
    public static void doHeaderLook(JTable table) {
        JTableHeader header = table.getTableHeader();
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.setToolTipText(TextConstants.TABLE_HEADER_TOOLTIP);

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel headerLabel = new JLabel(value.toString());
                headerLabel.setHorizontalAlignment(JLabel.CENTER);
                headerLabel.setBackground(TableColors.getHeaderColor());
                headerLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.BLACK));
                headerLabel.setOpaque(true);
                return headerLabel;
            }
        });
    }

    /**
     * Configures the appearance of table cells:
     * - Grid colors
     * - Alternating row colors
     * - Selection highlighting
     * - Empty field highlighting
     */
    public static void doCellLook(JTable table) {
        table.setGridColor(TableColors.getGridColor());

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                // Convert the value for display
                String displayValue = DataConstants.getDisplayValue((String)value);
                Component cell = super.getTableCellRendererComponent(table, displayValue,
                        isSelected, hasFocus, row, column);

                boolean useAltColor = (row % 2 == 0) || !TableColors.isAlternateShadesEnabled();

                // Disable cell selection border
                if (cell instanceof JLabel jLabel) {
                    jLabel.setBorder(null);
                }

                // Apply appropriate background color based on state and even/odd row status

                //If the cell is selected, apply the selected cell color
                if (isSelected) {
                    cell.setBackground(useAltColor ? TableColors.getSelectedEvenColor()
                                                   : TableColors.getSelectedOddColor());
                }

                //If the cell is empty and should be highlighted, apply the flagged color
                else if (shouldDoHighlight(table.getColumnName(column), displayValue)) {
                    cell.setBackground(useAltColor ? TableColors.getFlaggedEvenColor()
                                                   : TableColors.getFlaggedOddColor());
                }

                //If the cell is neither selected nor flagged, apply the default table colors
                else {
                    cell.setBackground( useAltColor ? TableColors.getTableEvenColor()
                                                    : TableColors.getTableOddColor());
                }

                cell.setForeground(Color.BLACK);
                return cell;
            }
        });
    }

    /**
     * Helper method indicates whether or not a cell should be highlighted, based on its column and content
     * Most columns have empty values highlighted, but some 'problem' columns invert this,
     * emphasizing fields that are unrequested or incompatible with a camper's swim level
     * 
     * @param columnName cell's header, used to determine the column type
     * @param cellValue cell's value, used to determine if cell is empty or not
     * @return true if cell should be highlighted. Obtained by XOR of empty boolean and status as a 'problem' column
     */
    private static boolean shouldDoHighlight(String columnName, String cellValue) {

        //If highlighting is disabled, return false (never highlight)
        if (!TableColors.isHighlightEmptyDataEnabled()){
            return false;
        }

        //If column matches a problem column, highlighting rules are reversed, so any data is highlighted
        boolean isProblemColumn = columnName.equals(RosterHeader.UNREQUESTED_ACTIVITIES.standardName)
                                || columnName.equals(RosterHeader.SWIMCONFLICTS.standardName)
                                || columnName.equals(RosterHeader.MEDICAL_NOTES.standardName);

        // XOR means that a problem column with data OR a normal column without are both highlighted                    
        return isProblemColumn ^ DataConstants.isEmpty(cellValue);                     



    }


    
}
