package com.echo.ui.dialog;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import com.echo.ui.elements.HelpButton;

/**
 * Utility methods for dialog components.
 * This class provides helper methods for dialog layout and formatting.
 */
public class DialogUtils {
    // All constants have been moved to DialogConstants class


     /**
     * For each dimension, determines NW corner for this panel by adding the parent's NW corner to half the difference of the elements' sizes
     */
    public static void setDialogLocation(Window parentWindow,Window childWindow){
        childWindow.setLocation(parentWindow.getX() + (parentWindow.getWidth() - childWindow.getWidth())/2,
                                parentWindow.getY() + (parentWindow.getHeight() - childWindow.getHeight())/2
        );
    }

    /**
     * Uses 3 size setting methods to a given dimension, ensuring it stays that size.
     * @param component to modify
     * @param dimension to apply
     */
    public static void fixSize(Component component,Dimension dimension){
        component.setMaximumSize(dimension);
        component.setMinimumSize(dimension);
        component.setPreferredSize(dimension);
    }

    /**
     * Creates a JLabel with HTML-formatted text that wraps at the specified width.
     *
     * @param text The plain text to display
     * @return A JLabel with formatted text
     */
    public static JLabel createFormattedLabel(String text,int pixelWidth) {
        // Use 380px to account for some padding
        String htmlText = DialogUtils.formatHTMLWithLines(text, pixelWidth);
        JLabel label = new JLabel(htmlText);
        return label;
    }

    /**
     * Formats plain text as HTML with automatic line breaks to fit within a specified width.
     * This method preserves existing line breaks and lets HTML handle word wrapping.
     * @param text The plain text to format
     * @param widthInPixels The desired width in pixels
     * @return HTML-formatted text
     */
    public static String formatHTMLWithLines(String text, int pixelWidth) {
        if (text == null || text.isEmpty()) {
            return "<html></html>";
        }

        // First escape any HTML special characters to prevent rendering issues
        String htmlText = text.replace("&", "&amp;")
                            .replace("<", "&lt;")
                            .replace(">", "&gt;");

        // Then convert existing line breaks to <br> tags
        htmlText = htmlText.replace("\n", "<br>");

        // Wrap in HTML tags with width style
        // The width constraint will cause automatic word wrapping
        return String.format("<html><div style='width: %dpx'>%s</div></html>", pixelWidth, htmlText);
    }


    /**
     * Creates a properly aligned component panel with the specified layout.
     * This is a helper method for subclasses to create consistent layouts.
     *
     * @param layout The layout manager to use
     * @return A new panel with proper alignment settings
     */
    public static JPanel createAlignedPanel(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    /**
     * Creates a properly aligned component panel with FlowLayout.LEFT.
     * This is a helper method for subclasses to create consistent layouts.
     *
     * @return A new panel with proper alignment settings
     */
    public static JPanel createAlignedFlowPanel() {
        return createAlignedPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    }

    /**
     * Creates a properly aligned component panel with BoxLayout.Y_AXIS.
     * This is a helper method for subclasses to create consistent layouts.
     *
     * @return A new panel with proper alignment settings
     */
    public static JPanel createAlignedBoxPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    /**
     * Adds vertical spacing to a panel.
     *
     * @param panel The panel to add spacing to
     * @param height The height of the spacing in pixels
     */
    public static void addVerticalSpacing(JPanel panel, int height) {
        panel.add(Box.createRigidArea(new Dimension(0, height)));
    }

    /**
     * Recursively updates the enabled state of all components in a container.
     *
     * @param container The container to update
     * @param enabled Whether the components should be enabled
     */
    public static void updateComponentsEnabledState(Container container, boolean enabled) {
        // Update the container itself
        if (container instanceof JComponent jComponent) {
            jComponent.setEnabled(enabled);
        }

        // Update all components in the container
        for (Component component : container.getComponents()) {
            // Update the component
            component.setEnabled(enabled);

            // Recursively update any containers
            if (component instanceof Container container1) {
                updateComponentsEnabledState(container1, enabled);
            }

            // Special handling for text components - update foreground color
            if (component instanceof JTextComponent jTextComponent) {
                jTextComponent.setForeground(enabled ? DialogConstants.TEXT_COLOR_NORMAL : DialogConstants.TEXT_COLOR_DISABLED);
            }

            // Special handling for labels - update foreground color
            if (component instanceof JLabel jLabel) {
                jLabel.setForeground(enabled ? DialogConstants.TEXT_COLOR_NORMAL : DialogConstants.TEXT_COLOR_DISABLED);
            }
        }
    }


    /**
     * Combines a component with a help button in a lightweight panel.
     * Makes it easy to add a help button to any component, with consistent formatting.
     * 
     * @param otherComponent initial component, likely an already-formatted JLabel
     * @param button HelpButton to add
     * @return simple panel containing the two components in a horizontal FlowLayout
     */
    public static JComponent combineWithHelpButton(JComponent otherComponent,HelpButton button){
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        panel.add(otherComponent);
        panel.add(button);
        panel.setBackground(null);
        return panel;
    }
}
