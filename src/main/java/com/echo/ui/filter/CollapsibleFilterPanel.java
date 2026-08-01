package com.echo.ui.filter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;

import com.echo.filter.AssertionResult;
import com.echo.filter.FilterManager;

/**
 * A collapsible panel used to display filter options.
 */
public class CollapsibleFilterPanel extends JPanel {

    // Size constants
    private static final int HEADER_HEIGHT = 30;
    private static final int CONTENT_PADDING = 2;

    /**
     * Swing component name for the in-panel claim label, so tests can locate it without depending
     * on layout position. Mirrors {@link AssertionBadge#COMPONENT_NAME}.
     */
    public static final String CLAIM_COMPONENT_NAME = "assertionClaim";

    /**
     * Wrap width for the claim text. Swing's HTML renderer ignores CSS width on body/div — only a
     * table element's width attribute actually wraps — so this is passed to <table width=...>.
     *
     * <p>228px, not the claim's own natural width, because the claim label sits inside several
     * layers of chrome that all eat into the 258px the sidebar viewport actually offers once its
     * vertical scrollbar (15-17px depending on look-and-feel) is subtracted from
     * {@code FilterSidebar.PREFERRED_WIDTH} (275). Horizontal budget:
     *
     * <pre>
     *   claim label                            228
     *   claim box: 1px line border  (x2)         2
     *   claim box: 4px inner padding (x2)        8
     *   content panel: 2px etched border (x2)    4
     *   content panel: 2px padding (x2)          4
     *   this panel: 2px empty border (x2)        4
     *                                          ---
     *   panel preferred width                  250   (measured; 8px under the 258 limit)
     * </pre>
     *
     * <p>The box added in the restyle costs 10px of the width that used to go to text, so this
     * dropped from 240 (which measured 250 + 10 = 260, over budget) to 228. There is room to
     * spare: every current claim still wraps to two lines at any width down to 190, and the
     * longest ("Checks that no camper is assigned an activity they didn't request") only takes a
     * third line below 190. Overflow matters because the sidebar sets
     * {@code HORIZONTAL_SCROLLBAR_NEVER} — too wide is silently clipped, not scrollable.
     * {@code FilterSidebarTest.testAssertionFilterPanelWidthFitsUsableViewport} guards this.
     */
    private static final int CLAIM_WRAP_WIDTH = 228;

    // UI components
    private final JPanel headerPanel;
    private final JPanel contentPanel;
    private JLabel titleLabel;
    private JLabel toggleLabel;
    private AssertionBadge assertionBadge;

    /** The inserted claim block, or null when this panel shows no assertion. */
    private JPanel claimBlock;

    // State. Panels start collapsed: a collapsed panel is just its 30px header, and the header
    // still carries the assertion badge, so a fresh sidebar reads as a compact stack of
    // title + status rows. Kept in sync with the setExpanded(false) call in the constructor.
    private boolean expanded = false;
    private final String title;

    // String tooltip
    private final String GENERIC_TOOLTIP = "Click to expand/collapse";

    /**
     * Creates a new collapsible panel with the given title.
     *
     * @param title The title to display in the header
     */
    public CollapsibleFilterPanel(String title) {
        this.title = title;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Create header panel
        headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Create content panel
        contentPanel = createContentPanel();
        add(contentPanel, BorderLayout.CENTER);

        // Set initial state. setExpanded is the single source of truth for the toggle glyph, the
        // header's etched-border direction and the content panel's visibility, so this one call
        // makes all three agree with the `expanded` field's initialiser above.
        setExpanded(false);
    }


