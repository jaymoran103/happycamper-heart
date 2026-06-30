package com.echo.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ViewPresetServiceTest {

    private static LinkedHashMap<String,Boolean> ov(String k, boolean v) {
        LinkedHashMap<String,Boolean> m = new LinkedHashMap<>(); m.put(k, v); return m;
    }

    @Test void savePersistsAndReloads(@TempDir Path dir) {
        Path file = dir.resolve("view-presets.json");
        ViewPresetService s = new ViewPresetService(file);
        s.savePreset("Swim view", ov("Cabin", false));
        s.setDefault("Swim view");

        ViewPresetService reloaded = new ViewPresetService(file);
        assertEquals(List.of("Swim view"), reloaded.listPresets());
        assertEquals("Swim view", reloaded.getDefaultName());
        assertEquals(Boolean.FALSE, reloaded.getOverrides("Swim view").get("Cabin"));
    }

    @Test void deleteClearsDefaultWhenItPointedThere(@TempDir Path dir) {
        ViewPresetService s = new ViewPresetService(dir.resolve("view-presets.json"));
        s.savePreset("X", ov("A", false));
        s.setDefault("X");
        s.deletePreset("X");
        assertNull(s.getDefaultName());
        assertFalse(s.listPresets().contains("X"));
    }

    @Test void resolveForInheritsDefaultsAndDropsAbsent(@TempDir Path dir) {
        ViewPresetService s = new ViewPresetService(dir.resolve("view-presets.json"));
        s.savePreset("P", ov("First Name", true)); // reveal a normally-hidden column
        var resolved = s.resolveFor(List.of("First Name", "Last Name"), "P");
        assertTrue(resolved.get("First Name"));  // override
        assertTrue(resolved.get("Last Name"));   // factory default
        assertFalse(resolved.containsKey("Removed Column"));
    }

    @Test void corruptFileIsQuarantinedAndServiceStillWorks(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("view-presets.json");
        Files.writeString(file, "{ this is not json");
        ViewPresetService s = new ViewPresetService(file); // must not throw
        assertTrue(s.listPresets().isEmpty());
        assertTrue(Files.exists(dir.resolve("view-presets.bak")));
        s.savePreset("Fresh", ov("A", false)); // still usable
        assertEquals(List.of("Fresh"), s.listPresets());
    }

    @Test void missingFileStartsEmpty(@TempDir Path dir) {
        ViewPresetService s = new ViewPresetService(dir.resolve("nope.json"));
        assertTrue(s.listPresets().isEmpty());
        assertNull(s.getDefaultName());
    }

    @Test void newerVersionFileIsQuarantinedAndStartsEmpty(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("view-presets.json");
        // A structurally VALID file, but a schema version newer than this build supports.
        Files.writeString(file,
            "{ \"version\": 2, \"default\": null, \"presets\": { \"P\": { \"overrides\": { \"A\": false } } } }");
        ViewPresetService s = new ViewPresetService(file); // must not throw
        assertTrue(s.listPresets().isEmpty());                       // unsupported version => treated as empty
        assertTrue(Files.exists(dir.resolve("view-presets.bak")));   // original quarantined, not lost
        s.savePreset("Fresh", ov("A", false));                       // still usable afterward
        assertEquals(List.of("Fresh"), s.listPresets());
    }
}
