package com.echo.ui.selector;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.echo.service.config.ViewPresetService;
import com.echo.service.config.VisibilityResolver;
import com.echo.ui.elements.HoverButton;
import com.echo.ui.elements.HoverRadioButton;

/**
 * Manages named column-visibility presets inside ColumnVisibilityDialog. Value = the active preset name.
 * Applies presets by writing into the supplied CheckBoxSelector; mutations persist immediately via the service.
 */
public class PresetSelector extends InputSelector<String> {

    private final ViewPresetService service;
    private final CheckBoxSelector visibilitySelector;
    private final List<String> headers;

    private String activeName;       // currently applied preset, or null
    private JPanel content;          // the panel we (re)build rows into
    private HoverButton updateButton;
    private HoverButton revertButton;

    public PresetSelector(String title, ViewPresetService service,
                          CheckBoxSelector visibilitySelector, List<String> headers) {
        super(title);
        this.service = service;
        this.visibilitySelector = visibilitySelector;
        this.headers = headers;
        setComponentHeight(220);
    }

    @Override public String getValue() { return activeName; }

    /** Apply the named preset to the visibility selector, update activeName, and fire the update callback. */
    @Override public void setValue(String name) {
        if (name == null || !service.listPresets().contains(name)) return;
        visibilitySelector.setValue(service.resolveFor(headers, name));
        activeName = name;
        onWorkingStateChanged();
        notifyUpdateCallback();
    }

    @Override public boolean hasSelection() { return true; } // never blocks the dialog's Apply

    /** Return true if the current visibility state has diverged from what was last applied by activeName. */
    public boolean isDirty() {
        if (activeName == null) return false;
        Map<String, Boolean> applied = service.resolveFor(headers, activeName);
        Map<String, Boolean> working = visibilitySelector.getValue();
        return !applied.equals(working);
    }

    /** Recompute dirty state and refresh the Update/Revert enabled state + active marker. */
    public void onWorkingStateChanged() {
        if (content != null) rebuild();
    }

    /** Wire the content panel to a BoxLayout and perform the initial rebuild. */
    @Override
    protected void buildSelectorPanel(JPanel panel) {
        this.content = panel;
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        rebuild();
    }

    /** Rebuild the preset list: "none" default radio, one row per preset, Update/Revert actions, and Save button. */
    private void rebuild() {
        content.removeAll();
        ButtonGroup defaultGroup = new ButtonGroup();

        // "none" default radio — clears the auto-apply default
        HoverRadioButton none = new HoverRadioButton("Default: none");
        none.setSelected(service.getDefaultName() == null);
        none.addActionListener(e -> service.setDefault(null));
        defaultGroup.add(none);
        content.add(none);

        // per-preset row: default-radio + apply button (with dirty marker) + delete button
        for (String name : service.listPresets()) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

            HoverRadioButton def = new HoverRadioButton("");
            def.setSelected(name.equals(service.getDefaultName()));
            def.addActionListener(e -> service.setDefault(name));
            defaultGroup.add(def);
            row.add(def);

            boolean active = name.equals(activeName);
            String label = name + (active && isDirty() ? "  • (modified)" : active ? "  •" : "");
            HoverButton applyBtn = new HoverButton(label);
            applyBtn.addActionListener(e -> setValue(name));
            row.add(applyBtn);

            HoverButton del = new HoverButton("×");
            del.addActionListener(e -> {
                service.deletePreset(name);
                if (name.equals(activeName)) activeName = null;
                rebuild();
            });
            row.add(del);

            content.add(row);
        }

        // Update / Revert buttons, enabled only when the active preset is dirty
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        updateButton = new HoverButton("Update");
        updateButton.setEnabled(isDirty());
        updateButton.addActionListener(e -> {
            if (activeName != null) {
                service.savePreset(activeName, VisibilityResolver.computeOverrides(headers, visibilitySelector.getValue()));
                rebuild();
            }
        });
        revertButton = new HoverButton("Revert");
        revertButton.setEnabled(isDirty());
        revertButton.addActionListener(e -> {
            if (activeName != null) {
                visibilitySelector.setValue(service.resolveFor(headers, activeName));
                rebuild();
            }
        });
        actions.add(updateButton);
        actions.add(revertButton);
        content.add(actions);

        HoverButton save = new HoverButton("+ Save current columns as preset…");
        save.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(content, "Preset name:");
            if (name != null && !name.isBlank()) {
                service.savePreset(name.trim(),
                    VisibilityResolver.computeOverrides(headers, visibilitySelector.getValue()));
                activeName = name.trim();
                rebuild();
            }
        });
        content.add(save);

        for (Component c : content.getComponents()) {
            if (c instanceof JPanel jp) jp.setOpaque(false);
        }
        content.revalidate();
        content.repaint();
    }
}
