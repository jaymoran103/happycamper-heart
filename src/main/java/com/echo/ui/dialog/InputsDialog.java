package com.echo.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import com.echo.ui.elements.CancelButton;
import com.echo.ui.elements.HoverButton;
import com.echo.ui.selector.InputSelector;

/**
 * Dialog for collecting user input through a set of InputSelector components.
 * This class provides a common base for dialogs that need to gather multiple inputs.
 */
public abstract class InputsDialog extends DialogBase {

    protected InputSelector<?>[] selectors;
    protected boolean inputConfirmed = false;

    // Button text constants
    private String BUTTON_TEXT_CONTINUE = "Apply";
    private String BUTTON_TEXT_CANCEL = "Cancel";

    /**
     * Creates a new InputsDialog with the specified selectors and options.
     *
     * @param parent The parent window
     * @param modal Whether the dialog should be modal
     * @param title The dialog title
     * @param selectors The input selectors to display
     * @param width The width of the dialog in pixels (defaults to DIALOG_WIDTH_STANDARD)
     * @param continueText The text to display on the continue button (defaults to "Apply")
     */
    public InputsDialog(Window parent, boolean modal, String title, InputSelector<?>[] selectors, int width, String continueText) {
        super(parent, modal, width);
        this.selectors = selectors;
        this.BUTTON_TEXT_CONTINUE = continueText;

        setTitle(title);

        // Set update callbacks for all selectors
        for (InputSelector<?> selector : selectors) {
            selector.setUpdateCallback(this::updateContinueButton);
        }

        setupMainPanel(width);
        setupBottomPanel();

        pack();
        setLocationRelativeTo(parent);

        updateContinueButton();
    }

    /**
     * Creates a new InputsDialog with the specified selectors.
     *
     * @param parent The parent window
     * @param modal Whether the dialog should be modal
     * @param title The dialog title
     * @param selectors The input selectors to display
     */
    public InputsDialog(Window parent, boolean modal, String title, InputSelector<?>[] selectors) {
        this(parent, modal, title, selectors, DialogConstants.DIALOG_WIDTH_STANDARD, "Apply");
    }

    /**
     * Sets up the main panel with all input selectors and custom width.
     *
     * @param width The width of the panel in pixels
     */
    protected void setupMainPanel(int width) {
        JPanel mainPanel = getMainPanel();
        mainPanel.setLayout(new BorderLayout());

        // Create a wrapper panel to center the content
        JPanel centeringPanel = new JPanel(new BorderLayout());
        centeringPanel.setBackground(DialogConstants.DIALOG_COLOR_MAIN);

        // Create options panel with vertical layout
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        optionsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Add all selectors to the panel
        for (InputSelector<?> selector : selectors) {
            JPanel selectorPanel = selector.createPanel();
            selectorPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            optionsPanel.add(selectorPanel);
            optionsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            DialogUtils.fixSize(selectorPanel, new Dimension(width - DialogConstants.DIALOG_MARGIN_WIDTH*2, selectorPanel.getPreferredSize().height));
        }

        // Calculate the total content height
        int totalContentHeight = 0;
        for (InputSelector<?> selector : selectors) {
            // Get the component height directly from the selector
            totalContentHeight += selector.getComponentHeight() + 10; // Add spacing
        }

        // Add padding
        totalContentHeight += 30;

        // Set a minimum height
        totalContentHeight = Math.max(totalContentHeight, 200);

        // Create scroll pane with automatic scrolling when content exceeds max height
        JScrollPane scrollPane = new JScrollPane(optionsPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Always show vertical scrollbar if needed
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Set preferred size to control dialog dimensions
        int preferredHeight = Math.min(totalContentHeight, DialogConstants.DIALOG_HEIGHT_MAX);
        scrollPane.setPreferredSize(new Dimension(width, preferredHeight));

        // Ensure the options panel has enough height to show all content
        optionsPanel.setPreferredSize(new Dimension(width - 30, totalContentHeight));

        // Make sure the scroll pane will show scrollbars when needed
        scrollPane.setMinimumSize(new Dimension(width - 30, Math.min(300, preferredHeight)));

        // System.out.println("Scroll pane preferred size: " + scrollPane.getPreferredSize());
        // System.out.println("Options panel preferred size: " + optionsPanel.getPreferredSize());

        // Add scroll pane to centering panel
        centeringPanel.add(scrollPane, BorderLayout.CENTER);

        // Add centering panel to main panel
        mainPanel.add(centeringPanel, BorderLayout.CENTER);
    }

    /**
     * Sets up the bottom panel with buttons.
     */
    protected void setupBottomPanel() {
        JPanel bottomPanel = getBottomPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

        // Add cancel button if dialog is modal
        cancelButton = new CancelButton(BUTTON_TEXT_CANCEL);
        cancelButton.addActionListener(e -> onCancelClicked());

        // Add continue button
        nextButton = new HoverButton(BUTTON_TEXT_CONTINUE);
        nextButton.addActionListener(e -> onContinueClicked());

        // Add flexible space between buttons
        bottomPanel.add(cancelButton);
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(nextButton);
    }

    /**
     * Called when the cancel button is clicked.
     * Resets the input confirmed flag and disposes the dialog.
     */
    protected void onCancelClicked() {
        inputConfirmed = false;
        dispose();
    }

    /**
     * Called when the continue button is clicked.
     * Sets the input confirmed flag and disposes the dialog if all inputs are valid.
     */
    protected void onContinueClicked() {
        if (areInputsValid()) {
            inputConfirmed = true;
            updateSelections();
            dispose();
        }
    }

    /**
     * Updates the enabled state of the continue button based on input validity.
     */
    protected void updateContinueButton() {
        if (nextButton != null) {
            nextButton.setEnabled(areInputsValid());
        }
    }

    /**
     * Checks if all inputs are valid.
     *
     * @return true if all selectors have valid selections, false otherwise
     */
    protected boolean areInputsValid() {
        for (InputSelector<?> selector : selectors) {
            if (!selector.hasSelection()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Updates any cached values or settings based on the current selections.
     * Subclasses should override this method to save selections to their settings objects.
     */
    protected void updateSelections() {
        // Default implementation does nothing
        // Subclasses should override to save selections
    }

    /**
     * Checks if the user confirmed the dialog by clicking the continue button.
     *
     * @return true if the user confirmed, false otherwise
     */
    public boolean isInputConfirmed() {
        return inputConfirmed;
    }

    /**
     * Shows the dialog and resets the input confirmed flag.
     */
    @Override
    public void showDialog() {
        inputConfirmed = false;
        super.showDialog();
    }
}
