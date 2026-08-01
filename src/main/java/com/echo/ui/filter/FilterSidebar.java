package com.echo.ui.filter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.echo.domain.EnhancedRoster;
import com.echo.filter.FilterManager;
import com.echo.filter.RosterFilter;

/**
 * Sidebar component that contains all filter panels.
 * Dynamically adds filter panels based on enabled features.
 */
public class FilterSidebar extends JPanel {
    public static final int PREFERRED_WIDTH = 275;

    private final EnhancedRoster roster;
    private final FilterManager filterManager;
    private final JPanel contentPanel;
    private final Map<String, CollapsibleFilterPanel> filterPanels = new HashMap<>();

    //FUTURE - settle on final color scheme, formalize?
    static int basicTone = 220;
    static int headerTone = basicTone - 20;
    static int contentTone = basicTone - 5;

    public static final Color SIDEBAR_COLOR = new Color(basicTone,basicTone,basicTone);
    public static final Color HEADER_COLOR = new Color(headerTone,headerTone,headerTone);
    public static final Color HEADER_COLOR_HIGHLIGHT = new Color(190, 210, 240); //Same as TableColors.SELECTED_EVEN. FUTURE centralize all colors somewhere?
    public static final Color FILTER_COLOR_EXPANDED = new Color(contentTone,contentTone,contentTone);

    // Assertion indicator colors (prototype). Saturated members of the same hue families as the
    // pale badge fills below, used for the glyph text so it reads clearly against the tinted fill.
    public static final Color ASSERTION_PASS_COLOR = new Color(46, 125, 50);
    public static final Color ASSERTION_FAIL_COLOR = new Color(178, 34, 34);

    // Badge fills. Pale tints of the same hue families as the glyph colors above. The tint is what
    // makes status scannable — color covers the whole badge, so the sidebar reads as a column of
    // green and red without the reader parsing digits. Continuity, not novelty:
    // TableColors.FLAGGED_EVEN is already the app's pale-red "something is wrong" row tint.
    public static final Color ASSERTION_PASS_BG = new Color(198, 214, 198);
    public static final Color ASSERTION_FAIL_BG = new Color(228, 198, 198);

    // Near the weight of the surrounding labels — the claim box and the checkboxes' bold weight
    // carry the hierarchy, rather than washing the claim out with low contrast.
    public static final Color ASSERTION_CLAIM_COLOR = new Color(45, 45, 45);

    // Outline of the box around the claim. A flat mid-gray rather than another etched bevel: the
    // claim box sits inside the content panel's own EtchedBorder.LOWERED and one panel up from the
    // header's recessed badge, so a second bevel reads as muddy nested grooves and as a second
    // status chip. A single flat line bounds the explanation without competing with either.
    public static final Color ASSERTION_CLAIM_BORDER_COLOR = new Color(160, 160, 160);

    /**
     * Creates a new FilterSidebar.
     *
     * @param roster The roster to filter
     * @param filterManager The filter manager to use
     */
    public FilterSidebar(EnhancedRoster roster, FilterManager filterManager) {
        this.roster = roster;
        this.filterManager = filterManager;

        setLayout(new BorderLayout());

        // Create content panel with BorderLayout to push components to the top
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        // wrapperPanel.setBackground(Color.red);
        wrapperPanel.setBackground(SIDEBAR_COLOR);

        // Create content panel with vertical box layout
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.blue);


        // Add content panel to the NORTH position of the wrapper to push everything to the top
        wrapperPanel.add(contentPanel, BorderLayout.NORTH);


        // Add wrapper panel to scroll pane
        JScrollPane scrollPane = new JScrollPane(wrapperPanel);
        scrollPane.setPreferredSize(new Dimension(PREFERRED_WIDTH, 0));
        scrollPane.setBorder(null);
        // Backstop for CollapsibleFilterPanel.CLAIM_WRAP_WIDTH: that constant is tuned to a
        // vertical-scrollbar width, but the app sets the system look-and-feel, so the real
        // scrollbar is 15px on Aqua and 17px on Windows. Without this, a wrap-width tuned to one
        // platform's scrollbar can still leave a permanent horizontal scrollbar on the other.
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Adds a filter panel for the given filter.
     *
     * @param filter The filter to add a panel for
     */
    public void addFilterPanel(RosterFilter filter) {
        //If filter doesn't exist, skip by returning
        if (filter == null) {
            return;
        }

        //If filter is already mapped in filterPanels, skip by returning
        if (filterPanels.containsKey(filter.getFilterId())) {
            return;
        }

        //Otherwise, create and add the filter panel
        try {
            // Create panel
            CollapsibleFilterPanel panel = filter.createFilterPanel(roster);

            if (panel != null) {
                filterPanels.put(filter.getFilterId(), panel);
                contentPanel.add(panel);

                // Assertion status is whole-roster and does not change with checkbox state, so it
                // is computed once here, at panel-creation time.
                panel.setAssertion(filter.checkAssertion(roster));
            }

        } catch (Exception e) {
            System.err.println("Error creating filter panel for " + filter.getFilterId() + ": " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                System.err.println(element);
            }
        }
    }

    /**
     * Removes all filter panels.
     */
    public void clearFilterPanels() {
        filterPanels.clear();
        contentPanel.removeAll();
        revalidate();
        repaint();
    }

    /**
     * Gets a filter panel by ID.
     *
     * @param filterId The ID of the filter panel to get
     * @return The filter panel, or null if not found
     */
    public CollapsibleFilterPanel getFilterPanel(String filterId) {
        return filterPanels.get(filterId);
    }

    /**
     * Updates all filter panels.
     */
    public void updateFilterPanels() {

        for (RosterFilter filter : filterManager.getAllFilters()) {
            if (!filterPanels.containsKey(filter.getFilterId())) {
                addFilterPanel(filter);
            }
        }

        revalidate();
        repaint();
    }
}
