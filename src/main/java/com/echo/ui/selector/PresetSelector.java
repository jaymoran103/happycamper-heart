package com.echo.ui.selector;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.echo.service.config.ViewPresetService;
import com.echo.service.config.VisibilityResolver;
import com.echo.ui.dialog.DialogUtils;
import com.echo.ui.elements.HoverButton;
import com.echo.ui.elements.HoverRadioButton;

/**
 * PROTOTYPE (throwaway design probe) — redesigned preset manager.
 * Model: radio = selected preset (selecting also LOADS it into the working CheckBoxSelector);
 * applied/modified/default are DERIVED status shown on the row label; all verbs live in a
 * bottom toolbar acting on the selected row. One working draft, so only the selected preset
 * is ever "modified". The panel pre-selects the preset matching the current columns on open,
 * and resizes to the preset count (recomputed on add/delete).
 */
public class PresetSelector extends InputSelector<String> {

    // Vertical budget (px) for the title + 3-row toolbar + padding; rows add on top.
    private static final int CHROME_HEIGHT = 150;
    private static final int ROW_HEIGHT = 26;

    private final ViewPresetService service;
    private final CheckBoxSelector visibilitySelector;
    private final List<String> headers;

    private String selectedName;                 // selected/active preset (radio), or null
    private JPanel listPanel;                     // rows live here (NOT the title-bearing panel)
    private ButtonGroup group;
    private final Map<String, JLabel> rowLabels = new LinkedHashMap<>();
    private final Map<String, HoverRadioButton> rowRadios = new LinkedHashMap<>();

    private HoverButton saveAsNewButton;
    private HoverButton updateButton;
    private HoverButton revertButton;
    private HoverButton deleteButton;
    private HoverButton setDefaultButton;

    public PresetSelector(String title, ViewPresetService service,
                          CheckBoxSelector visibilitySelector, List<String> headers) {
        super(title);
        this.service = service;
        this.visibilitySelector = visibilitySelector;
        this.headers = headers;
        setComponentHeight(heightForCount(service.listPresets().size()));
    }

    @Override public String getValue() { return selectedName; }

    /** Select a preset: load its columns into the working set (select == load). */
    @Override public void setValue(String name) {
        if (name == null || !service.listPresets().contains(name)) return;
        select(name);
    }

    @Override public boolean hasSelection() { return true; } // never blocks the dialog's Apply

    public boolean isDirty() {
        return selectedName != null && !workingMatches(selectedName);
    }

    public void onWorkingStateChanged() { refresh(); }

    @Override
    protected void buildSelectorPanel(JPanel panel) {
        // panel is the title-bearing content panel (BoxLayout Y, title already added). Do NOT wipe it.
        listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(listPanel);

        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel toolbar = buildToolbar();
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(toolbar);

        // Open already pointing at the preset whose columns match the current view (prefer the default).
        selectedName = detectCurrentSelection();
        rebuildList();
    }

