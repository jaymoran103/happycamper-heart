package com.echo.ui.elements;

import java.awt.Cursor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;



/**
 * HoverButton extends JButton to add a hover effect and enter click functionality
 */
public class HoverButton extends JButton {

    private final boolean DO_ENTER_LISTENER = true;

    /**
     * Standard constructor passes on given text, Calls init.
     * @param text String to display, like any JButton
     */
    public HoverButton(String text){
        super(text);
        init();
    }

    /**
     * Alternate constructor requires no text. Calls init.
     */
    public HoverButton(){
        super();
        init();
    }

    /**
     * Init method currently sets cursor to Cursor.HAND_CURSOR
     */
    private void init(){
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (DO_ENTER_LISTENER){
            setupEnterListener();
        }
    }

    /**
     * Set hover effect to a specific cursor type 
     * @param CURSOR_TYPE integer representing a predefined cursor from java.awt.Cursor.
     * For consistency, Cursor.DEFAULT_CURSOR (0) and Cursor.HAND_CURSOR (12) are the two referenced explictly in this class.
     */
    public void setHoverEffect(int cursorType){
        if (cursorType<0 || cursorType>13){
            setCursor(new Cursor(0));
            throw new IllegalArgumentException("Specified cursorType '"+cursorType+"'' falls outside range [0,13]. Setting to 0.");
        }
        setCursor(new Cursor(cursorType));
    }

    /**
     * Enables or disables the hover effect.
     * @param enable boolean indicating whether or not the Hand cursor should be enabled.
     */
    public void setHoverEffect(boolean enable){
        int value = enable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR;
        setCursor(new Cursor(value));
    }

    /**
     * Adds a keyListener, clicking the button when Enter is pressed and the button is focused.
     */
    private void setupEnterListener(){
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    ((JButton)e.getSource()).doClick();
                }
            }
        });
    }
}
