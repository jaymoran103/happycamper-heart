package com.echo.ui.filter;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JLabel;

/**
 * Shared helpers for filter-sidebar tests.
 */
final class FilterTestSupport {

    private FilterTestSupport() {
    }

    /**
     * Finds the assertion indicator label inside a filter panel by its Swing component name.
     *
     * @param container the component tree to search
     * @return the indicator label, or null if the tree has none
     */
    static JLabel findAssertionLabel(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label && "assertionIndicator".equals(label.getName())) {
                return label;
            }
            if (child instanceof Container nested) {
                JLabel found = findAssertionLabel(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Finds the in-panel assertion claim label by its Swing component name.
     *
     * @param container the component tree to search
     * @return the claim label, or null if the tree has none
     */
    static JLabel findClaimLabel(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label && "assertionClaim".equals(label.getName())) {
                return label;
            }
            if (child instanceof Container nested) {
                JLabel found = findClaimLabel(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
