package com.echo.ui.elements;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.net.URL;

import javax.swing.ImageIcon;

import com.echo.HappyCamper;
import com.echo.ui.dialog.DialogUtils;
import com.echo.ui.dialog.HelpDialog;
import com.echo.ui.help.PageContentBuilder.HelpPage;

/**
 * HelpButton provides a universal source of context in GUI displays, featuring an icon or '?' sign and displaying a help popup when clicked.
 */
public class HelpButton extends HoverButton{

    private static final String IMAGE_PATH = "images/button2.png";
    // private static final String IMAGE_URL = "/images/button2.png";
    
    private final HelpPage helpPage;
    private final int defaultDim = 15;
    private final String fallbackText = "?";

    private ImageIcon fullIcon;
    private ImageIcon scaledIcon;
    
    /**
     * Constructor sets up button basics, gets icon, and sets up button look.
     * @param page Help page enum determining which page to show upon clicking.
     */
    public HelpButton(HelpPage _helpPage){
        helpPage = _helpPage;

        addActionListener(a -> onClick_helpButton());
        setToolTipText("Click for info");

        setIconResource();
        generalSetup(defaultDim);
    }

    /**
     * Updates button look based on given size and null status of the fullIcon
     * @param dim int to apply as X and Y dimensions for button
     */
    public void generalSetup(int dim){
        DialogUtils.fixSize(this, new Dimension(dim,dim));
        if (fullIcon!=null){
            doImageSetup();
        } else {
            nonImageSetup();
        }
    }

    /**
     * Sets up button without image, using default text instead
     */
    private void nonImageSetup(){
        setBackground(Color.blue);
        setText(fallbackText);
    }

    /**
     * Sets up button with image, setting it to a scaled version based on the current size.
     */
    private void doImageSetup(){
        setIcon(getScaledIcon());
    }

    /**
     * Provides a scaled version of the current icon. 
     * @return icon scaled to this button's preferred dimensions.
     */
    private ImageIcon getScaledIcon(){
        int width = getPreferredSize().width;
        int height = getPreferredSize().height;
        if (height==0||width==0){
            width = defaultDim;
            height = defaultDim;
        }

        //Scale and return the icon.
        Image scaledImage = fullIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        scaledIcon = new ImageIcon(scaledImage);
        return scaledIcon;
    }


    /**
     * Uses path field to get the specified icon resource.
     * Updates fullIcon, printing message to console upon failure.
     * fullIcon validity is checked before use, so failure here should lead to a default character replacing the icon.
     */
    private void setIconResource(){
        try {
            URL resource = getClass().getClassLoader().getResource(IMAGE_PATH);
            if (resource != null) {
                fullIcon = new ImageIcon(resource);
            } else {
                System.err.println("Failed to load image: " + IMAGE_PATH);
            }        
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }


    /**
     * onClick method to display help dialog.
     */
    private void onClick_helpButton(){
        HelpDialog helpDialog = new HelpDialog(HappyCamper.accessSingleWindow(),helpPage);
        helpDialog.showDialog();
    }



    // /**
    //  * Alternate setup method using a URL to load image icon
    //  */
    // private void setupIcon(){
    //     URL iconUrl = getClass().getResource(IMAGE_URL);
    //     if (iconUrl!=null){
    //         fullIcon = new ImageIcon(iconUrl);
    //     }
    //     System.err.println("HelpButton failed to load image url for icon");
    // }

}
