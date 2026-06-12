package com.echo.ui.component;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Thin, logic-free status strip docked beneath the roster table (B1).
 *
 * Renders a composed view-state line (see {@link com.echo.service.ViewStateSummary}) on the left and a
 * Reset button on the right. All text composition lives in the non-UI helper; this component only
 * displays the string and forwards the reset click.
 */
public class ViewStatusBar extends JPanel {

    private final JLabel statusLabel = new JLabel();

    /**
     * @param onReset callback invoked when the user clicks Reset (clears search, filters, and sort)
     */
    public ViewStatusBar(Runnable onReset) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        add(statusLabel, BorderLayout.CENTER);

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            if (onReset != null) {
                onReset.run();
            }
        });
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.add(resetButton);
        add(rightPanel, BorderLayout.EAST);
    }

    /**
     * Sets the displayed status text (the composed view-state summary).
     */
    public void setStatusText(String text) {
        statusLabel.setText(text);
    }
}
