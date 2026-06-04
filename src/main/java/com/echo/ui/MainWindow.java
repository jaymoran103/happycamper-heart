package com.echo.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.echo.HappyCamper;
import com.echo.automation.TestPreset;
import com.echo.domain.EnhancedRoster;
import com.echo.filter.FilterManager;
import com.echo.service.RosterService;
import com.echo.ui.component.RosterTable;
import com.echo.ui.dialog.ColumnVisibilityDialog;
import com.echo.ui.dialog.ExportDialog;
import com.echo.ui.dialog.HelpDialog;
import com.echo.ui.dialog.ImportDialog;
import com.echo.ui.dialog.ViewSettingsDialog;
import com.echo.ui.elements.HoverButton;
import com.echo.ui.filter.FilterSidebar;
import com.echo.ui.help.PageContentBuilder.HelpPage;

/**
 * Main application window.
 * Contains the roster table, sidebar, and control buttons.
 */
public class MainWindow extends JFrame {
    private final RosterService rosterService;

    private EnhancedRoster currentRoster;
    private FilterManager filterManager;

    private final JPanel sidebarPanel;
    private final RosterTable rosterTable;
    private final JPanel controlPanel;

    /**
     * Creates a new MainWindow with the given roster service.
     *
     * @param rosterService The service for managing rosters
     */
    public MainWindow(RosterService rosterService) {
        this.rosterService = rosterService;

        // Set up the window
        setTitle(HappyCamper.NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Create components
        sidebarPanel = new JPanel(new BorderLayout());
        // DialogUtils.fixSize(sidebarPanel, new Dimension(FilterSidebar.PREFERRED_WIDTH, 0));
        sidebarPanel.setPreferredSize(new Dimension(FilterSidebar.PREFERRED_WIDTH, 0));

        rosterTable = new RosterTable();

        controlPanel = createControlPanel();

        // Create welcome panel
        JPanel welcomePanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("<html><div style='text-align: center;'><h2>Welcome to "+HappyCamper.NAME_VERSION+"</h2>" +
                "<p>Click 'Import' to get started, or click 'Help' for more instructions.</p></div></html>");
        welcomeLabel.setHorizontalAlignment(JLabel.CENTER);
        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);

        // Create split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, rosterTable);
        splitPane.setDividerLocation(FilterSidebar.PREFERRED_WIDTH);

        // Set up layout
        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.NORTH);
        add(welcomePanel, BorderLayout.CENTER); // Start with welcome panel

