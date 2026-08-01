package com.echo.ui.filter;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    @DisplayName("A violated assertion shows the failure count in the fail color")
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
        JLabel indicator = findAssertionLabel(panel);
        Dimension satisfied = indicator.getPreferredSize();
        Dimension satisfiedMax = indicator.getMaximumSize();

        panel.setAssertion(AssertionResult.of(7, "camper", "Everything holds"));
        Dimension singleDigit = findAssertionLabel(panel).getPreferredSize();

        panel.setAssertion(AssertionResult.of(12, "camper", "Everything holds"));
        Dimension doubleDigit = findAssertionLabel(panel).getPreferredSize();

        assertEquals(satisfied, singleDigit);
        assertEquals(satisfied, doubleDigit);
        assertEquals(new Dimension(38, 22), satisfied);

        // getMaximumSize() matters as much as getPreferredSize(): BoxLayout honours the maximum,
        // and ComponentUI.getMaximumSize() ignores setPreferredSize entirely, so dropping
        // setMaximumSize renders the badge at 38x19 (the glyph's natural height) at a shifted y
        // while a preferred-size-only assertion here would stay green.
        assertEquals(new Dimension(38, 22), satisfiedMax);
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

    /**
     * Regression test for the alignmentX mismatch found in visual verification
     * (2026-07-31 assertion badge redesign, task 3): {@code buildClaimBlock()} sets
     * {@code LEFT_ALIGNMENT} (0.0) on the claim block it inserts into {@code contentPanel}, but
     * every real sibling - the checkbox rows every filter adds via {@code addContent()}, e.g.
     * through {@code FilterPanelFactory} - used to default to {@code JComponent}'s alignment of
     * 0.5 (CENTER). {@code BoxLayout}'s Y_AXIS perpendicular-axis sizing
     * ({@code SizeRequirements.getAlignedSizeRequirements}) combines every child's alignment into
     * one shared ascent+descent span; mixing 0.0 and 0.5 inflates the computed container width far
     * past any child's real size - independent of the actual claim/checkbox content. In the real
     * sidebar this pushed a ~257px filter panel to ~360px preferred width, carrying every
     * assertion badge off the visible 275px sidebar entirely, and every unit test still passed:
     * every existing claim test here calls {@code setAssertion()} on a bare panel with no sibling
     * content, so none of them ever built the two-sibling case where the mismatch bites.
     *
     * <p>This test also closes a second, previously-open gap: nothing asserted that the claim
     * block sits at index 0 of the content panel, ahead of sibling content. A future change that
     * appended the claim below the checkboxes would have passed every other test here while
     * inverting the design's core reading order - the assertion, then the controls that isolate
     * the rows it concerns.
     *
     * <p>The 300px bound is deliberately loose: correct layout for this bare test panel (one
     * short checkbox row plus one claim block, no real sidebar chrome) measures well under it,
     * while the alignmentX mismatch inflates the total independent of how small the content
     * actually is - see the class javadoc above for the real-sidebar numbers this reproduces.
     */
    @Test
    @DisplayName("Claim block leads real sibling content, and the mismatch that broke this stays fixed")
    public void testClaimBlockPrecedesSiblingContentWithoutWidthBlowup() {
        JPanel checkboxRow = new JPanel();
        checkboxRow.add(new JCheckBox("Enable option", true));
        panel.addContent(checkboxRow);

        panel.setAssertion(AssertionResult.of(3, "camper", "Some claim"));

        JLabel claimLabel = findClaimLabel(panel);
        assertNotNull(claimLabel);
        Container claimBlock = claimLabel.getParent();
        Container contentPanel = claimBlock.getParent();

        assertSame(claimBlock, contentPanel.getComponent(0),
            "the claim must be the content panel's first child, ahead of the sibling checkbox "
                + "row - the assertion, then the controls that isolate the rows it concerns");

        int preferredWidth = panel.getPreferredSize().width;
        assertTrue(preferredWidth < 300,
            "panel preferred width should stay well under the 275px sidebar width, but was "
                + preferredWidth + "px - a mixed alignmentX between the claim block and sibling "
                + "content (see CollapsibleFilterPanel#addContent) inflates BoxLayout's computed "
                + "width far past what the actual content needs");
    }

    /** Counts every claim label in the tree, to prove repeat calls do not stack blocks. */
    private int countClaimLabels(java.awt.Container container) {
        int count = 0;
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof JLabel label && CollapsibleFilterPanel.CLAIM_COMPONENT_NAME.equals(label.getName())) {
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
