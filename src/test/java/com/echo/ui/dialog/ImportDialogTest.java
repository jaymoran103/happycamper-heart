package com.echo.ui.dialog;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.echo.domain.EnhancedRoster;
import com.echo.service.ExportService;
import com.echo.service.ImportService;
import com.echo.service.RosterService;

/**
 * Test class for demonstrating the ImprovedImportsDialog.
 */
public class ImportDialogTest {

    private JFrame frame;
    private RosterService rosterService;

    /**
     * Creates a new ImportDialogTest.
     */
    public ImportDialogTest() {
        // Create services
        ImportService importService = new ImportService();
        ExportService exportService = new ExportService();
        rosterService = new RosterService(importService, exportService);

        // Create UI
        frame = new JFrame("Import Dialog Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);

        JPanel panel = new JPanel(new BorderLayout());

        // Add buttons
        JButton oldDialogButton = new JButton("Show Old Import Dialog");
        oldDialogButton.addActionListener(this::showOldDialog);

        JButton newDialogButton = new JButton("Show New Import Dialog");
        newDialogButton.addActionListener(this::showNewDialog);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(oldDialogButton);
        buttonPanel.add(newDialogButton);

        panel.add(buttonPanel, BorderLayout.CENTER);

        frame.add(panel);
        frame.setLocationRelativeTo(null);
    }

    /**
     * Shows the old import dialog.
     */
    private void showOldDialog(ActionEvent e) {
        ImportDialog dialog = new ImportDialog(frame, rosterService);
        dialog.setVisible(true);

        if (dialog.isImportSuccessful()) {
            EnhancedRoster roster = dialog.getImportedRoster();
            JOptionPane.showMessageDialog(frame,
                "Import successful! Roster has " + roster.getCampers().size() + " rows and " +
                roster.getAllHeaders().size() + " columns.",
                "Import Result",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Shows the new import dialog.
     */
    private void showNewDialog(ActionEvent e) {
        ImportDialog dialog = new ImportDialog(frame, rosterService);
        dialog.showDialog();

        if (dialog.isImportSuccessful()) {
            EnhancedRoster roster = dialog.getImportedRoster();
            JOptionPane.showMessageDialog(frame,
                "Import successful! Roster has " + roster.getCampers().size() + " rows and " +
                roster.getAllHeaders().size() + " columns.",
                "Import Result",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Shows the frame.
     */
    public void show() {
        frame.setVisible(true);
    }

    /**
     * Main method.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ImportDialogTest test = new ImportDialogTest();
            test.show();
        });
    }
}
