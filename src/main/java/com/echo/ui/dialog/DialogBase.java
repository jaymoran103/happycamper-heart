package com.echo.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;



/**
 * Inheritance plan (From v1)
 *
 * (Abstract) DialogBase - basic layout and panel setup
 * (Abstract) CardDialog extends DialogBase - Handles a cardlayout in the main panel, buttons on bottom panel to cycle through cards
 * InputsDialog extends DialogBase - sibling to CardDialog, handling a scrollpane (if needed) of inputs required of the user.
 *                                               Currently done fine in ExportDialog, but that code could be reused for the inputs step of the main roster conversion.
 *
 * WarningDialog extends CardDialog - creates WarningPanels representing each error type and passes to parent as cards. Handles one user input boolean
 * HelpDialog extends CardDialog - display some predetermined info for the user, with the ability to show as multiple cards
 * ExportDialog extends InputsDialog - gathers user input, and sets as local fields before beginning export process.
 **/

 /**
  * DialogBase is the parent for a set of dialogs used throughout the roster conversion and view process.
  * This class provides the underlying structure for the dialog, with BorderLayout and basic panel setup handled here
  */
public abstract class DialogBase extends JDialog{

    private JPanel mainPanel;
    private JPanel bottomPanel;

    protected JButton cancelButton;
    protected JButton backButton;
    protected JButton nextButton;

    protected int dialogWidth;


    /**
     * Constructor passes on parent window, modal status, and custom width for the dialog
     * @param parentWindow Window used as reference for dialog position
     * @param modal boolean indicating modal status, converted to enum here
     * @param width The width of the dialog in pixels (defaults to DIALOG_WIDTH_STANDARD)
     */
    public DialogBase(Window parentWindow, boolean modal, int width) {
        super(parentWindow, modal ? JDialog.ModalityType.APPLICATION_MODAL
                                  : JDialog.ModalityType.MODELESS);
        dialogWidth = width;
        setup(parentWindow, width);
    }

    /**
     * Sets up basic panels in CENTER and PAGE_END regions of a BorderLayout with custom width
     * @param parentWindow Window used as reference for dialog position
     * @param width The width of the dialog in pixels
     */
    private void setup(Window parentWindow, int width) {
        setLayout(new BorderLayout());

        createMainPanel();
        add(mainPanel, BorderLayout.CENTER);

        createBottomPanel(width);
        add(bottomPanel, BorderLayout.PAGE_END);

        // Lock down dialog size and location
        setResizable(false);
        setSize(new Dimension(width, DialogConstants.DIALOG_HEIGHT_STANDARD));
        DialogUtils.setDialogLocation(parentWindow, this);

        // setResizable(true);
        // setMinimumSize(PanelConstants.DIALOG_DIMS_STANDARD);
        // setPreferredSize(PanelConstants.DIALOG_DIMS_STANDARD);
    }

    /**
     * Wrapper method sets dialog visible, beginning user input process
     */
    public void showDialog(){
        setVisible(true);
    }

    /**
     * Creates the base for the center panel
     */
    private void createMainPanel(){
        mainPanel = new JPanel();
        mainPanel.setBackground(DialogConstants.DIALOG_COLOR_MAIN);
    }

    /**
     * Creates the base for a bottom panel with custom width
     *
     * @param width The width of the bottom panel in pixels
     */
    private void createBottomPanel(int width) {
        bottomPanel = new JPanel();
        bottomPanel.setBackground(DialogConstants.DIALOG_COLOR_BOTTOM);
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        DialogUtils.fixSize(bottomPanel, new Dimension(width, DialogConstants.DIALOG_HEIGHT_BOTTOMPANEL));
    }

    /**
     * @return JPanel in the center section of the dialog layout, containing the main dialog contents
     */
    public JPanel getMainPanel(){
        return mainPanel;
    }

    /**
     * @return JPanel in the PAGE_END section of the dialog layout, usually containing navigation buttons added by child classes
     */
    public JPanel getBottomPanel(){
        return bottomPanel;
    }

}


