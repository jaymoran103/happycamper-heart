package com.echo.ui.dialog.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.echo.service.config.ViewPresetService;
import com.echo.ui.selector.CheckBoxSelector;
import com.echo.ui.selector.PresetSelector;

class PresetSelectorTest {

    private static LinkedHashMap<String,Boolean> visibility(boolean firstNameShown) {
        LinkedHashMap<String,Boolean> m = new LinkedHashMap<>();
        m.put("First Name", firstNameShown); // factory default = false (hidden)
        m.put("Last Name", true);            // factory default = true
        return m;
    }

    private PresetSelector build(Path file, CheckBoxSelector cb) {
        ViewPresetService svc = new ViewPresetService(file);
        return new PresetSelector("Presets", svc, cb, List.of("First Name", "Last Name"));
    }

    @Test void applyingSavedPresetUpdatesVisibility(@TempDir Path dir) {
        Path file = dir.resolve("view-presets.json");
        ViewPresetService svc = new ViewPresetService(file);
        LinkedHashMap<String,Boolean> ov = new LinkedHashMap<>(); ov.put("First Name", true);
        svc.savePreset("Reveal", ov);

        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = new PresetSelector("Presets", svc, cb, List.of("First Name", "Last Name"));
        ps.createPanel(); // builds rows
        ps.setValue("Reveal"); // apply

        assertTrue(cb.getValue().get("First Name")); // revealed by preset
        assertFalse(ps.isDirty());                   // freshly applied → clean
    }

    @Test void togglingVisibilityAfterApplyMarksDirty(@TempDir Path dir) {
        Path file = dir.resolve("view-presets.json");
        ViewPresetService svc = new ViewPresetService(file);
        svc.savePreset("Clean", new LinkedHashMap<>()); // no overrides → equals factory defaults

        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = new PresetSelector("Presets", svc, cb, List.of("First Name", "Last Name"));
        ps.createPanel();
        ps.setValue("Clean");
        assertFalse(ps.isDirty());

        cb.setValue(visibility(true)); // user reveals First Name
        ps.onWorkingStateChanged();
        assertTrue(ps.isDirty());
    }

    @Test void getValueReturnsActivePreset(@TempDir Path dir) {
        ViewPresetService svc = new ViewPresetService(dir.resolve("view-presets.json"));
        svc.savePreset("A", new LinkedHashMap<>());
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = new PresetSelector("Presets", svc, cb, List.of("First Name", "Last Name"));
        ps.createPanel();
        ps.setValue("A");
        assertEquals("A", ps.getValue());
    }

    // -----------------------------------------------------------------------
    // Group 2 — mutation-path tests
    // -----------------------------------------------------------------------

    /** Recursively collects all AbstractButton descendants of a Container. */
    private static List<AbstractButton> collectButtons(Container c) {
        List<AbstractButton> result = new ArrayList<>();
        for (Component comp : c.getComponents()) {
            if (comp instanceof AbstractButton ab) result.add(ab);
            if (comp instanceof Container sub) result.addAll(collectButtons(sub));
        }
        return result;
    }

