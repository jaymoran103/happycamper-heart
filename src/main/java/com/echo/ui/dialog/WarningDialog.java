package com.echo.ui.dialog;

import java.awt.Window;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JPanel;

import com.echo.logging.RosterWarning;
import com.echo.logging.RosterWarning.WarningType;
import com.echo.logging.WarningManager;

/**
 * WarningDialog extends CardDialog to display a set of caught RosterWarnings, sorted by WarningType
 */
public class WarningDialog extends CardDialog{

    private Map<WarningType, ArrayList<RosterWarning>> warningLog;
    private Boolean checkboxChoice;

    /**
     * Constructor sets up warning panels, ensures the first is shown, and sets the dialog title to indicate the total number or warning types caught
     * @param _parentWindow Parent window relevant to dialog setup
     * @param _warningManager Log that provides warnings to display
     */
    public WarningDialog(Window _parentWindow,WarningManager _warningManager){
        super(_parentWindow, true, new String[]{"Try Other Files","Back","Next","Continue"});
        warningLog = _warningManager.getWarningLog();

        String titleString = warningLog.size()==1? "Warning" : Integer.toString(warningLog.size())+" Warnings";
        setTitle(titleString);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        setupWarningPanels();
        switchStateIfAllowed(0);

        setLocationRelativeTo(this.getParent());
        setAlwaysOnTop(true);

        requestFocus();
        repaint();
        revalidate();
    }

    

    /**
     * Assigns the mainPanel a cardlayout and adds a warning panel for each warning type in warningLog
     */
    private void setupWarningPanels(){

        //Get dialog main panel, assign CardLayout
        JPanel mainPanel = getMainPanel();
        mainPanel.setLayout(cardLayout);

        //For each list of warnings (sorted by type) create a panel based on type, storing in the LinkedHashMap and adding to the CardLayout
        for (ArrayList<RosterWarning> warningSet:warningLog.values()){
            //Based on type, create a basic panel or one with a checkbox
            // WarningType type = warningSet.get(0).getType();
            // WarningPanel panel = type.equals(WarningType.CAMPER_NOT_FOUND) 
            //                         ? new CheckboxWarningPanel(this,warningSet)                                                  
            //                         : new WarningPanel(this,warningSet);
            WarningDialogPanel panel = new WarningDialogPanel(this,warningSet);
            addCard(panel);

        }
    }

    /**
     * Updates choice boolean for dialogs that return a user's decision for a warning.
     * @param choice boolean indicating user choice
     */
    public void setUserChoice(boolean choice){
        checkboxChoice = choice;
    }
    
    /**
     * Returns user's decision, indicated by setUserChoice
     * @return boolean indicating user's last selection
     */
    public Boolean getCheckboxDecision(){
        return checkboxChoice;
    }

    
    
}

