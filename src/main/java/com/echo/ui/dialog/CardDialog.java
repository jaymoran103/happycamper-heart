package com.echo.ui.dialog;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.LinkedHashMap;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.echo.ui.elements.CancelButton;
import com.echo.ui.elements.HoverButton;



/**
 * CardDialog extends the DialogBase to feature a cardLayout in its base panel, navigated through by buttons in the bottom panel.
 */
public abstract class CardDialog extends DialogBase{

    /**
     * Enum specifies action to take upon reaching the end of a set of cards
     * EXIT updates the 'next' button to indicate that the panel will close if clicked again.
     * STOP grays the button out, so the 'cancel'button alone can close the dialog.
     */
    public enum EndAction{
        EXIT,STOP
    }

    protected EndAction endAction = EndAction.EXIT;

    protected final String BUTTONTEXT_RETURN;
    protected final String BUTTONTEXT_BACK;
    protected final String BUTTONTEXT_NEXT;
    protected final String BUTTONTEXT_CONTINUE;

    public CardLayout cardLayout;
    public LinkedHashMap<String,JComponent> displayCards;
    public int currentIndex = 0;

    private boolean selectedContinue = false;



    /**
     * Constructor passes on basic dialog arguments and sets Strings indicating button labels
     * @param parentWindow JFrame indicating dialog's default position and window dependence
     * @param modal boolean indicating modal status
     * @param buttonLabels String[] indicating text for each button's label
     */
    public CardDialog(Window parentWindow, Boolean modal, String[] buttonLabels) {
        super(parentWindow, modal, DialogConstants.DIALOG_WIDTH_STANDARD);
        //FUTURE: Use enums, a wrapper class, or separate string args
        BUTTONTEXT_RETURN = buttonLabels[0];
        BUTTONTEXT_BACK = buttonLabels[1];
        BUTTONTEXT_NEXT = buttonLabels[2];
        BUTTONTEXT_CONTINUE = buttonLabels[3];

        displayCards = new LinkedHashMap<>();
        cardLayout = new CardLayout();
        getMainPanel().setLayout(cardLayout);
        setupBottomPanel();
        // switchStateIfAllowed(0);
    }

//SETUP METHODS

    /**
     * Child instances use addCard to add to the set of displayed panels.
     * Added panels are mapped to a string indicating their index, and added to a cardLayout keyed by said string.
     * @param panel panel to add to the dialog
     */
    protected void addCard(JComponent panel){
        //Add panel and index string to Map and Layout
        String indexString = Integer.toString(displayCards.size());
        displayCards.put(indexString,panel);
        getMainPanel().add(panel,indexString);
    }

    /**
     * Modifies the parent's bottom panel to feature buttons that navigate through the supplied cards.
     */
    protected void setupBottomPanel(){
        JPanel bottomPanel = getBottomPanel();

        //Create navigation buttons and add listeners
        cancelButton = new CancelButton(BUTTONTEXT_RETURN);
        cancelButton.addActionListener(a -> onClick_cancel());

        backButton = new HoverButton(BUTTONTEXT_BACK);
        backButton.addActionListener(a -> onClick_back());

        nextButton = new HoverButton(BUTTONTEXT_NEXT);
        nextButton.addActionListener(a -> onClick_next());

        //Create spacers
        JPanel middleSpacer = new JPanel();
        middleSpacer.setBackground(DialogConstants.DIALOG_COLOR_BOTTOM);
        middleSpacer.setMinimumSize(new Dimension(10,50));
        middleSpacer.setPreferredSize(new Dimension(10,50));

        JPanel leftSpacer = new JPanel();
        leftSpacer.setBackground(DialogConstants.DIALOG_COLOR_BOTTOM);
        leftSpacer.setMaximumSize(new Dimension(30,30));
        leftSpacer.setPreferredSize(new Dimension(30,30));

        JPanel rightSpacer = new JPanel();
        rightSpacer.setBackground(DialogConstants.DIALOG_COLOR_BOTTOM);
        rightSpacer.setMaximumSize(new Dimension(25,50));
        rightSpacer.setPreferredSize(new Dimension(25,50));


        //Set layout and add buttons and spacers sequentially
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));

        bottomPanel.add(leftSpacer);
        bottomPanel.add(cancelButton);

        bottomPanel.add(middleSpacer);

        bottomPanel.add(backButton);
        bottomPanel.add(nextButton);
        bottomPanel.add(rightSpacer);
    }

//BUTTON ACTION LISTENERS

    /**
     * Action listener for the 'next' button.
     * Disposes dialog if the current card is the final one.
     * Otherwise attempts to display to the next card.
     */
    protected void onClick_next(){
        //If the panel range limit has been reached, the user has clicked through all cards.
        if (currentIndex+1 == displayCards.size()){
            selectedContinue = true; //Register that the user chose to proceed before disposing
            this.dispose();
        }
        //Otherwise, another panel exists and should be switched to.
        else{
            switchStateIfAllowed(currentIndex+1);
        }
    }

    /**
     * Action listener for the 'back' button
     * Attempts to display to the previous card.
     */
    protected void onClick_back(){
        switchStateIfAllowed(currentIndex-1);
    }

    /**
     * Action listener for the 'cancel' button
     * Disposes the dialog, ensuring that selectedContinue indicates that the user cancelled early
     */
    protected void onClick_cancel(){
        selectedContinue = false; //Should be false automatically
        this.dispose();
    }



    /**
     * Attempts to display a target index, updating bottom buttons accordingly
     * @param newIndex index to switch to, pending validation
     */
    protected void switchStateIfAllowed(int newIndex){

        String newIndexString = Integer.toString(newIndex);

        //Check index against valid range
        if (newIndex<0 || newIndex>=displayCards.size()){
            System.out.println("Invalid index - shouldn't be possible to get here");
            return;
        }
        //Ensure index maps to an existing card
        if (!displayCards.containsKey(newIndexString)){
            System.out.println("Panel index not found - shouldn't be possible to get here");
            return;
        }

        //If checks are passed, set currentIndex field to new value
        currentIndex = newIndex;

        //Modify buttons based on index's position in panel range
        updateButtons();

        //Display the panel indicated by the index String
        cardLayout.show(getMainPanel(), newIndexString);
    }


    /**
     * Updates navigation buttons, reflecting current position in card series.
     */
    protected void updateButtons(){
        //Back button is enabled as long as the current card is not the first
        backButton.setEnabled(currentIndex > 0);

        //Next button's text and visibility depend on endAction and position in the card series.
        switch (endAction){
			case EXIT -> {
                //If endAction is EXIT, button should show the 'continue' message for the final card.

                nextButton.setText(
                    (currentIndex < displayCards.size()-1)
                    ? BUTTONTEXT_NEXT
                    : BUTTONTEXT_CONTINUE
                );
            }
			case STOP -> {
                //If endAction is STOP, button should be disabled for the final card.
                nextButton.setEnabled(currentIndex != displayCards.size()-1);
            }
        }
    }

    /**
     * For a CardDialog, hides the backbutton if displayCards doesn't contain multiple cards
     */
    @Override
    public void showDialog(){
        if (displayCards.size()<2){
            getBottomPanel().remove(backButton);
        }
        super.showDialog();
        repaint();
        revalidate();
    }

    /**
     * Checks whether or not the user chose to continue
     * @return true if the user clicked through all cards and exited with the same button, false otherwise
     */
    public boolean checkSelectedContinue(){
        return selectedContinue;
    }
}
