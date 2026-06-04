package com.echo.ui.component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Custom TableRowSorter that supports different sorting modes for different columns.
 * This allows for numeric sorting of columns that contain numeric data stored as strings.
 */
public class CustomTableRowSorter<M extends TableModel> extends TableRowSorter<M> {
    
    /**
     * Enum defining different sorting modes for columns.
     */
    public enum SortMode {
        STRING, //Default string comparison
        
        NUMERIC, //Numeric comparison (parses strings as numbers) 
        
        PERCENTAGE //Percentage comparison (strips % sign and parses as number)
    }
    
    // Map to store sorting modes for each column
    private final Map<Integer, SortMode> columnSortModes = new HashMap<>();
    
    /**
     * Creates a new CustomTableRowSorter with the specified model.
     * 
     * @param model The table model to use
     */
    public CustomTableRowSorter(M model) {
        super(model);
    }
    
    /**
     * Sets the sort mode for a specific column.
     * 
     * @param columnIndex The index of the column
     * @param mode The sort mode to use for the column
     */
    public void setSortMode(int columnIndex, SortMode mode) {
        columnSortModes.put(columnIndex, mode);


        
        
        // Set a custom comparator based on the sort mode
        switch (mode) {
            case NUMERIC:
                setComparator(columnIndex, new NumericComparator());
                break;
            case PERCENTAGE:
                setComparator(columnIndex, new PercentageComparator());
                break;
            case STRING: // Default to string comparison
            default:
                // Use default string comparator
                setComparator(columnIndex, null);
                break;
        }
    }
    
    /**
     * Gets the sort mode for a specific column.
     * 
     * @param columnIndex The index of the column
     * @return The sort mode for the column, or STRING if not specified
     */
    public SortMode getSortMode(int columnIndex) {
        return columnSortModes.getOrDefault(columnIndex, SortMode.STRING);
    }
    
    /**
     * Comparator for numeric values stored as strings.
     */
    private static class NumericComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            
            try {
                double n1 = Double.parseDouble(o1.trim());
                double n2 = Double.parseDouble(o2.trim());
                return Double.compare(n1, n2);
            } catch (NumberFormatException e) {
                // Fall back to string comparison if parsing fails
                return o1.compareTo(o2);
            }
        }
    }
    
    /**
     * Comparator for percentage values stored as strings (e.g., "85%").
     */
    private static class PercentageComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            
            try {
                // Remove % sign and parse as double
                double n1 = Double.parseDouble(o1.replace("%", "").trim());
                double n2 = Double.parseDouble(o2.replace("%", "").trim());
                return Double.compare(n1, n2);
            } catch (NumberFormatException e) {
                // Fall back to string comparison if parsing fails
                return o1.compareTo(o2);
            }
        }
    }
}
