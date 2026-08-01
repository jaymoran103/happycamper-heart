package com.echo.ui.filter;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.filter.AssertionResult;

import static com.echo.ui.filter.FilterTestSupport.findAssertionLabel;
import static com.echo.ui.filter.FilterTestSupport.findClaimLabel;

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

    @Test
    @DisplayName("A panel with no assertion hides the indicator")
    public void testAssertionIndicatorHiddenByDefault() {
        JLabel indicator = findAssertionLabel(panel);
        assertNotNull(indicator);
        assertFalse(indicator.isVisible());
    }

    @Test
    @DisplayName("A satisfied assertion shows a green checkmark with no count")
    public void testSatisfiedAssertionIndicator() {
        panel.setAssertion(AssertionResult.of(0, "camper", "Everything holds"));

        JLabel indicator = findAssertionLabel(panel);
        assertTrue(indicator.isVisible());
        assertEquals("✓", indicator.getText());
        assertEquals(FilterSidebar.ASSERTION_PASS_COLOR, indicator.getForeground());
    }

    @Test
    @DisplayName("A violated assertion shows a red dot with the failure count")
    public void testViolatedAssertionIndicator() {
        panel.setAssertion(AssertionResult.of(12, "camper", "Everything holds"));

        JLabel indicator = findAssertionLabel(panel);
        assertTrue(indicator.isVisible());
        assertEquals("12", indicator.getText());
        assertEquals(FilterSidebar.ASSERTION_FAIL_COLOR, indicator.getForeground());
    }

    @Test
    @DisplayName("none() hides the indicator again")
    public void testNoneHidesIndicator() {
        panel.setAssertion(AssertionResult.of(3, "camper", "Everything holds"));
        panel.setAssertion(AssertionResult.none());

        assertFalse(findAssertionLabel(panel).isVisible());
    }

    @Test
    @DisplayName("An applicable assertion sets the header tooltip to the claim and status text")
    public void testApplicableAssertionSetsTooltip() {
        panel.setAssertion(AssertionResult.of(3, "camper", "Everything holds"));

        // The assertion label is added directly to the header panel (see
        // CollapsibleFilterPanel#createHeaderPanel), so its parent *is* the header panel that
        // carries the tooltip - no separate tree-walk helper needed.
        JPanel headerPanel = (JPanel) findAssertionLabel(panel).getParent();
        assertNotNull(headerPanel);
        String tooltip = headerPanel.getToolTipText();
        assertTrue(tooltip.contains("Everything holds"));
        assertTrue(tooltip.contains("3 campers fail"));
    }

    @Test
    @DisplayName("none() restores the original header tooltip, not a stale claim string")
    public void testNoneRestoresOriginalTooltip() {
        panel.setAssertion(AssertionResult.of(3, "camper", "Everything holds"));
        panel.setAssertion(AssertionResult.none());

        JPanel headerPanel = (JPanel) findAssertionLabel(panel).getParent();
        assertNotNull(headerPanel);
        assertEquals(TITLE + " - Click to expand/collapse", headerPanel.getToolTipText());
    }

    @Test
    @DisplayName("The badge is the same size whatever it displays")
    public void testBadgeSizeIsFixed() {
        panel.setAssertion(AssertionResult.of(0, "camper", "Everything holds"));
        Dimension satisfied = findAssertionLabel(panel).getPreferredSize();

        panel.setAssertion(AssertionResult.of(7, "camper", "Everything holds"));
        Dimension singleDigit = findAssertionLabel(panel).getPreferredSize();

        panel.setAssertion(AssertionResult.of(12, "camper", "Everything holds"));
        Dimension doubleDigit = findAssertionLabel(panel).getPreferredSize();

        assertEquals(satisfied, singleDigit);
        assertEquals(satisfied, doubleDigit);
        assertEquals(new Dimension(38, 22), satisfied);
    }

    @Test
    @DisplayName("Showing a badge does not grow the header")
    public void testHeaderHeightUnchanged() {
        JLabel indicator = findAssertionLabel(panel);
        JPanel header = (JPanel) indicator.getParent();
        int before = header.getPreferredSize().height;

        panel.setAssertion(AssertionResult.of(12, "camper", "Everything holds"));

        assertEquals(before, header.getPreferredSize().height);
        assertEquals(30, header.getPreferredSize().height);
        // The vacuous version of this test only checked the header's own preferred size, which is
        // pinned unconditionally in createHeaderPanel and can't reflect what the badge does. The
        // real constraint is that the badge fits inside the header, not merely that the constant
        // wasn't edited.
        assertTrue(indicator.getPreferredSize().height < header.getPreferredSize().height);
    }

    @Test
    @DisplayName("An applicable assertion puts its claim in the expandable region")
    public void testClaimAppearsInContent() {
        panel.setAssertion(AssertionResult.of(3, "camper", "Rounds assigned are consistent"));

        JLabel claim = findClaimLabel(panel);
        assertNotNull(claim);
        assertTrue(claim.getText().contains("Rounds assigned are consistent"));
    }

    @Test
    @DisplayName("A non-applicable assertion adds no claim block")
    public void testNoClaimWhenNotApplicable() {
        panel.setAssertion(AssertionResult.none());

        assertNull(findClaimLabel(panel));
    }

    @Test
    @DisplayName("Re-setting an assertion replaces the claim rather than stacking duplicates")
    public void testClaimIsNotDuplicated() {
        panel.setAssertion(AssertionResult.of(3, "camper", "First claim"));
        panel.setAssertion(AssertionResult.of(4, "camper", "Second claim"));

        JLabel claim = findClaimLabel(panel);
        assertNotNull(claim);
        assertTrue(claim.getText().contains("Second claim"));
        assertFalse(claim.getText().contains("First claim"));
        assertEquals(1, countClaimLabels(panel));
    }

    @Test
    @DisplayName("Going from applicable to none removes the claim block")
    public void testClaimRemovedWhenAssertionCleared() {
        panel.setAssertion(AssertionResult.of(3, "camper", "Some claim"));
        assertNotNull(findClaimLabel(panel));

        panel.setAssertion(AssertionResult.none());

        assertNull(findClaimLabel(panel));
    }

    /** Counts every claim label in the tree, to prove repeat calls do not stack blocks. */
    private int countClaimLabels(java.awt.Container container) {
        int count = 0;
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof JLabel label && "assertionClaim".equals(label.getName())) {
                count++;
            }
            if (child instanceof java.awt.Container nested) {
                count += countClaimLabels(nested);
            }
        }
        return count;
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
