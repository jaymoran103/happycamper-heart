package com.echo.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import com.echo.ui.component.TableColors;
import com.echo.ui.component.TableLook;

/**
 * Generic read-only report popup (D1): a table of pre-computed rows plus CSV export.
 *
 * Accepts separate Object rows (for numeric sort) and String rows (for CSV). All report content is
 * computed by the caller; this dialog is a thin shell that displays and exports verbatim.
 */
public class ReportDialog extends JDialog {

    private final List<String> headers;
    private final List<List<String>> csvRows;
    private final String defaultFileName;

    /**
     * @param parent          the owner window
     * @param title           the dialog title
     * @param headers         column headers
     * @param tableRows       Object rows for the table model (enables numeric sort)
     * @param csvRows         String rows for CSV export
     * @param defaultFileName suggested CSV filename
     */
    public ReportDialog(Window parent, String title, List<String> headers,
                        List<List<Object>> tableRows, List<List<String>> csvRows,
                        String defaultFileName) {
        super(parent, title, ModalityType.MODELESS);
        this.headers = headers;
        this.csvRows = csvRows;
        this.defaultFileName = defaultFileName;

        setLayout(new BorderLayout());
        add(new JScrollPane(buildTable(tableRows)), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.PAGE_END);

        setSize(new Dimension(620, 440));
        setLocationRelativeTo(parent);
    }

    // Builds the JTable for displaying the report data.
    private JTable buildTable(List<List<Object>> tableRows) {
        DefaultTableModel model = new DefaultTableModel(headers.toArray(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                // Numeric sort for Integer/Double columns; String otherwise
                if (!tableRows.isEmpty()) {
                    Object sample = tableRows.get(0).get(column);
                    if (sample instanceof Integer) return Integer.class;
                    if (sample instanceof Double)  return Double.class;
                }
                return String.class;
            }
        };
        for (List<Object> row : tableRows) {
            model.addRow(row.toArray());
        }
        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setAutoCreateRowSorter(true);
        table.setGridColor(TableColors.getGridColor());
        applyRenderer(table);
        TableLook.doHeaderLook(table);
        return table;
    }

    /**
     * Applies the custom renderer to the table.
     *
     * @param table The JTable to apply the renderer to
     */
    private void applyRenderer(JTable table) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                // Apply custom styling to each cell
                Component cell = super.getTableCellRendererComponent(t,
                        value == null ? "" : value.toString(), isSelected, hasFocus, row, column);

                setHorizontalAlignment(column == 0 ? JLabel.LEFT : JLabel.CENTER);
                setBorder(null);
                boolean alt = (row % 2 == 0) || !TableColors.isAlternateShadesEnabled();
                if (isSelected) {
                    cell.setBackground(alt ? TableColors.getSelectedEvenColor() : TableColors.getSelectedOddColor());
                } else {
                    cell.setBackground(alt ? TableColors.getTableEvenColor() : TableColors.getTableOddColor());
                }
                cell.setForeground(Color.BLACK);
                return cell;
            }
        };
        // Register for all column types used by this table (Integer for round counts, Object/String for the rest).
        table.setDefaultRenderer(Object.class,  renderer);
        table.setDefaultRenderer(Integer.class, renderer);
    }

    // Builds the button bar for the dialog.
    private JPanel buildButtonBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton exportButton = new JButton("Export CSV");
        exportButton.addActionListener(e -> exportCsv());
        panel.add(exportButton);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);

        return panel;
    }

    public void showDialog() {
        setVisible(true);
    }

    /**
     * Exports the report data to a CSV file, using apache commons CSVPrinter
     */
    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultFileName));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();

        try (FileWriter writer = new FileWriter(file);
             CSVPrinter printer = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.builder()
                             .setHeader(headers.toArray(new String[0]))
                             .setQuoteMode(QuoteMode.ALL)
                             .build())) {
            for (List<String> row : csvRows) {
                printer.printRecord(row);
            }
            JOptionPane.showMessageDialog(this, "Export successful", "Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
