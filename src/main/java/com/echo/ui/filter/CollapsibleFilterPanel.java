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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;

import com.echo.filter.FilterManager;

/**
 * A collapsible panel used to display filter options.
 */
public class CollapsibleFilterPanel extends JPanel {

    // Size constants
    private static final int HEADER_HEIGHT = 30;
    private static final int CONTENT_PADDING = 2;

    // UI components
    private final JPanel headerPanel;
    private final JPanel contentPanel;
    private JLabel titleLabel;
    private JLabel toggleLabel;

    // State
    private boolean expanded = true;
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

        // Set initial state
        setExpanded(true);
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

        // Create toggle indicator
        toggleLabel = new JLabel("-");
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
            container.add(component, BorderLayout.CENTER);
            contentPanel.add(container);
        } else {
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
