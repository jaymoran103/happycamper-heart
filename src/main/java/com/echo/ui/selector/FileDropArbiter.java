package com.echo.ui.selector;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Owns the two-tier drag feedback shared by a dialog's file pickers.
 *
 * Tier one: as soon as a file drag enters the dialog, every registered picker shows its dashed
 * frame, so the user can see which widgets accept a file before committing to one. Tier two: the
 * picker actually under the pointer darkens, disambiguating where the file will land.
 *
 * A single arbiter is required rather than per-selector logic because the two tiers are a property
 * of the dialog, not of any one widget: "a drag is somewhere in here" cannot be known by a component
 * that only receives events while the drag is over itself.
 *
 * Note the ceiling: AWT delivers drag events to a component only while the drag is over it, and
 * there is no screen-wide notification for a drag that originated outside the app (a Finder drag has
 * no in-process DragSource). So arming begins when the drag crosses into the dialog, not when the
 * user first picks the file up. That is the earliest moment available to us.
 */
public class FileDropArbiter {

    private final List<FileSelector> selectors = new ArrayList<>();

    /** The selector currently under the pointer, or null when the drag is over dialog background. */
    private FileSelector hovered;

    /** True while a drag is known to be somewhere inside the dialog. */
    private boolean armed;

    /** True while a disarm is queued; see {@link #exit()}. */
    private boolean clearPending;

    /**
     * Puts a selector under this arbiter's control. Registering twice is harmless.
     *
     * @param selector The picker to include in the shared feedback
     */
    public void register(FileSelector selector) {
        if (!selectors.contains(selector)) {
            selectors.add(selector);
        }
    }

    /**
     * Reports that a file drag is over the given selector, or over dialog background when null.
     *
     * @param target The selector under the pointer, or null for "in the dialog but not on a picker"
     */
    void enter(FileSelector target) {
        clearPending = false;
        armed = true;
        hovered = target;
        apply();
    }

    /**
     * Reports that a drag left one of the reporting components, deferred by one event.
     *
     * Every picker registers its whole subtree, and the dialog registers its background, so moving
     * the pointer anywhere fires an exit on what it left before the enter on what it reached.
     * Disarming immediately would flicker every frame the pointer crosses a component boundary, so
     * the clear is queued and any incoming {@link #enter} cancels it. It therefore only runs when
     * the drag genuinely left the dialog and nothing else claimed it.
     */
    void exit() {
        clearPending = true;
        SwingUtilities.invokeLater(() -> {
            if (clearPending) {
                reset();
            }
        });
    }

    /**
     * Clears all drag feedback immediately.
     */
    public void reset() {
        clearPending = false;
        armed = false;
        hovered = null;
        apply();
    }

    /**
     * Pushes the current state out to every registered picker.
     */
    private void apply() {
        for (FileSelector selector : selectors) {
            FileSelector.DragState state;
            if (!armed) {
                state = FileSelector.DragState.IDLE;
            } else if (selector == hovered) {
                state = FileSelector.DragState.HOVERED;
            } else {
                state = FileSelector.DragState.ARMED;
            }
            selector.setDragState(state);
        }
    }

    /**
     * Watches a dialog container so that arming covers the whole dialog, not just the pickers.
     *
     * Without this, dragging over the gap between two pickers would disarm both. The sentinel claims
     * those in-between areas as "still in the dialog, but not on a target" - which is exactly tier
     * one with no tier two.
     *
     * Also installs a plain mouse-motion listener as a self-healing net: if a drag is abandoned
     * outside the window and the platform never delivers the closing dragExit, the feedback would
     * otherwise stay stuck on. Ordinary mouse motion is not delivered during a native drag, so
     * seeing any means no drag is in progress and the state can safely be cleared. This cannot
     * misfire mid-drag, unlike keying off window focus.
     *
     * Skipped when headless, where {@link DropTarget}'s constructor throws by contract.
     *
     * @param container The dialog's main panel
     */
    public void installSentinel(JComponent container) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        installBackgroundTarget(container);

        container.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (armed) {
                    reset();
                }
            }
        });
    }

    /**
     * Registers the container and any descendant that no picker already claimed.
     *
     * Stops at anything that already has a drop target: that means a picker owns the region, and
     * claiming its interior would make hovering the picker report as background - so it would arm
     * instead of darkening, and its hint text would revert mid-hover. Pickers install across their
     * whole subtree precisely so this walk stops at their edge.
     *
     * @param component The subtree root to claim for background arming
     */
    private void installBackgroundTarget(Component component) {
        if (component instanceof JComponent jComponent && jComponent.getDropTarget() != null) {
            return;
        }

        if (component instanceof JComponent jComponent) {
            new DropTarget(jComponent, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
                @Override public void dragEnter(DropTargetDragEvent dtde) { updateDrag(dtde); }
                @Override public void dragOver(DropTargetDragEvent dtde) { updateDrag(dtde); }
                @Override public void dragExit(DropTargetEvent dte) { exit(); }

                @Override
                public void drop(DropTargetDropEvent dtde) {
                    // Background is not a real target - clear the feedback and decline
                    reset();
                    dtde.rejectDrop();
                }

                private void updateDrag(DropTargetDragEvent dtde) {
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        // Accepted so the cursor stays a copy cursor across the whole dialog, but
                        // reported with no target: tier one only.
                        dtde.acceptDrag(DnDConstants.ACTION_COPY);
                        enter(null);
                    } else {
                        dtde.rejectDrag();
                    }
                }
            });
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installBackgroundTarget(child);
            }
        }
    }
}
