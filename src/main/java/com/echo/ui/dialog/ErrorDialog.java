package com.echo.ui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.echo.logging.DetailedRosterException;
import com.echo.logging.RosterException;
import com.echo.logging.RosterException.ErrorType;
import com.echo.logging.WarningManager;

/**
 * ErrorDialog extends CardDialog to display a set of RosterExceptions, sorted by ErrorType
 */
public class ErrorDialog extends CardDialog {

    private Map<ErrorType, ArrayList<RosterException>> errorLog;

    /**
     * Constructor sets up error panels, ensures the first is shown, and sets the dialog title to indicate the total number of error types caught
     *
     * @param parentWindow Parent window relevant to dialog setup
     * @param warningManager Log that provides errors to display
     */
    public ErrorDialog(Window parentWindow, WarningManager warningManager) {
        super(parentWindow, true, new String[]{"Close", "Back", "Next", "Continue"});
        errorLog = warningManager.getErrorLog();

        String titleString = errorLog.size() == 1 ? "Error" : Integer.toString(errorLog.size()) + " Errors";
        setTitle(titleString);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        setupErrorPanels();

        switchStateIfAllowed(0);

        setLocationRelativeTo(this.getParent());
        setAlwaysOnTop(true);
        requestFocus();

        // Calculate optimal height after panels are created
        SwingUtilities.invokeLater(() -> setOptimalDialogHeight());

        repaint();
        revalidate();
    }

    /**
     * Assigns the mainPanel a cardlayout and adds an error panel for each error type in errorLog
     */
    private void setupErrorPanels() {
        // Get dialog main panel, assign CardLayout
        JPanel mainPanel = getMainPanel();
        mainPanel.setLayout(cardLayout);

        // For each list of errors (sorted by type) create a panel, storing in the LinkedHashMap and adding to the CardLayout
        for (ArrayList<RosterException> errorSet : errorLog.values()) {
            ErrorPanel panel = errorSet.get(0) instanceof DetailedRosterException detailedRosterException
                    ? new DetailedErrorPanel(this, detailedRosterException)
                    : new ErrorPanel(this, errorSet.get(0));
            addCard(panel);
        }
    }

    /**
     * Calculates and sets the optimal dialog height based on all panels.
     */
    private void setOptimalDialogHeight() {
        // Get all panels
        Collection<JComponent> components = displayCards.values();

        // Find the tallest panel
        int maxPanelHeight = 0;
        for (Component component : components) {
            if (component instanceof ErrorPanel) {
                ErrorPanel panel = (ErrorPanel) component;
                int panelHeight = panel.calculatePreferredHeight();
                maxPanelHeight = Math.max(maxPanelHeight, panelHeight);
            }
        }

        // Add height for navigation panel
        int totalHeight = maxPanelHeight + DialogConstants.DIALOG_HEIGHT_BOTTOMPANEL;

        // Set minimum height
        int minHeight = 50; // Minimum reasonable height
        totalHeight = Math.max(totalHeight, minHeight);

        // Set maximum height (optional - remove if you want to allow taller dialogs)
        int maxAllowedHeight = 700; // Maximum reasonable height
        totalHeight = Math.min(totalHeight, maxAllowedHeight);

        // Set new size
        Dimension newDims = new Dimension(DialogConstants.DIALOG_WIDTH_STANDARD, totalHeight);
        setSize(newDims);
        // DialogUtils.fixSize(this, newDims);
        // // Center dialog
        // setLocationRelativeTo(getParent());

        // Force layout update
        invalidate();
        validate();
    }
}
