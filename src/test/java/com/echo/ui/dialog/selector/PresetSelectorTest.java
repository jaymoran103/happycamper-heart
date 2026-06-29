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

    private static final List<String> HEADERS = List.of("First Name", "Last Name");

    private static LinkedHashMap<String,Boolean> visibility(boolean firstNameShown) {
        LinkedHashMap<String,Boolean> m = new LinkedHashMap<>();
        m.put("First Name", firstNameShown); // factory default = false (hidden)
        m.put("Last Name", true);            // factory default = true
        return m;
    }
    private static LinkedHashMap<String,Boolean> ov(String k, boolean v) {
        LinkedHashMap<String,Boolean> m = new LinkedHashMap<>(); m.put(k, v); return m;
    }
    private static List<AbstractButton> buttons(Container c) {
        List<AbstractButton> r = new ArrayList<>();
        for (Component x : c.getComponents()) {
            if (x instanceof AbstractButton b) r.add(b);
            if (x instanceof Container k) r.addAll(buttons(k));
        }
        return r;
    }
    /** A labeled toolbar button. */
    private static AbstractButton button(Container c, String text) {
        return buttons(c).stream().filter(b -> text.equals(b.getText())).findFirst().orElse(null);
    }
    /** A per-row selection radio (empty text), in list order. */
    private static AbstractButton firstRowRadio(Container c) {
        return buttons(c).stream().filter(b -> "".equals(b.getText())).findFirst().orElse(null);
    }
    private static PresetSelector build(ViewPresetService svc, CheckBoxSelector cb) {
        return new PresetSelector("Presets", svc, cb, HEADERS);
    }

    @Test void applyingSavedPresetUpdatesVisibility(@TempDir Path dir) {
        ViewPresetService svc = new ViewPresetService(dir.resolve("v.json"));
        svc.savePreset("Reveal", ov("First Name", true));
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = build(svc, cb);
        ps.createPanel();
        ps.setValue("Reveal");
        assertTrue(cb.getValue().get("First Name"));
        assertFalse(ps.isDirty());
    }

    @Test void togglingVisibilityAfterApplyMarksDirty(@TempDir Path dir) {
        ViewPresetService svc = new ViewPresetService(dir.resolve("v.json"));
        svc.savePreset("Clean", new LinkedHashMap<>());
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = build(svc, cb);
        ps.createPanel();
        ps.setValue("Clean");
        assertFalse(ps.isDirty());
        cb.setValue(visibility(true));
        ps.onWorkingStateChanged();
        assertTrue(ps.isDirty());
    }

    @Test void getValueReturnsActivePreset(@TempDir Path dir) {
        ViewPresetService svc = new ViewPresetService(dir.resolve("v.json"));
        svc.savePreset("A", new LinkedHashMap<>());
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = build(svc, cb);
        ps.createPanel();
        ps.setValue("A");
        assertEquals("A", ps.getValue());
    }

    @Test void clickingRowRadioLoadsPresetColumns(@TempDir Path dir) {
        ViewPresetService svc = new ViewPresetService(dir.resolve("v.json"));
        svc.savePreset("Reveal", ov("First Name", true)); // resolves First Name -> true
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = build(svc, cb);
        JPanel panel = ps.createPanel();
        assertFalse(cb.getValue().get("First Name"));   // hidden before any click
        AbstractButton radio = firstRowRadio(panel);
        assertNotNull(radio, "a row selection radio should exist");
        radio.doClick();                                // click the radio
        assertTrue(cb.getValue().get("First Name"));    // preset loaded into the checkboxes
        assertEquals("Reveal", ps.getValue());
    }

    @Test void preSelectsPresetMatchingCurrentColumnsOnOpen(@TempDir Path dir) {
        ViewPresetService svc = new ViewPresetService(dir.resolve("v.json"));
        svc.savePreset("Factory", new LinkedHashMap<>()); // resolves to factory defaults
        // working set already equals factory defaults
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = build(svc, cb);
        ps.createPanel();                       // no click
        assertEquals("Factory", ps.getValue()); // pre-selected because columns match
        assertFalse(ps.isDirty());
    }

    @Test void updateSavesCurrentColumnsToActivePreset(@TempDir Path dir) {
        ViewPresetService svc = new ViewPresetService(dir.resolve("v.json"));
        svc.savePreset("Clean", new LinkedHashMap<>());
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = build(svc, cb);
        JPanel panel = ps.createPanel();
        ps.setValue("Clean");
        cb.setValue(visibility(true));          // reveal First Name -> diverge
        ps.onWorkingStateChanged();
        assertTrue(ps.isDirty());
        AbstractButton update = button(panel, "Update");
        assertNotNull(update);
        assertTrue(update.isEnabled());
        update.doClick();
        assertEquals(Boolean.TRUE, svc.getOverrides("Clean").get("First Name"));
        assertFalse(ps.isDirty());
    }

    @Test void revertRestoresActivePresetAndClearsDirty(@TempDir Path dir) {
        ViewPresetService svc = new ViewPresetService(dir.resolve("v.json"));
        svc.savePreset("Clean", new LinkedHashMap<>());
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = build(svc, cb);
        JPanel panel = ps.createPanel();
        ps.setValue("Clean");
        cb.setValue(visibility(true));
        ps.onWorkingStateChanged();
        assertTrue(ps.isDirty());
        AbstractButton revert = button(panel, "Revert");
        assertNotNull(revert);
        revert.doClick();
        assertFalse(cb.getValue().get("First Name")); // restored to preset's resolved value
        assertFalse(ps.isDirty());
    }

    @Test void deleteToolbarButtonRemovesSelectedAndClearsActive(@TempDir Path dir) {
        ViewPresetService svc = new ViewPresetService(dir.resolve("v.json"));
        svc.savePreset("A", ov("First Name", true));
        svc.savePreset("B", new LinkedHashMap<>());
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = build(svc, cb);
        JPanel panel = ps.createPanel();
        ps.setValue("A");
        assertEquals("A", ps.getValue());
        AbstractButton delete = button(panel, "Delete");
        assertNotNull(delete);
        delete.doClick();
        assertEquals(List.of("B"), svc.listPresets());
        assertNull(ps.getValue());
    }

    @Test void setAsDefaultButtonUpdatesService(@TempDir Path dir) {
        ViewPresetService svc = new ViewPresetService(dir.resolve("v.json"));
        svc.savePreset("A", new LinkedHashMap<>());
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        PresetSelector ps = build(svc, cb);
        JPanel panel = ps.createPanel();
        ps.setValue("A");
        AbstractButton setDefault = button(panel, "Set as default");
        assertNotNull(setDefault);
        setDefault.doClick();
        assertEquals("A", svc.getDefaultName());
    }
}
