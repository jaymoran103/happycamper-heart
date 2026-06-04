package com.echo.ui.dialog;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.echo.ui.help.PageContentBuilder;
import com.echo.ui.help.PageContentBuilder.HelpPage;

/**
 * HelpDialog displays a helpful message explaining program functionality or context
 */
public class HelpDialog extends CardDialog{
    HelpPage page;
    private final String titleText = "User Guide";


    /**
     * Constructor requires a parent frame as reference, and an enum indicating which help page to build.
     * @param _parent
     * @param _page
     */
    public HelpDialog(Window _parent,HelpPage _page){
        super(_parent, true,new String[]{"Close","Back","Next","Continue"});//FUTURE - Define button next in enum
        page = _page;
        endAction = EndAction.STOP;
        setTitle(titleText);
        setupCards();
        switchStateIfAllowed(0);
        setResizable(true);
    }

    /**
     * Iterates through content given by the help page builder, building as cards and adding them to the dialog.
     */
    private void setupCards(){
        String[] cardContent = PageContentBuilder.getCardStrings(page);
        for (String s:cardContent){
            addCard(buildCard(s));
        }
    }

    /**
     * Builds a card to be added to the dialog. wraps in scrollpane if content exceeds available height
     * @param htmlString String with html formatting, containing panel content.
     * @return Component featuring the given html text, scrollable if necessary
     */
    private JComponent buildCard(String htmlString){
        JLabel label = new JLabel(htmlString);
        
        JComponent container = new JPanel();
        container.add(label);

        label.setAlignmentY(TOP_ALIGNMENT);
        label.setAlignmentX(LEFT_ALIGNMENT);

        int widthLimit = DialogConstants.COMPONENT_WIDTH_STANDARD;
        int labelHeight = checkIdealHeight(label,widthLimit);
        // int heightLimit = DialogConstants.DIALOG_DIMS_STANDARD.height - DialogConstants.DIALOG_DIMS_BOTTOMPANEL.height-100;

        //Leaving conditional scroller logic for posterity
        // if (labelHeight>=heightLimit){
            container = new JScrollPane(container);
            ((JScrollPane) container).getVerticalScrollBar().setCursor(new Cursor(Cursor.HAND_CURSOR));
            container.setAlignmentY(TOP_ALIGNMENT);
        // } 

        label.setPreferredSize(new Dimension(widthLimit, labelHeight+100));//FUTURE - Check this math for extreme sizes
        container.setPreferredSize(new Dimension(widthLimit, labelHeight));//FUTURE - Check this math for extreme sizes
    


        return container;
    }

    //Generic version that takes in an already built component and checks the height.
    public static int checkIdealHeight(JComponent component,int width){
        component.setSize(width,Short.MAX_VALUE);
        return component.getPreferredSize().height;
    }

    /**
     * Checks a text string for the maximum height it would extend 
     * to if given endless vertical space and a width contraint
     * @param text String to test as a JLabel
     * @param width width to constrain to
     * @return height the text fills if given space
     */
    public static int checkIdealHeight(String text,int width){
        JLabel label = new JLabel();
        label.setText(text);
        label.setSize(width,Integer.MAX_VALUE);
        return label.getPreferredSize().height;
    }
}
