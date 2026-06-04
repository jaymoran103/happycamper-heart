package com.echo.ui.selector;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.echo.ui.dialog.DialogUtils;
import com.echo.ui.elements.HoverButton;

/**
 * InputSelector that displays a set of action buttons.
 * This selector doesn't actually store a value but provides buttons that perform actions.
 * 
 * Made as an inputSelector extension for visualconsistency with other selectors, and ease of use in InputsDialog implementations
 */
public class ActionButtonSelector extends InputSelector<Void> {
    private final String[] buttonLabels;
    private final Runnable[] buttonActions;

    /**
     * Creates a new ActionButtonSelector with the specified buttons and actions.
     *
     * @param title The title for this selector
     * @param buttonLabels The labels for the buttons
     * @param buttonActions The actions to perform when the buttons are clicked
     */
    public ActionButtonSelector(String title, String[] buttonLabels, Runnable[] buttonActions) {
        super(title);

        if (buttonLabels.length != buttonActions.length) {
            throw new IllegalArgumentException("Button labels and actions must have the same length");
        }

        this.buttonLabels = buttonLabels;
        this.buttonActions = buttonActions;
    }

    @Override
    protected void buildSelectorPanel(JPanel panel) {
        // Use the helper method to create a properly aligned box panel for vertical stacking
        JPanel buttonPanel = DialogUtils.createAlignedBoxPanel();
        DialogUtils.addVerticalSpacing(buttonPanel, 5);

        // Add buttons to the panel vertically

        for (int i = 0; i < buttonLabels.length; i++) {
            final int index = i;
            JButton button = new HoverButton(buttonLabels[i]);
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.addActionListener(e -> buttonActions[index].run());
            buttonPanel.add(button);
            
        }

        // Add a small amount of bottom padding
        DialogUtils.addVerticalSpacing(buttonPanel, 5);

        // Set a reasonable height based on number of buttons
        int buttonHeight = 35; // Approximate height of a button
        this.componentHeight = (buttonHeight * buttonLabels.length) + 25; // Additional padding

        panel.add(buttonPanel);
    }

    @Override
    public Void getValue() {
        return null; // This selector doesn't store a value
    }

    @Override
    public void setValue(Void value) {
        // Nothing to do
    }

    @Override
    public boolean hasSelection() {
        return true; // This selector always has a "valid" selection
    }
}