    /**
     * Creates the header panel with title and toggle indicator.
     *
     * @return The configured header panel
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        panel.setBackground(FilterSidebar.HEADER_COLOR);
        panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        // panel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        panel.setPreferredSize(new Dimension(0, HEADER_HEIGHT));
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.setToolTipText(title + " - " + GENERIC_TOOLTIP);

        // Create toggle indicator. "+" matches the collapsed initial state; setExpanded() owns it
        // from then on.
        toggleLabel = new JLabel("+");
        toggleLabel.setFont(toggleLabel.getFont().deriveFont(Font.BOLD,15));
        toggleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 8));

        // Create title label
        titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Add components to panel
        panel.add(toggleLabel);
        panel.add(titleLabel);
        panel.add(Box.createHorizontalGlue());

        // Assertion status badge (prototype): right-aligned after the glue. Added directly to the
        // header — not wrapped — so it stays a direct child; the strut supplies the right gap.
        // See docs/superpowers/specs/2026-07-31-assertion-indicators-design.md section 5a.
        assertionBadge = new AssertionBadge();
        panel.add(assertionBadge);
        panel.add(Box.createHorizontalStrut(8));

        // Add mouse listeners for interactivity
        panel.addMouseListener(new MouseAdapter() {
            // Use mousePressed instead of mouseClicked for better responsiveness
            // when the user is moving the mouse while clicking
            @Override
            public void mousePressed(MouseEvent e) {
                setExpanded(!expanded);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(FilterSidebar.HEADER_COLOR);
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                panel.setBackground(FilterSidebar.HEADER_COLOR_HIGHLIGHT);
            }
        });

        return panel;
    }

    /**
     * Creates the content panel that holds the filter content.
     *
     * @return The configured content panel
     */
    private JPanel createContentPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(FilterSidebar.SIDEBAR_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
            // BorderFactory.createBevelBorder(BevelBorder.LOWERED),

            BorderFactory.createEmptyBorder(CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING)
        ));
        return panel;
    }

    /**
     * Sets whether the panel is expanded or collapsed.
     *
     * @param expanded true to expand, false to collapse
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;

        // Update toggle indicator
        toggleLabel.setText(expanded ? "-" : "+");

        headerPanel.setBorder(BorderFactory.createEtchedBorder(
            expanded ? EtchedBorder.LOWERED : EtchedBorder.RAISED
        ));

        // Show/hide content
        contentPanel.setVisible(expanded);

        // Update the UI
        SwingUtilities.invokeLater(() -> {
            invalidate();
            revalidate();
            repaint();

            // Also update the parent container
            Container parent = getParent();
            if (parent != null) {
                parent.invalidate();
                parent.revalidate();
                parent.repaint();
            }
        });
    }

    /**
     * Checks if the panel is currently expanded.
     *
     * @return true if expanded, false if collapsed
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * Shows this filter's assertion status in the header as a fixed-size tinted badge — a green
     * checkmark when satisfied, the red failure count when not, hidden entirely when the filter is
     * not an assertion — and places the assertion's plain-English claim in a bounded box at the
     * top of the expandable content area, above the checkboxes.
     *
     * @param assertion the assertion outcome; null or a non-applicable result hides the badge and
     *                  removes the claim block
     */
    public void setAssertion(AssertionResult assertion) {
        if (claimBlock != null) {
            contentPanel.remove(claimBlock);
            claimBlock = null;
        }

        if (assertion == null || !assertion.applicable()) {
            assertionBadge.clear();
            headerPanel.setToolTipText(title + " - " + GENERIC_TOOLTIP);
            return;
        }

        assertionBadge.showResult(assertion);

        headerPanel.setToolTipText(assertion.claim() + " — " + assertion.statusText());

        claimBlock = buildClaimBlock(assertion.claim());
        contentPanel.add(claimBlock, 0);
    }

    /**
     * Builds the claim block: the assertion's plain-English claim inside a modest outlined box,
     * sized to sit at the top of the content panel and read as "the assertion, then the controls
     * that isolate the rows it concerns".
     *
     * <p>The box replaced an earlier {@code JSeparator} beneath the claim. The box already
     * separates the explanation from the checkboxes, so keeping the rule as well was two
     * separators doing one job.
     *
     * @param claim the plain-English assertion text
     * @return the block, ready to insert at index 0 of the content panel
     */
    private JPanel buildClaimBlock(String claim) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);
        block.setBorder(BorderFactory.createCompoundBorder(
            // Outer: no top margin (the content panel's own 2px padding already supplies one), a
            // 4px bottom margin so the box does not sit against the first checkbox.
            BorderFactory.createEmptyBorder(0, 0, 4, 0),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FilterSidebar.ASSERTION_CLAIM_BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(3, 4, 3, 4))));
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        // cellpadding=0 cellspacing=0 is load-bearing, not decoration. Swing's HTML renderer
        // defaults a <table> to cellpadding=1 cellspacing=2, which pads the claim on all four
        // sides - 10px of invisible vertical space on a two-line claim, more than the block's own
        // border and padding combined. Measured on the longest claim ("Checks that no camper is
        // assigned an activity they didn't request"): label 38px -> 28px.
        JLabel claimLabel = new JLabel(
            "<html><table width=" + CLAIM_WRAP_WIDTH + " cellpadding=0 cellspacing=0><tr><td>"
                + claim + "</td></tr></table></html>");
        claimLabel.setName(CLAIM_COMPONENT_NAME);
        claimLabel.setFont(claimLabel.getFont().deriveFont(Font.PLAIN, 11f));
        claimLabel.setForeground(FilterSidebar.ASSERTION_CLAIM_COLOR);
        claimLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(claimLabel);
        return block;
    }

    /**
     * Adds a component to the content panel.
     * If the component is another CollapsibleFilterPanel, it will be properly indented and styled as a nested panel.
     *
     * @param component The component to add
     */
    public void addContent(Component component) {
        // If adding a nested collapsible panel, add special styling
        if (component instanceof CollapsibleFilterPanel) {
            // Create a container with left padding for indentation
            JPanel container = new JPanel(new BorderLayout());
            container.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);
            container.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            container.setAlignmentX(Component.LEFT_ALIGNMENT);
            container.add(component, BorderLayout.CENTER);
            contentPanel.add(container);
        } else {
            // contentPanel's other direct child is the claim block (see buildClaimBlock), which is
            // explicitly LEFT_ALIGNMENT. BoxLayout's Y_AXIS perpendicular-axis sizing combines every
            // child's alignmentX via SizeRequirements.getAlignedSizeRequirements(); mixing an
            // unaligned (default 0.5 CENTER) child with a 0.0 LEFT child inflates the computed
            // container width far past any single child's actual width - not a fixed offset, but a
            // blowup that scales with content width (observed: a ~257px-wide panel demanding ~360px).
            // That's what pushed the assertion badge (this filter's very reason for having a claim in
            // the first place) off the visible 275px sidebar. Match the claim block's alignment so
            // every direct child of contentPanel agrees.
            if (component instanceof JComponent jc) {
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            contentPanel.add(component);
        }
    }

    /**
     * Removes all components from the content panel.
     */
    public void clearContent() {
        contentPanel.removeAll();
    }

    /**
     * Notifies that a filter has changed.
     * This method should be called whenever a filter option is changed.
     * It will use the FilterManager to update the table.
     */
    public void notifyFilterChanged() {
        FilterManager.updateTable(this);
    }
}