        // Initially hide the roster table
        rosterTable.setVisible(false);
        sidebarPanel.setVisible(false);
    }

    /**
     * Creates the control panel with buttons.
     *
     * @return The control panel
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton importButton = new HoverButton("Import");
        importButton.addActionListener(this::handleImport);

        JButton exportButton = new HoverButton("Export");
        exportButton.addActionListener(this::handleExport);

        JButton viewSettingsButton = new HoverButton("View Settings");
        viewSettingsButton.addActionListener(this::handleViewSettings);

        JButton columnVisibilityButton = new HoverButton("Column Visibility");
        columnVisibilityButton.addActionListener(this::handleColumnVisibility);

        JButton tutorialButton = new HoverButton("Help");
        tutorialButton.addActionListener(this::handleTutorial);

        panel.add(importButton);
        panel.add(exportButton);
        panel.add(viewSettingsButton);
        panel.add(columnVisibilityButton);
        panel.add(tutorialButton);

        return panel;
    }

    /**
     * Sets the current roster and updates the UI.
     *
     * @param roster The roster to display
     */
    public void setRoster(EnhancedRoster roster) {
        this.currentRoster = roster;

        // Create filter manager and set up filters
        //System.out.println("MainWindow.setRoster: Creating filter manager");
        filterManager = new FilterManager();
        filterManager.createFiltersForRoster(roster);
        //System.out.println("MainWindow.setRoster: Filter manager created with " + filterManager.getFilterCount() + " filters");

        // Reset column visibility dialog's cached settings, ensuring it matches the new roster
        ColumnVisibilityDialog.resetCachedSettings();

        // Reset header visibility to default values
        // First, explicitly set all headers to false
        for (String header : roster.getAllHeaders()) {
            roster.setHeaderVisibility(header, false);
        }

        // Then reset to default values
        roster.resetHeaderVisibility();

        // Set roster in table
        rosterTable.setRoster(roster, filterManager);

        // Create and add filter sidebar
        //System.out.println("MainWindow.setRoster: Creating filter sidebar");
        sidebarPanel.removeAll();
        FilterSidebar filterSidebar = new FilterSidebar(roster, filterManager);
        sidebarPanel.add(filterSidebar, BorderLayout.CENTER);

        // Update filter panels
        //System.out.println("MainWindow.setRoster: Updating filter panels");
        filterSidebar.updateFilterPanels();

        // Make sure the sidebar is visible
        sidebarPanel.revalidate();
        sidebarPanel.repaint();

        // Show the split pane and hide the welcome panel
        Component centerComponent = ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (!(centerComponent instanceof JSplitPane)) {
            // Remove the welcome panel
            getContentPane().remove(centerComponent);

            // Create split pane
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, rosterTable);
            splitPane.setDividerLocation(FilterSidebar.PREFERRED_WIDTH);

            // Add the split pane
            add(splitPane, BorderLayout.CENTER);
        }

        // Make components visible
        rosterTable.setVisible(true);
        sidebarPanel.setVisible(true);

        // Refresh UI
        revalidate();
        repaint();
    }

    /**
     * Handles the import button action.
     *
     * @param event The action event
     */
    private void handleImport(ActionEvent event) {
        ImportDialog dialog = new ImportDialog(this, rosterService);
        dialog.showDialog();

        if (dialog.isImportSuccessful()) {
            setRoster(dialog.getImportedRoster());
        }
    }

    public void automateImport(TestPreset preset){
        ImportDialog dialog = new ImportDialog(this, rosterService);

        dialog.automateSelection(preset.getCamperFile(),preset.getActivityFile(),preset.getFeatures());
        if (dialog.isImportSuccessful()) {
            setRoster(dialog.getImportedRoster());
        }
    }

    /**
     * Handles the export button action.
     *
     * @param event The action event
     */
    private void handleExport(ActionEvent event) {
        if (currentRoster == null) {
            JOptionPane.showMessageDialog(this, "No roster to export", "Needs Roster", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Show export dialog
        ExportDialog exportDialog = new ExportDialog(this, rosterService, currentRoster, filterManager);
        exportDialog.showDialog();

        // If export was confirmed, perform the export
        if (exportDialog.isExportConfirmed()) {
            try {
                exportDialog.performExport();
                JOptionPane.showMessageDialog(this, "Export successful", "Export", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                // Log the error instead of printing stack trace
                System.err.println("Export error: " + ex.getMessage());
            }
        }
    }

    /**
     * Handles the view settings button action.
     */
    private void handleViewSettings(ActionEvent event) {
        if (currentRoster == null) {
            JOptionPane.showMessageDialog(this, "No roster to apply settings to.\nUse 'Import' to load a roster.", "Needs Roster", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Show the view settings dialog
        ViewSettingsDialog viewSettingsDialog = new ViewSettingsDialog(this, rosterService.getViewSettings());
        viewSettingsDialog.showDialog();

        // If settings were confirmed, refresh the table to apply the new settings
        if (viewSettingsDialog.isSettingsConfirmed() && currentRoster != null) {
            rosterTable.repaint();
        }
    }

    /**
     * Handles the column visibility button action.
     */
    private void handleColumnVisibility(ActionEvent event) {
        if (currentRoster == null) {
            JOptionPane.showMessageDialog(this, "No roster to apply settings to.\nUse 'Import' to load a roster.", "Needs Roster", JOptionPane.ERROR_MESSAGE);
            return;
        }

        rosterTable.showColumnVisibilityDialog();
    }

    /**
     * Handles the tutorial button action, creating a help dialog to give the user context
     */
    private void handleTutorial(ActionEvent event) {
        HelpPage helpPage = HelpPage.WELCOME;
        HelpDialog helpDialog = new HelpDialog(this, helpPage);
        helpDialog.showDialog();
    }
}
