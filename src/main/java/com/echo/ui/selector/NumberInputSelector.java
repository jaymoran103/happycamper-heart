package com.echo.ui.selector;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.echo.ui.dialog.DialogUtils;

/**
 * InputSelector for numeric values with validation.
 * Allows the user to enter a number within a specified range.
 */
public class NumberInputSelector extends InputSelector<Integer> {
    private JTextField inputField;
    private JLabel errorLabel;
    private final int minValue;
    private final int maxValue;
    private int currentValue;
    private final String unitLabel;
    private final Color errorLabelColor = Color.RED;

    /**
     * Creates a new NumberInputSelector with the specified range.
     *
     * @param title The title for this selector
     * @param defaultValue The default value
     * @param minValue The minimum allowed value
     * @param maxValue The maximum allowed value
     * @param unitLabel Optional unit label to display after the input field (e.g., "px")
     */
    public NumberInputSelector(String title, int defaultValue, int minValue, int maxValue, String unitLabel) {
        super(title);
        this.currentValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.unitLabel = unitLabel;
    }

    /**
     * Creates a new NumberInputSelector with the specified range.
     *
     * @param title The title for this selector
     * @param defaultValue The default value
     * @param minValue The minimum allowed value
     * @param maxValue The maximum allowed value
     */
    public NumberInputSelector(String title, int defaultValue, int minValue, int maxValue) {
        this(title, defaultValue, minValue, maxValue, null);
    }

    @Override
    protected void buildSelectorPanel(JPanel panel) {
        // Create input field with current value
        inputField = new JTextField(String.valueOf(currentValue), 10);

        // Create error label (initially empty)
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(errorLabelColor);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add document listener to validate as user types
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { validateInput(); }

            @Override
            public void removeUpdate(DocumentEvent e) { validateInput(); }

            @Override
            public void changedUpdate(DocumentEvent e) { validateInput(); }
        });

        // Add focus listener to validate when field loses focus
        inputField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateInput();
                // If invalid, reset to last valid value
                if (!hasSelection()) {
                    inputField.setText(String.valueOf(currentValue));
                    errorLabel.setText(" ");
                    notifyUpdateCallback();
                }
            }
        });

        // Add key listener to handle Enter key
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    validateInput();
                    // If invalid, reset to last valid value
                    if (!hasSelection()) {
                        inputField.setText(String.valueOf(currentValue));
                        errorLabel.setText(" ");
                    }
                    notifyUpdateCallback();
                }
            }
        });

        // Create a panel for the input field and unit label with proper alignment
        JPanel inputPanel = DialogUtils.createAlignedFlowPanel();
        inputPanel.add(inputField);

        // Add unit label if provided
        if (unitLabel != null && !unitLabel.isEmpty()) {
            JLabel unitLabelComponent = new JLabel(unitLabel);
            inputPanel.add(unitLabelComponent);
        }

        // Add components to panel with proper alignment
        panel.add(inputPanel);
        DialogUtils.addVerticalSpacing(panel, 5); // Add spacing
        panel.add(errorLabel);

        this.componentHeight = 80;

        // Initial validation
        validateInput();
    }

    /**
     * Validates the current input and updates the error message.
     */
    private void validateInput() {
        try {
            int value = Integer.parseInt(inputField.getText().trim());

            if (value < minValue) {
                errorLabel.setText("Value must be at least " + minValue);
            } else if (value > maxValue) {
                errorLabel.setText("Value must be at most " + maxValue);
            } else {
                // Valid input
                currentValue = value;
                errorLabel.setText(" ");
                notifyUpdateCallback();
                return;
            }
        } catch (NumberFormatException e) {
            errorLabel.setText("Please enter a valid number");
        }

        // If we get here, input is invalid
        notifyUpdateCallback();
    }

    @Override
    public Integer getValue() {
        return currentValue;
    }

    @Override
    public void setValue(Integer value) {
        if (value != null && value >= minValue && value <= maxValue) {
            this.currentValue = value;
            if (inputField != null) {
                inputField.setText(String.valueOf(value));
                validateInput();
            }
        }
    }

    @Override
    public boolean hasSelection() {
        try {
            int value = Integer.parseInt(inputField.getText().trim());
            return value >= minValue && value <= maxValue;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Sets the visibility of this selector.
     *
     * @param visible Whether this selector should be visible
     */
    public void setVisible(boolean visible) {
        if (inputField != null) {
            inputField.getParent().getParent().setVisible(visible);
        }
    }

    /**
     * Sets the enabled state of this selector.
     * When disabled, the input field is grayed out and cannot be edited.
     *
     * @param enabled Whether this selector should be enabled
     */
    @Override
    public void setEnabled(boolean enabled) {
        // Call the parent implementation to handle the panel and title
        super.setEnabled(enabled);

        // Additional handling specific to NumberInputSelector
        if (inputField != null && !enabled) {
            // Clear any error message when disabled
            errorLabel.setText(" ");
        }


        //Restore proper color when re-enabled
        if (enabled){
            errorLabel.setForeground(errorLabelColor);
        }
    }
}
