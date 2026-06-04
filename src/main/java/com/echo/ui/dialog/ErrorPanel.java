package com.echo.ui.dialog;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.echo.logging.RosterException;

/**
 * ErrorPanel displays a set of RosterExceptions corresponding to a single ErrorType, using a JTable to display context for each error instance
 */
public class ErrorPanel extends JPanel {

    public final ErrorDialog parent;
    public final RosterException displayedExeption;

    protected final String generalExplanationText;
    protected final String secondaryExplanationText;

    protected final int wrappingTextWidth = 550; // Width for text wrapping

    protected final Color tableGridColor = DialogConstants.WARNINGTABLE_COLOR_GRID;
    protected final Color tableHeaderColor = DialogConstants.WARNINGTABLE_COLOR_HEADER;
    protected final Color tableEvenColor = DialogConstants.WARNINGTABLE_COLOR_EVENROW;
    protected final Color tableOddColor = DialogConstants.WARNINGTABLE_COLOR_ODDROW;

    /**
     * Constructor sets the text and table fields, then builds the panel.
     *
     * @param parent Parent dialog that displays this panel
     * @param errors List of errors displayed in this panel
     */
    ErrorPanel(ErrorDialog parent, RosterException displayedExeption) {
        this.parent = parent;
        this.displayedExeption = displayedExeption;

        // Set text to show in panel
        generalExplanationText = displayedExeption.getSummary();
        secondaryExplanationText = displayedExeption.getExplanation();

        buildPanel();
    }

    /**
     * Sets up each panel element and adds to a vertical boxlayout.
     */
    protected void buildPanel() {

        setBackground(DialogConstants.DIALOG_COLOR_MAIN);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Explanation text at top
        JLabel explanationLabel = createWrappingLabel(generalExplanationText);
        add(explanationLabel);

        // Secondary explanation above table
        JLabel secondaryExplanationLabel = createWrappingLabel(secondaryExplanationText);
        add(secondaryExplanationLabel);
    }


    protected JLabel createWrappingLabel(String text) {

        String formattedText = DialogUtils.formatHTMLWithLines(text, wrappingTextWidth);
        JLabel wrappingLabel = new JLabel(formattedText);

        int borderWidth = 10;
        wrappingLabel.setBorder(BorderFactory.createEmptyBorder(borderWidth, borderWidth, borderWidth, borderWidth));
        wrappingLabel.setAlignmentX(LEFT_ALIGNMENT);

        return wrappingLabel;
    }

    /**
     * Calculates the preferred height for this panel based on its content.
     *
     * @return The preferred height in pixels
     */
    public int calculatePreferredHeight() {
        // For basic ErrorPanel with just two labels
        int textBorderWidth = 10; // The border width used in createWrappingLabel

        // Count lines in both text fields
        int explanationLines = countTextLines(generalExplanationText, wrappingTextWidth);
        int secondaryLines = countTextLines(secondaryExplanationText, wrappingTextWidth);

        // Calculate height: fixed height per line + borders
        int lineHeight = 20; // Approximate height per line

        return (explanationLines + secondaryLines) * lineHeight + (textBorderWidth * 4) + 20;
    }

    /**
     * Counts the number of lines in text, accounting for both explicit line breaks
     * and estimated line wrapping based on width.
     *
     * @param text The text to analyze
     * @param width The width in pixels for wrapping calculation
     * @return Estimated number of lines
     */
    protected int countTextLines(String text, int width) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Count explicit line breaks (both \n and <br> if present)
        int explicitBreaks = 0;

        // Count \n breaks
        explicitBreaks += text.split("\n").length - 1;

        // Count <br> tags if present (in case HTML is already applied)
        if (text.contains("<br>")) {
            explicitBreaks += text.split("<br>").length - 1;
        }

        // Estimate additional line breaks due to wrapping
        int charsPerLine = width / 8; // Approximate characters per line

        // Remove HTML tags for length calculation if present
        String plainText = text;
        if (plainText.startsWith("<html>")) {
            plainText = plainText.replaceAll("<[^>]*>", "");
        }

        int textLength = plainText.length();
        int wrappingLines = Math.max(0, textLength / charsPerLine);

        return explicitBreaks + wrappingLines + 1; // +1 for the first line
    }
}