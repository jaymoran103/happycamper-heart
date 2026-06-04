package com.echo.ui.dialog;

import java.awt.Color;
import java.awt.Dimension;

/**
 * Constants used throughout the dialog package.
 * This class centralizes all constants related to dialog appearance and behavior.
 */
public class DialogConstants {


    // ===== DIALOG DIMENSIONS =====

    // Dialog width constants
    public static final int DIALOG_WIDTH_STANDARD = 600;
    public static final int DIALOG_WIDTH_NARROW = 450;
    public static final int DIALOG_WIDTH_WIDE = 750;

    // Dialog height constants
    public static final int DIALOG_HEIGHT_STANDARD = 300;
    public static final int DIALOG_HEIGHT_MAX = 400; // Maximum height before scrolling
    public static final int DIALOG_HEIGHT_BOTTOMPANEL = 50;

    // Margin and padding constants
    public static final int DIALOG_MARGIN_WIDTH = 25;
    public static final int COMPONENT_PADDING = 5;
    public static final int COMPONENT_SPACING = 10;

    // Standard dimensions
    public static final Dimension DIALOG_DIMS_STANDARD = new Dimension(DIALOG_WIDTH_STANDARD, DIALOG_HEIGHT_STANDARD);
    public static final Dimension DIALOG_DIMS_BOTTOMPANEL = new Dimension(DIALOG_WIDTH_STANDARD, DIALOG_HEIGHT_BOTTOMPANEL);





    // ===== COMPONENT DIMENSIONS =====

    // Component width constants
    public static final int COMPONENT_WIDTH_STANDARD = DIALOG_WIDTH_STANDARD - (DIALOG_MARGIN_WIDTH * 2);
    public static final int COMPONENT_WIDTH_NARROW = DIALOG_WIDTH_NARROW - (DIALOG_MARGIN_WIDTH * 2);
    public static final int COMPONENT_WIDTH_WIDE = DIALOG_WIDTH_WIDE - (DIALOG_MARGIN_WIDTH * 2);

    // Component height constants
    public static final int COMPONENT_HEIGHT_STANDARD = 85;
    public static final int COMPONENT_HEIGHT_SMALL = 60;
    public static final int COMPONENT_HEIGHT_LARGE = 120;






    // ===== COLORS =====

    // Dialog panel colors
    public static final Color DIALOG_COLOR_MAIN = new Color(240, 240, 240);
    public static final Color DIALOG_COLOR_BOTTOM = new Color(220, 220, 220);

    // Text colors
    public static final Color TEXT_COLOR_NORMAL = Color.BLACK;
    public static final Color TEXT_COLOR_DISABLED = Color.GRAY;
    public static final Color TEXT_COLOR_ERROR = Color.RED;





    // ===== WARNING TABLE CONSTANTS =====

    public static final int WARNINGTABLE_HEIGHT = 100;
    public static final int WARNINGTABLE_WIDTH_MAXIMUM = DIALOG_WIDTH_STANDARD - (DIALOG_MARGIN_WIDTH * 2);
    public static final int WARNINGTABLE_WIDTH_MINIMUM = 300;
    // public static final Dimension WARNINGTABLE_DIMS = new Dimension(WARNINGTABLE_WIDTH, WARNINGTABLE_HEIGHT);

    public static final Color WARNINGTABLE_COLOR_GRID = Color.GRAY;
    public static final Color WARNINGTABLE_COLOR_HEADER = new Color(220, 220, 220);
    public static final Color WARNINGTABLE_COLOR_EVENROW = new Color(245, 245, 245);
    public static final Color WARNINGTABLE_COLOR_ODDROW = new Color(230, 230, 230);





    // ===== BUTTON TEXT =====

    public static final String BUTTON_TEXT_CONTINUE = "Apply";
    public static final String BUTTON_TEXT_CANCEL = "Cancel";
    public static final String BUTTON_TEXT_BROWSE = "Browse";




    // ===== LABEL TEXT =====

    public static final String LABEL_TEXT_NO_FILE = "No file selected";
    public static final String LABEL_TEXT_EMPTY = " ";
}
