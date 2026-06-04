package com.echo.ui.selector;

import java.awt.Component;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.echo.ui.dialog.DialogUtils;
import com.echo.ui.elements.HoverRadioButton;

/**
 * BooleanSelector facilitates gathering boolean inputs in an input dialog.
 * This class stores a boolean value and builds a panel that allows the user to update that value.
 */
public class BooleanSelector extends InputSelector<Boolean> {
    private boolean currentValue;
    private final String trueText;
    private final String falseText;

    private ButtonGroup buttonGroup;
    private JRadioButton trueButton;
    private JRadioButton falseButton;

    /**
     * Constructor establishes default value and text for the displayed selector.
     *
     * @param defaultValue Default boolean value represented by this selector
     * @param title Label text at the top of selector box
     * @param trueText Label text corresponding to true option
     * @param falseText Label text corresponding to false option
     */
    public BooleanSelector(boolean defaultValue, String title, String trueText, String falseText) {
        super(title);
        this.currentValue = defaultValue;
        this.trueText = trueText;
        this.falseText = falseText;
    }

    @Override
    protected void buildSelectorPanel(JPanel panel) {
        // Create a panel for the radio buttons using our helper method
        JPanel radioPanel = DialogUtils.createAlignedBoxPanel();

        // Create radio buttons
        buttonGroup = new ButtonGroup();

        trueButton = new HoverRadioButton(trueText);
        trueButton.setSelected(currentValue);
        trueButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        trueButton.addActionListener(e -> {
            currentValue = true;
            notifyUpdateCallback();
        });

        falseButton = new HoverRadioButton(falseText);
        falseButton.setSelected(!currentValue);
        falseButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        falseButton.addActionListener(e -> {
            currentValue = false;
            notifyUpdateCallback();
        });

        // Add buttons to group so they can toggle the other off when clicked
        buttonGroup.add(trueButton);
        buttonGroup.add(falseButton);

        // Link buttons for circular navigation with enter key
        ((HoverRadioButton)trueButton).linkNext(falseButton);
        ((HoverRadioButton)falseButton).linkNext(trueButton);

        // Add buttons to the panel
        radioPanel.add(trueButton);
        radioPanel.add(falseButton);

        // Add a small amount of bottom padding
        DialogUtils.addVerticalSpacing(radioPanel, 5);

        // Set a reasonable height
        // this.componentHeight = 75; // Enough for two radio buttons plus padding

        panel.add(radioPanel);
    }

    @Override
    public Boolean getValue() {
        return currentValue;
    }

    @Override
    public void setValue(Boolean value) {
        if (value == null) {
            return;
        }

        this.currentValue = value;
        if (trueButton != null && falseButton != null) {
            trueButton.setSelected(value);
            falseButton.setSelected(!value);
        }
    }

    @Override
    public boolean hasSelection() {
        // Boolean selector always has a selection
        return true;
    }
}
