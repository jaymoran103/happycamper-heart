package com.echo.ui.dialog.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
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
}
