package com.echo.ui.component;

import java.awt.Color;

/**
 * Manages consistent color usage throughout the table display.
 * Provides color constants and handles row contrast logic.
 */
public class TableColors {
    // Color adjustment for alternating rows
    private static final int DARKENER = 25;

    // Controls whether alternating row colors are used
    private static boolean alternateShades = true;

    // Controls whether empty data fields are highlighted
    private static boolean highlightEmptyData = false;

    // Base colors
    private static final Color HEADER_COLOR = new Color(200, 200, 200);
    private static final Color GRID_COLOR = Color.gray;

    // Table row colors
    private static final Color TABLE_EVEN = new Color(230, 230, 230);
    private static Color tableOdd;

    // Flagged cell colors
    private static final Color FLAGGED_EVEN = new Color(240, 185, 185);
    private static Color flaggedOdd;

    // Selected cell colors
    private static final Color SELECTED_EVEN = new Color(190, 210, 240);
    private static Color selectedOdd;
    
    static {
        updateOddColors();
    }

    /**
     * Updates all odd-numbered row colors based on the current alternateShades setting.
     */
    private static void updateOddColors() {
        tableOdd = darkenShade(TABLE_EVEN);
        flaggedOdd = darkenShade(FLAGGED_EVEN);
        selectedOdd = darkenShade(SELECTED_EVEN);
    }

    /**
     * Gets a darker shade of the given color if alternating shades are enabled,
     * otherwise returns the original color.
     *
     * @param baseColor The color to get a darker shade of
     * @return A darker shade of the base color if alternating shades are enabled,
     *         otherwise the original color
     */
    private static Color darkenShade(Color baseColor) {
        return alternateShades ?
            new Color(
                baseColor.getRed() - DARKENER,
                baseColor.getGreen() - DARKENER,
                baseColor.getBlue() - DARKENER
        ) : baseColor;
    }

    // Getters for colors
    public static Color getHeaderColor() { return HEADER_COLOR; }
    public static Color getGridColor() { return GRID_COLOR; }
    public static Color getTableEvenColor() { return TABLE_EVEN; }
    public static Color getTableOddColor() { return tableOdd; }
    public static Color getFlaggedEvenColor() { return FLAGGED_EVEN; }
    public static Color getFlaggedOddColor() { return flaggedOdd; }
    public static Color getSelectedEvenColor() { return SELECTED_EVEN; }
    public static Color getSelectedOddColor() { return selectedOdd; }

    /**
     * Sets whether alternating row colors should be used.
     * Updates all odd-numbered row colors when changed.
     *
     * @param alternateShades true to enable alternating row colors
     */
    public static void setAlternateShades(boolean alternateShades) {
        TableColors.alternateShades = alternateShades;
        updateOddColors();
    }

    /**
     * Gets whether alternating row colors are enabled.
     *
     * @return true if alternating row colors are enabled
     */
    public static boolean isAlternateShadesEnabled() {
        return alternateShades;
    }

    /**
     * Sets whether empty data fields should be highlighted.
     *
     * @param highlight true to highlight empty data fields
     */
    public static void setHighlightEmptyData(boolean highlight) {
        TableColors.highlightEmptyData = highlight;
    }

    /**
     * Gets whether empty data fields are highlighted.
     *
     * @return true if empty data fields are highlighted
     */
    public static boolean isHighlightEmptyDataEnabled() {
        return highlightEmptyData;
    }
}