    private JPanel buildToolbar() {
        saveAsNewButton  = new HoverButton("+ Save as new…");
        updateButton     = new HoverButton("Update");
        revertButton     = new HoverButton("Revert");
        deleteButton     = new HoverButton("Delete");
        setDefaultButton = new HoverButton("Set as default");

        saveAsNewButton.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(listPanel, "Name this preset:");
            if (name != null && !name.isBlank()) {
                service.savePreset(name.trim(),
                    VisibilityResolver.computeOverrides(headers, visibilitySelector.getValue()));
                selectedName = name.trim();
                rebuildList();
            }
        });
        updateButton.addActionListener(e -> {
            if (selectedName != null && isDirty()) {
                service.savePreset(selectedName,
                    VisibilityResolver.computeOverrides(headers, visibilitySelector.getValue()));
                refresh();
            }
        });
        revertButton.addActionListener(e -> {
            if (selectedName != null && isDirty()) {
                visibilitySelector.setValue(service.resolveFor(headers, selectedName));
                refresh();
            }
        });
        deleteButton.addActionListener(e -> {
            if (selectedName != null) {
                service.deletePreset(selectedName);
                selectedName = null;
                rebuildList();
            }
        });
        setDefaultButton.addActionListener(e -> {
            if (selectedName != null) { service.setDefault(selectedName); refresh(); }
        });

        JPanel bar = new JPanel();
        bar.setOpaque(false);
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.add(row(saveAsNewButton));                       // creation primitive, full width
        bar.add(Box.createRigidArea(new Dimension(0, 4)));
        bar.add(row(updateButton, revertButton));            // the modified-state pair
        bar.add(Box.createRigidArea(new Dimension(0, 4)));
        bar.add(row(deleteButton, setDefaultButton));        // manage
        return bar;
    }

    /** A fixed-height, full-width row of equal buttons (GridLayout avoids FlowLayout clipping). */
    private static JPanel row(HoverButton... buttons) {
        JPanel p = new JPanel(new GridLayout(1, buttons.length, 6, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (HoverButton b : buttons) p.add(b);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return p;
    }

    private void select(String name) {
        visibilitySelector.setValue(service.resolveFor(headers, name)); // load into working draft
        selectedName = name;
        refresh();
        notifyUpdateCallback();
    }

    /** Structural rebuild — only when the set of presets changes (save / delete / init). */
    private void rebuildList() {
        listPanel.removeAll();
        rowLabels.clear();
        rowRadios.clear();
        group = new ButtonGroup();

        List<String> names = service.listPresets();
        if (names.isEmpty()) {
            JLabel empty = new JLabel("No presets yet — use “+ Save as new…”");
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(empty);
        }
        for (String name : names) {
            JPanel rowPanel = new JPanel();
            rowPanel.setOpaque(false);
            rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
            rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));

            HoverRadioButton radio = new HoverRadioButton("");
            radio.setOpaque(false);
            radio.setSelected(name.equals(selectedName));
            radio.addActionListener(e -> select(name));
            group.add(radio);

            JLabel label = new JLabel(composeLabel(name));

            rowPanel.add(radio);
            rowPanel.add(Box.createRigidArea(new Dimension(6, 0)));
            rowPanel.add(label);
            rowPanel.add(Box.createHorizontalGlue());

            rowRadios.put(name, radio);
            rowLabels.put(name, label);
            listPanel.add(rowPanel);
        }
        listPanel.revalidate();
        listPanel.repaint();
        refresh();
        resizeForCount();
    }

    /** Lightweight refresh — labels + button enablement, no structural change (radios persist). */
    private void refresh() {
        boolean hasSel = selectedName != null;
        boolean dirty = isDirty();
        for (Map.Entry<String, JLabel> e : rowLabels.entrySet()) {
            e.getValue().setText(composeLabel(e.getKey()));
            HoverRadioButton r = rowRadios.get(e.getKey());
            if (r != null) r.setSelected(e.getKey().equals(selectedName));
        }
        if (updateButton != null) {
            updateButton.setEnabled(hasSel && dirty);
            revertButton.setEnabled(hasSel && dirty);
            deleteButton.setEnabled(hasSel);
            setDefaultButton.setEnabled(hasSel);
        }
    }

    /** Grow/shrink the fixed-size component to fit the current preset count, then reflow the dialog. */
    /** Grow/shrink to fit the preset count, preserving the dialog-assigned width, then reflow the scroll view. */
    private void resizeForCount() {
        int newHeight = heightForCount(service.listPresets().size());
        int delta = newHeight - componentHeight;
        setComponentHeight(newHeight);
        if (cachedPanel == null || delta == 0) return;

        // Preserve the width the parent dialog assigned (NOT STANDARD_COMPONENT_WIDTH, which is wider
        // than this dialog's content area and caused the box to jump wider on the first resize).
        int width = cachedPanel.getWidth() > 0
            ? cachedPanel.getWidth()
            : (int) cachedPanel.getPreferredSize().getWidth();
        DialogUtils.fixSize(cachedPanel, new Dimension(width, newHeight));

        // InputsDialog fixes the scroll view's height at construction; grow it by the delta so the
        // taller panel gets room and the scrollbar updates, without re-packing the window (which widened it).
        JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, cachedPanel);
        if (scroll != null) {
            Component view = scroll.getViewport().getView();
            if (view != null) {
                Dimension vp = view.getPreferredSize();
                view.setPreferredSize(new Dimension(vp.width, vp.height + delta));
                view.revalidate();
            }
            scroll.revalidate();
            scroll.repaint();
        } else {
            cachedPanel.revalidate();
            cachedPanel.repaint();
        }
    }

    private static int heightForCount(int n) {
        return CHROME_HEIGHT + ROW_HEIGHT * Math.max(1, n);
    }

    /** The preset whose resolved columns match the current working set, preferring the default. */
    private String detectCurrentSelection() {
        List<String> names = service.listPresets();
        String def = service.getDefaultName();
        if (def != null && names.contains(def) && workingMatches(def)) return def;
        for (String name : names) if (workingMatches(name)) return name;
        return null;
    }

    /** True if every currently-shown column matches what this preset resolves it to. */
    private boolean workingMatches(String name) {
        Map<String, Boolean> resolved = service.resolveFor(headers, name);
        for (Map.Entry<String, Boolean> e : visibilitySelector.getValue().entrySet()) {
            Boolean r = resolved.get(e.getKey());
            if (r == null || r.booleanValue() != e.getValue().booleanValue()) return false;
        }
        return true;
    }

    private String composeLabel(String name) {
        StringBuilder status = new StringBuilder();
        if (name.equals(selectedName) && isDirty()) status.append("modified");
        if (name.equals(service.getDefaultName())) {
            if (status.length() > 0) status.append("  ·  ");
            status.append("default");
        }
        return status.length() == 0 ? name : name + "      ·  " + status;
    }
}
