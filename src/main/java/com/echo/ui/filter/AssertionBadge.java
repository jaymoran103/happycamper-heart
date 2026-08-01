package com.echo.ui.filter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import com.echo.filter.AssertionResult;

/**
 * The fixed-size status badge shown at the right of a filter header.
 *
 * Renders as a recessed, tinted well matching the sidebar's raised/shaded surface language: a green
 * checkmark when the filter's assertion is satisfied, the failure count in red when it is not.
 *
 * The box is deliberately the same size in every state — a checkmark, "1", and "12" all occupy an
 * identical 38x22 — so the column of badges down the sidebar stays aligned and reads at a glance.
 *
 * See docs/superpowers/specs/2026-07-31-assertion-indicators-design.md section 5a.
 */
public class AssertionBadge extends JLabel {

    /** Swing component name, so tests can find the badge without depending on layout position. */
    public static final String COMPONENT_NAME = "assertionIndicator";

    private static final Dimension FIXED_SIZE = new Dimension(38, 22);

    /**
     * Creates a hidden badge. Call {@link #showResult(AssertionResult)} to populate it.
     */
    public AssertionBadge() {
        setName(COMPONENT_NAME);
        setHorizontalAlignment(SwingConstants.CENTER);
        setFont(getFont().deriveFont(Font.BOLD, 12f));
        setOpaque(true);
        setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        // All three, because ComponentUI.getMaximumSize() ignores setPreferredSize and returns the
        // UI-computed preferred size instead - without an explicit maximum, BoxLayout clamps the
        // badge down to the glyph's natural height (38x19) rather than stretching it.
        setPreferredSize(FIXED_SIZE);
        setMinimumSize(FIXED_SIZE);
        setMaximumSize(FIXED_SIZE);

        setVisible(false);
    }

    /**
     * Shows the given outcome: a checkmark when satisfied, the failure count when not.
     *
     * @param assertion an applicable assertion result
     */
    public void showResult(AssertionResult assertion) {
        boolean satisfied = assertion.satisfied();
        setText(satisfied ? "✓" : String.valueOf(assertion.failureCount()));
        setForeground(Color.BLACK);
        setBackground(satisfied
            ? FilterSidebar.ASSERTION_PASS_BG
            : FilterSidebar.ASSERTION_FAIL_BG);
        setVisible(true);
    }

    /**
     * Hides the badge, leaving the header exactly as it renders for a filter with no assertion.
     */
    public void clear() {
        setText("");
        setVisible(false);
    }
}
