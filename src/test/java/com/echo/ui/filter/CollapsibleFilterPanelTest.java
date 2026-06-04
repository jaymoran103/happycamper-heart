package com.echo.ui.filter;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JLabel;
import javax.swing.JPanel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the CollapsibleFilterPanel class.
 */
public class CollapsibleFilterPanelTest {
    private CollapsibleFilterPanel panel;
    private final String TITLE = "Test Filter";

    @BeforeEach
    public void setUp() {
        panel = new CollapsibleFilterPanel(TITLE);
    }

    @Test
    @DisplayName("Test panel creation with title")
    public void testPanelCreationWithTitle() {
        // Verify the panel was created with the correct title
        assertNotNull(panel);

        // Find the title label
        JLabel titleLabel = findTitleLabel(panel);
        assertNotNull(titleLabel);
        assertEquals(TITLE, titleLabel.getText());
    }

    @Test
    @DisplayName("Test adding content")
    public void testAddingContent() {
        // Create a content panel
        JPanel contentPanel = new JPanel();
        JLabel testLabel = new JLabel("Test Content");
        contentPanel.add(testLabel);

        // Add the content panel
        panel.addContent(contentPanel);

        // Verify the content was added
        assertTrue(containsComponent(panel, testLabel));
    }

    @Test
    @DisplayName("Test collapse/expand functionality")
    public void testCollapseExpandFunctionality() {
        // Create a content panel
        JPanel contentPanel = new JPanel();
        JLabel testLabel = new JLabel("Test Content");
        contentPanel.add(testLabel);

        // Add the content panel
        panel.addContent(contentPanel);

        // Verify the content is visible initially
        assertTrue(isContentVisible(panel, testLabel));

        // Toggle the expanded state
        panel.setExpanded(false);

        // Verify the content is hidden
        assertFalse(isContentVisible(panel, testLabel));

        // Toggle the expanded state again
        panel.setExpanded(true);

        // Verify the content is visible again
        assertTrue(isContentVisible(panel, testLabel));
    }

    @Test
    @DisplayName("Test nested panel support")
    public void testNestedPanelSupport() {
        // Create a nested panel
        CollapsibleFilterPanel nestedPanel = new CollapsibleFilterPanel("Nested Panel");
        JLabel nestedLabel = new JLabel("Nested Content");
        nestedPanel.addContent(nestedLabel);

        // Add the nested panel to the main panel
        panel.addContent(nestedPanel);

        // Verify the nested panel was added
        assertTrue(containsComponent(panel, nestedPanel));

        // Verify the nested content is accessible
        assertTrue(containsComponent(panel, nestedLabel));
    }

    /**
     * Helper method to find the title label in the panel.
     */
    private JLabel findTitleLabel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                if (label.getText().equals(TITLE)) {
                    return label;
                }
            } else if (component instanceof Container) {
                JLabel label = findTitleLabel((Container) component);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }

    /**
     * Helper method to check if a component is contained in a container.
     */
    private boolean containsComponent(Container container, Component component) {
        for (Component c : container.getComponents()) {
            if (c == component) {
                return true;
            } else if (c instanceof Container) {
                if (containsComponent((Container) c, component)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper method to check if a component is visible in a container.
     * This checks both the component's visibility and the visibility of all its parent containers.
     */
    private boolean isContentVisible(Container container, Component component) {
        for (Component c : container.getComponents()) {
            if (c == component) {
                // Check if the component itself is visible
                return c.isVisible();
            } else if (c instanceof Container) {
                Container childContainer = (Container) c;
                // Only check inside this container if it's visible
                if (childContainer.isVisible()) {
                    boolean visible = isContentVisible(childContainer, component);
                    if (visible) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
