package com.echo.ui.component;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableCellRenderer;

/**
 * Header renderer that decorates the table's default header with a sort direction arrow.
 * The arrow appears only on the currently sorted column: pointing down for ascending (A-Z),
 * up for descending (Z-A), and disappears when the sort moves to another column or is cleared.
 *
 * Arrows are drawn as vectors rather than image assets, so they stay crisp at any display scale.
 */
public class SortArrowHeaderRenderer implements TableCellRenderer {

    private static final Icon DOWN_ARROW = new ArrowIcon(true);
    private static final Icon UP_ARROW = new ArrowIcon(false);

    private final TableCellRenderer defaultRenderer;

    /**
     * Wraps the given renderer, typically the JTableHeader's look-and-feel default.
     * @param defaultRenderer renderer that paints the base header cell
     */
    public SortArrowHeaderRenderer(TableCellRenderer defaultRenderer) {
        this.defaultRenderer = defaultRenderer;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        Component component = defaultRenderer.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        // Setting the icon on every call also clears any arrow the look-and-feel drew itself,
        // keeping this renderer the single source of truth for sort indication.
        if (component instanceof JLabel label) {
            label.setIcon(getSortArrow(table, column));
            label.setHorizontalTextPosition(JLabel.LEADING);
        }
        return component;
    }

    /**
     * Determines which arrow (if any) the given view column should display.
     * @param table table whose sorter holds the current sort state
     * @param viewColumn column index as displayed
     * @return down arrow for ascending, up arrow for descending, or null when this column is unsorted
     */
    private Icon getSortArrow(JTable table, int viewColumn) {
        RowSorter<?> sorter = table.getRowSorter();
        if (sorter == null) {
            return null;
        }

        List<? extends RowSorter.SortKey> sortKeys = sorter.getSortKeys();
        if (sortKeys.isEmpty()) {
            return null;
        }

        // Only the primary sort key gets an arrow, so sorting one column removes the previous column's icon
        RowSorter.SortKey primaryKey = sortKeys.get(0);
        if (primaryKey.getColumn() != table.convertColumnIndexToModel(viewColumn)) {
            return null;
        }

        if (primaryKey.getSortOrder() == SortOrder.ASCENDING) {
            return DOWN_ARROW;
        }
        if (primaryKey.getSortOrder() == SortOrder.DESCENDING) {
            return UP_ARROW;
        }
        return null;
    }

    /**
     * Small triangle icon drawn with antialiased vector graphics in the header's text color.
     */
    private static class ArrowIcon implements Icon {
        private static final int SIZE = 8;

        private final boolean pointsDown;

        ArrowIcon(boolean pointsDown) {
            this.pointsDown = pointsDown;
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getForeground());

            int[] xPoints = {x, x + SIZE, x + SIZE / 2};
            int[] yPoints = pointsDown
                    ? new int[]{y + 2, y + 2, y + SIZE - 1}
                    : new int[]{y + SIZE - 2, y + SIZE - 2, y + 1};
            g2.fillPolygon(xPoints, yPoints, 3);
            g2.dispose();
        }
    }
}
