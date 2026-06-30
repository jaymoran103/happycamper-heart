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

    /** Save button has an ellipsis in its label; match by prefix. */
    private static AbstractButton saveButton(Container c) {
        return buttons(c).stream()
            .filter(b -> b.getText() != null && b.getText().startsWith("+ Save"))
            .findFirst().orElse(null);
    }

    /** PresetSelector with prompt seams stubbed so headless tests never open JOptionPane. */
    private static class StubSelector extends PresetSelector {
        String nameToReturn;
        boolean confirmResult = true;
        String lastWarn;
        StubSelector(ViewPresetService svc, CheckBoxSelector cb) { super("Presets", svc, cb, HEADERS); }
        @Override protected String promptForName() { return nameToReturn; }
        @Override protected boolean confirm(String title, String message) { return confirmResult; }
        @Override protected void warn(String title, String message) { lastWarn = message; }
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
        StubSelector ps = new StubSelector(svc, cb);
        ps.confirmResult = true;                       // user confirms the delete
        JPanel panel = ps.createPanel();
        ps.setValue("A");
        assertEquals("A", ps.getValue());
        button(panel, "Delete").doClick();
        assertEquals(List.of("B"), svc.listPresets());
        assertNull(ps.getValue());
    }

    @Test void deleteConfirmDeclinedKeepsPreset(@TempDir Path dir) {
        ViewPresetService svc = new ViewPresetService(dir.resolve("v.json"));
        svc.savePreset("A", new LinkedHashMap<>());
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        StubSelector ps = new StubSelector(svc, cb);
        ps.confirmResult = false;                      // user cancels the delete
        JPanel panel = ps.createPanel();
        ps.setValue("A");
        button(panel, "Delete").doClick();
        assertEquals(List.of("A"), svc.listPresets()); // still there
        assertEquals("A", ps.getValue());
    }

    @Test void saveRejectsDuplicateNameWithoutOverwriting(@TempDir Path dir) {
        ViewPresetService svc = new ViewPresetService(dir.resolve("v.json"));
        svc.savePreset("A", ov("First Name", true));   // existing A reveals First Name
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false);
        StubSelector ps = new StubSelector(svc, cb);
        ps.nameToReturn = "A";                         // try to save-as-new under the existing name
        JPanel panel = ps.createPanel();
        saveButton(panel).doClick();
        assertEquals(List.of("A"), svc.listPresets());                       // no new entry
        assertEquals(Boolean.TRUE, svc.getOverrides("A").get("First Name")); // A NOT overwritten
        assertNotNull(ps.lastWarn);                                          // user was warned
    }

    @Test void saveIdenticalColumnsRespectsDeclineThenAccepts(@TempDir Path dir) {
        ViewPresetService svc = new ViewPresetService(dir.resolve("v.json"));
        svc.savePreset("A", new LinkedHashMap<>());    // A == factory defaults
        CheckBoxSelector cb = new CheckBoxSelector("Column Visibility", visibility(false), false); // == factory
        StubSelector ps = new StubSelector(svc, cb);
        JPanel panel = ps.createPanel();
        ps.nameToReturn = "B";
        ps.confirmResult = false;                      // decline the identical-columns warning
        saveButton(panel).doClick();
        assertEquals(List.of("A"), svc.listPresets()); // B not saved
        ps.confirmResult = true;                       // accept the duplicate
        saveButton(panel).doClick();
        assertTrue(svc.listPresets().contains("B"));   // now saved despite identical columns
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