    /**
     * Finds the preset row JPanel inside a container whose direct children include a button
     * whose text starts with presetName (and is non-empty) AND a "×" delete button.
     */
    private static JPanel findPresetRow(Container panel, String presetName) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JPanel row) {
                boolean hasName = false, hasDelete = false;
                for (Component c : row.getComponents()) {
                    if (c instanceof AbstractButton ab) {
                        String t = ab.getText();
                        if (t != null && !t.isEmpty() && t.startsWith(presetName)) hasName = true;
                        if ("×".equals(t)) hasDelete = true;
                    }
                }
                if (hasName && hasDelete) return row;
            }
        }
        return null;
    }

    @Test void deletingPresetRemovesItAndClearsActive(@TempDir Path dir) {
        Path file = dir.resolve("view-presets.json");
        ViewPresetService svc = new ViewPresetService(file);
        svc.savePreset("A", new LinkedHashMap<>());
        svc.savePreset("B", new LinkedHashMap<>());
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = new PresetSelector("Presets", svc, cb, List.of("First Name", "Last Name"));
        JPanel outerPanel = ps.createPanel();
        ps.setValue("A");
        assertEquals("A", ps.getValue());

        Container content = (Container) outerPanel.getComponent(0);
        JPanel rowA = findPresetRow(content, "A");
        assertNotNull(rowA, "row for preset A should exist in the panel");
        AbstractButton del = collectButtons(rowA).stream()
            .filter(b -> "×".equals(b.getText())).findFirst().orElseThrow();
        del.doClick();

        assertEquals(List.of("B"), svc.listPresets());
        assertNull(ps.getValue());
    }

    @Test void updateSavesCurrentColumnsToActivePreset(@TempDir Path dir) {
        Path file = dir.resolve("view-presets.json");
        ViewPresetService svc = new ViewPresetService(file);
        svc.savePreset("Clean", new LinkedHashMap<>()); // no overrides = factory defaults

        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = new PresetSelector("Presets", svc, cb, List.of("First Name", "Last Name"));
        JPanel outerPanel = ps.createPanel();
        ps.setValue("Clean");
        assertFalse(ps.isDirty());

        cb.setValue(visibility(true));  // First Name = true, deviates from factory default false
        ps.onWorkingStateChanged();     // marks dirty, triggers rebuild
        assertTrue(ps.isDirty());

        Container content = (Container) outerPanel.getComponent(0);
        AbstractButton updateBtn = collectButtons(content).stream()
            .filter(b -> "Update".equals(b.getText())).findFirst().orElseThrow();
        assertTrue(updateBtn.isEnabled());
        updateBtn.doClick();

        // Update persists the deviation: First Name = true (deviates from false factory default)
        assertTrue(svc.getOverrides("Clean").containsKey("First Name"));
        assertEquals(Boolean.TRUE, svc.getOverrides("Clean").get("First Name"));
    }

    @Test void revertRestoresActivePresetAndClearsDirty(@TempDir Path dir) {
        Path file = dir.resolve("view-presets.json");
        ViewPresetService svc = new ViewPresetService(file);
        svc.savePreset("Clean", new LinkedHashMap<>()); // no overrides = factory defaults

        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = new PresetSelector("Presets", svc, cb, List.of("First Name", "Last Name"));
        JPanel outerPanel = ps.createPanel();
        ps.setValue("Clean");
        assertFalse(ps.isDirty());

        cb.setValue(visibility(true));  // dirty: First Name revealed
        ps.onWorkingStateChanged();
        assertTrue(ps.isDirty());

        Container content = (Container) outerPanel.getComponent(0);
        AbstractButton revertBtn = collectButtons(content).stream()
            .filter(b -> "Revert".equals(b.getText())).findFirst().orElseThrow();
        revertBtn.doClick();

        // Revert restores cb to the preset's resolved value: First Name = false (factory)
        assertFalse(cb.getValue().get("First Name"));
        assertFalse(ps.isDirty());
    }

    @Test void defaultRadiosUpdateService(@TempDir Path dir) {
        Path file = dir.resolve("view-presets.json");
        ViewPresetService svc = new ViewPresetService(file);
        svc.savePreset("A", new LinkedHashMap<>());
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = new PresetSelector("Presets", svc, cb, List.of("First Name", "Last Name"));
        JPanel outerPanel = ps.createPanel();

        Container content = (Container) outerPanel.getComponent(0);
        JPanel rowA = findPresetRow(content, "A");
        assertNotNull(rowA, "row for preset A should exist in the panel");

        // The empty-text radio in A's row sets A as default
        AbstractButton defRadio = collectButtons(rowA).stream()
            .filter(b -> "".equals(b.getText())).findFirst().orElseThrow();
        defRadio.doClick();
        assertEquals("A", svc.getDefaultName());

        // The "Default: none" radio clears the default
        AbstractButton noneRadio = collectButtons(content).stream()
            .filter(b -> "Default: none".equals(b.getText())).findFirst().orElseThrow();
        noneRadio.doClick();
        assertNull(svc.getDefaultName());
    }
}
