package com.echo.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.Collections;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.echo.HappyCamper;
import com.echo.automation.TestPreset;
import com.echo.domain.EnhancedRoster;
import com.echo.filter.ActivityFilter;
import com.echo.filter.FilterManager;
import com.echo.filter.TextSearchFilter;
import com.echo.service.ActivityCatalog;
import com.echo.service.DemandSort;
import com.echo.service.RosterService;
import com.echo.service.ViewStateSummary;
import com.echo.ui.component.RosterTable;
import com.echo.ui.component.ViewStatusBar;
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

    private static final String ICON_PATH = "app.png";

    private final RosterService rosterService;

    private EnhancedRoster currentRoster;
    private FilterManager filterManager;

    private final JPanel sidebarPanel;
    private final RosterTable rosterTable;
    private final JPanel controlPanel;

    // Control-bar buttons that require a displayed roster; disabled until setRoster() runs.
    private JButton exportButton;
    private JButton viewSettingsButton;
    private JButton columnVisibilityButton;

    // B1: universal search (top-right) + shared view-state status bar (south)
    private final JTextField searchField = new JTextField(16);
    private final JComboBox<String> scopeCombo = new JComboBox<>();
    private final JComboBox<String> demandSortCombo = new JComboBox<>();
    private final ViewStatusBar viewStatusBar = new ViewStatusBar(this::handleReset);
    // Suppresses re-application while the scope / demand-sort combos are repopulated programmatically.
    private boolean suppressSearchEvents = false;
    // B2: the activity currently chosen for demand sort, or null for None.
    private String demandSortActivity = null;

    private static final String DEMAND_SORT_NONE = "None";

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
        setAppIcon();

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
        add(viewStatusBar, BorderLayout.SOUTH);

        // B1: refresh the status bar whenever the visible row set changes
        rosterTable.setOnTableUpdated(this::refreshViewStatus);

        // Initially hide the roster table and status bar (shown once a roster loads)
        rosterTable.setVisible(false);
        sidebarPanel.setVisible(false);
        viewStatusBar.setVisible(false);
    }

    /**
     * Sets the application icon for this window and (where supported) the taskbar/dock,
     * replacing the default Java mascot shown by the windowing system.
     */
    private void setAppIcon() {
        URL resource = getClass().getClassLoader().getResource(ICON_PATH);
        if (resource == null) {
            System.err.println("Failed to load image: " + ICON_PATH);
            return;
        }
        Image icon = new ImageIcon(resource).getImage();

        // Window/title bar and taskbar icon on Windows and Linux. Dialogs inherit it from this frame.
        setIconImage(icon);

        // Dock icon on macOS when launched from a plain jar (the packaged .app already provides it).
        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                taskbar.setIconImage(icon);
            }
        }
    }

    /**
     * Creates the control panel with buttons.
     *
     * @return The control panel
     */
    private JPanel createControlPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton importButton = new HoverButton("Import");
        importButton.addActionListener(this::handleImport);

        exportButton = new HoverButton("Export");
        exportButton.addActionListener(this::handleExport);

        viewSettingsButton = new HoverButton("View Settings");
        viewSettingsButton.addActionListener(this::handleViewSettings);

        columnVisibilityButton = new HoverButton("Column Visibility");
        columnVisibilityButton.addActionListener(this::handleColumnVisibility);

        JButton tutorialButton = new HoverButton("Help");
        tutorialButton.addActionListener(this::handleTutorial);

        // Roster-dependent controls stay disabled until a roster is displayed (see setRoster).
        // Import and Help remain enabled at all times.
        exportButton.setEnabled(false);
        viewSettingsButton.setEnabled(false);
        columnVisibilityButton.setEnabled(false);

        buttonPanel.add(importButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(viewSettingsButton);
        buttonPanel.add(columnVisibilityButton);
        buttonPanel.add(tutorialButton);

        // Buttons on the left, universal search on the right (B1)
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(buttonPanel, BorderLayout.WEST);
        topBar.add(createSearchPanel(), BorderLayout.EAST);
        return topBar;
    }

    /**
     * Builds the top-right universal search controls (B1): a scope selector + text field. Both are
     * inert until a roster is loaded; an empty term passes every camper.
     */
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // B2: demand-sort control, parallel to search (separate from the Activity restrict filter)
        panel.add(new JLabel("Sort by demand:"));
        panel.add(demandSortCombo);
        demandSortCombo.addActionListener(e -> handleDemandSortChange());

        panel.add(new JLabel("Search:"));
        panel.add(scopeCombo);
        panel.add(searchField);

        // Live-search as the user types
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { handleSearchChange(); }
            @Override public void removeUpdate(DocumentEvent e) { handleSearchChange(); }
            @Override public void changedUpdate(DocumentEvent e) { handleSearchChange(); }
        });
        scopeCombo.addActionListener(e -> handleSearchChange());

        return panel;
    }

    /** Pushes the current search field/scope into the always-on TextSearchFilter and refreshes the table. */
    private void handleSearchChange() {
        if (filterManager == null || suppressSearchEvents) {
            return;
        }
        applySearchScope();
        rosterTable.applyFilters(); // fires refreshViewStatus via the table-update callback
    }

    /** Reads the search field + scope combo into the TextSearchFilter (no table refresh). */
    private void applySearchScope() {
        TextSearchFilter search = (TextSearchFilter) filterManager.getFilter("textsearch");
        if (search == null) {
            return;
        }
        search.setSearchTerm(searchField.getText());

        Object scope = scopeCombo.getSelectedItem();
        if (scope == null) {
            return;
        }
        String label = scope.toString();
        if (TextSearchFilter.SCOPE_ALL_COLUMNS.equals(label)) {
            // "All Columns" searches every currently-visible column (respects visibility)
            search.setScopeFields(label, currentRoster.getOrderedVisibleHeaders());
        } else {
            search.setScope(label); // named preset or a single per-column header
        }
    }

    /**
     * Applies the top-bar demand-sort selection (B2): orders campers by preference rank for the chosen
     * activity, or clears the ordering when "None". Independent of the Activity restrict filter.
     */
    private void handleDemandSortChange() {
        if (suppressSearchEvents) {
            return;
        }
        Object selected = demandSortCombo.getSelectedItem();
        String activity = (selected == null || DEMAND_SORT_NONE.equals(selected)) ? null : selected.toString();
        demandSortActivity = activity;

        if (activity == null) {
            rosterTable.setCamperOrdering(null);
        } else {
            rosterTable.setCamperOrdering(DemandSort.comparator(activity));
            // Entering a demand sort clears any active column sort (and its arrow)
            rosterTable.getRowSorter().setSortKeys(Collections.emptyList());
        }
        rosterTable.applyFilters();
    }

    /** Populates the demand-sort selector with None + each catalog activity. */
    private void populateDemandSortCombo() {
        suppressSearchEvents = true;
        try {
            demandSortCombo.removeAllItems();
            demandSortCombo.addItem(DEMAND_SORT_NONE);
            for (String activity : ActivityCatalog.build(currentRoster)) {
                demandSortCombo.addItem(activity);
            }
            demandSortCombo.setSelectedItem(DEMAND_SORT_NONE);
        } finally {
            suppressSearchEvents = false;
        }
        demandSortActivity = null;
        rosterTable.setCamperOrdering(null);
    }

    /** Recomposes and displays the shared view-state status line. */
    private void refreshViewStatus() {
        if (currentRoster == null) {
            return;
        }
        ViewStateSummary summary = new ViewStateSummary()
                .setCounts(rosterTable.getVisibleCount(), rosterTable.getTotalCount())
                .setSelectedCount(rosterTable.getSelectedCount());

        TextSearchFilter search = filterManager == null
                ? null : (TextSearchFilter) filterManager.getFilter("textsearch");
        if (search != null) {
            summary.setSearch(search.getSearchTerm(), search.getScopeLabel());
        }

        // B2: activity restrict segment (the filter) — independent of the demand sort
        ActivityFilter activity = filterManager == null
                ? null : (ActivityFilter) filterManager.getFilter("activity-select");
        if (activity != null && !activity.getSelectedActivities().isEmpty()) {
            summary.setActivity(activity.getActivitySummary(), activity.getRoundLabel(), false);
        }

        // B2: demand-sort segment (the top-bar control)
        if (demandSortActivity != null) {
            summary.setSort("demand for " + demandSortActivity);
        }

        viewStatusBar.setStatusText(summary.compose());
    }

    /**
     * Reset (B1): clears the universal search, restores all sidebar filters to defaults, and drops any
     * active sort, then refreshes the table.
     */
    private void handleReset() {
        if (currentRoster == null || filterManager == null) {
            return;
        }
        searchField.setText(""); // clears the search term via the document listener
        if (scopeCombo.getItemCount() > 0) {
            scopeCombo.setSelectedIndex(0);
        }

        // Clear the demand sort (B2)
        suppressSearchEvents = true;
        demandSortCombo.setSelectedItem(DEMAND_SORT_NONE);
        suppressSearchEvents = false;
        demandSortActivity = null;
        rosterTable.setCamperOrdering(null);

        // Rebuild filters from scratch (resets every sidebar filter to its default state)
        filterManager.createFiltersForRoster(currentRoster);
        rebuildSidebar();

        // Drop any active sort so the header arrow clears too
        rosterTable.getRowSorter().setSortKeys(Collections.emptyList());

        rosterTable.applyFilters();
    }

    /** Rebuilds the filter sidebar panels from the current filter manager state. */
    private void rebuildSidebar() {
        sidebarPanel.removeAll();
        FilterSidebar filterSidebar = new FilterSidebar(currentRoster, filterManager);
        sidebarPanel.add(filterSidebar, BorderLayout.CENTER);
        filterSidebar.updateFilterPanels();
        sidebarPanel.revalidate();
        sidebarPanel.repaint();
    }

    /**
     * Populates the search-scope selector with "All Columns" (default), the two named presets, and each
     * currently-visible column. Preserves the prior selection when that scope still exists; otherwise
     * falls back to "All Columns". Repopulated whenever column visibility changes.
     */
    private void populateScopeCombo() {
        String previous = (String) scopeCombo.getSelectedItem();

        suppressSearchEvents = true; // avoid a flurry of re-applies while rebuilding the model
        try {
            scopeCombo.removeAllItems();
            scopeCombo.addItem(TextSearchFilter.SCOPE_ALL_COLUMNS);
            scopeCombo.addItem(TextSearchFilter.SCOPE_ALL_NAMES);
            scopeCombo.addItem(TextSearchFilter.SCOPE_ASSIGNED_ACTIVITIES);
            for (String header : currentRoster.getOrderedVisibleHeaders()) {
                scopeCombo.addItem(header);
            }
            scopeCombo.setSelectedItem(comboContains(previous) ? previous : TextSearchFilter.SCOPE_ALL_COLUMNS);
        } finally {
            suppressSearchEvents = false;
        }
    }

    private boolean comboContains(String item) {
        if (item == null) {
            return false;
        }
        for (int i = 0; i < scopeCombo.getItemCount(); i++) {
            if (item.equals(scopeCombo.getItemAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the current roster and updates the UI.
     *
     * @param roster The roster to display
     */
    public void setRoster(EnhancedRoster roster) {
        this.currentRoster = roster;

        // A roster is now displayed: enable the controls that act on it.
        exportButton.setEnabled(true);
        viewSettingsButton.setEnabled(true);
        columnVisibilityButton.setEnabled(true);

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

        // Reset any stale search term before wiring the new roster's scope options
        searchField.setText("");

        // Set roster in table
        rosterTable.setRoster(roster, filterManager);

        // Create and add filter sidebar
        //System.out.println("MainWindow.setRoster: Creating filter sidebar");
        rebuildSidebar();

        // B1/B2: populate search scopes + demand-sort activities for this roster, reveal the status bar
        populateScopeCombo();
        populateDemandSortCombo();
        viewStatusBar.setVisible(true);

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

        // Initial status line ("Showing N of N campers")
        refreshViewStatus();

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

        // Column visibility may have changed — refresh the search scopes (and re-apply the active
        // search, since "All Columns" / a per-column scope depends on which columns are visible).
        populateScopeCombo();
        handleSearchChange();
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
