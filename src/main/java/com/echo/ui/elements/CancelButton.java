package com.echo.ui.elements;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

import javax.swing.SwingUtilities;

/**
 * CancelButton extends HoverButton to provide an escape key listener
 * Enables user to intuitively close dialogs using the enter key
 */
public class CancelButton extends HoverButton {

    
    /**
     * Standard constructor sets button text and sets up listener
     * @param text String for button to display 
     */
    public CancelButton(String text){
        super(text);
        setupEscListener();
    }
    
    /**
     * Basic constructor expects no arguments, sets up listener.
     */
    public CancelButton(){
        super();
        setupEscListener();
    }

    /**
     * Sets up listener for escape key, enabling user to intuitively close dialogs using the enter key
     */
    private void setupEscListener() {
        // Add global ESC listener when button is shown
        addAncestorListener(new javax.swing.event.AncestorListener() {
            private KeyEventDispatcher escapeDispatcher = null;

            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                if (escapeDispatcher == null) {
                    escapeDispatcher = e -> {
                        if (e.getID() == KeyEvent.KEY_PRESSED && 
                            e.getKeyCode() == KeyEvent.VK_ESCAPE && 
                            isShowing()) {
                            SwingUtilities.invokeLater(() -> doClick());
                            return true;
                        }
                        return false;
                    };
                    KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .addKeyEventDispatcher(escapeDispatcher);
                }
            }

            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {
                if (escapeDispatcher != null) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .removeKeyEventDispatcher(escapeDispatcher);
                    escapeDispatcher = null;
                }
            }

            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });
    }

}
