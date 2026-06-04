package com.echo.ui.elements;

import java.awt.Cursor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JRadioButton;

/**
 * HoverRadioButton extends JRadioButton to add a hover effect and enter click functionality
 */
public class HoverRadioButton extends JRadioButton {

    private final boolean DO_ENTER_LISTENER = true;
    private JRadioButton nextButton;

    /**
     * Standard constructor passes label text to parent, calls init.
     * @param text String to display
     */
    public HoverRadioButton(String text){
        super(text);
        init();
    }
    
    /**
     * Init method sets cursor to Cursor.HAND_CURSOR
     */
    private void init() {
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        if (DO_ENTER_LISTENER) {
            setupEnterListener();
        }
    }
    
    /**
     * Set hover effect to a specific cursor type 
     * @param cursorType integer representing a predefined cursor from java.awt.Cursor
     */
    public void setHoverEffect(int cursorType) {
        if (cursorType < 0 || cursorType > 13) {
            setCursor(new Cursor(0));
            throw new IllegalArgumentException("Specified cursorType '" + cursorType + "' falls outside range [0,13]. Setting to 0.");
        }
        setCursor(new Cursor(cursorType));
    }
    
    /**
     * Enables or disables the hover effect
     * @param enable boolean indicating whether the Hand cursor should be enabled
     */
    public void setHoverEffect(boolean enable){
        int value = enable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR;
        setCursor(new Cursor(value));
    }
    
    /**
     * Adds a keyListener, clicking the radio button when Enter is pressed and it is focused
     */
    private void setupEnterListener(){
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (nextButton != null) {
                        nextButton.doClick();
                        nextButton.requestFocusInWindow();
                    } else {
                        ((JRadioButton)e.getSource()).doClick();
                    }
                }
            }
        });
    }

    /**
     * Links the 'next' radio button to be selected when enter is clicked and this button has focus
     * Enables enter key to cycle through a button group, facilitating keyboard navigation
     */
    public void linkNext(JRadioButton nextButton){
        this.nextButton = nextButton;
    }

    
}