package com.echo.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class ViewPresetJsonTest {

    private static LinkedHashMap<String,Boolean> ov(String k, boolean v) {
        LinkedHashMap<String,Boolean> m = new LinkedHashMap<>(); m.put(k, v); return m;
    }

    @Test void roundTripPreservesData() {
        ViewPresetStore s = new ViewPresetStore();
        s.setDefaultName("Swim view");
        s.putPreset("Swim view", ov("Cabin", false));
        s.putPreset("Medical view", ov("Activity 1", false));

        ViewPresetStore back = ViewPresetJson.parse(ViewPresetJson.toJson(s));
        assertEquals(1, back.getVersion());
        assertEquals("Swim view", back.getDefaultName());
        assertEquals(Boolean.FALSE, back.getOverrides("Swim view").get("Cabin"));
        assertTrue(back.hasPreset("Medical view"));
    }

    @Test void nullDefaultSerializesAndParses() {
        ViewPresetStore s = new ViewPresetStore();
        s.setDefaultName(null);
        s.putPreset("X", ov("A", true));
        ViewPresetStore back = ViewPresetJson.parse(ViewPresetJson.toJson(s));
        assertNull(back.getDefaultName());
        assertEquals(Boolean.TRUE, back.getOverrides("X").get("A"));
    }

    @Test void escapesSpecialCharactersInNames() {
        ViewPresetStore s = new ViewPresetStore();
        s.putPreset("Quote \" and \\ slash", ov("Col\tTab", false));
        ViewPresetStore back = ViewPresetJson.parse(ViewPresetJson.toJson(s));
        assertTrue(back.hasPreset("Quote \" and \\ slash"));
        assertEquals(Boolean.FALSE, back.getOverrides("Quote \" and \\ slash").get("Col\tTab"));
    }

    @Test void emptyStoreRoundTrips() {
        ViewPresetStore back = ViewPresetJson.parse(ViewPresetJson.toJson(new ViewPresetStore()));
        assertEquals(1, back.getVersion());
        assertNull(back.getDefaultName());
        assertTrue(back.getPresets().isEmpty());
    }

    @Test void malformedThrows() {
        assertThrows(IllegalArgumentException.class, () -> ViewPresetJson.parse("{ not json"));
        assertThrows(IllegalArgumentException.class, () -> ViewPresetJson.parse("[]"));
        assertThrows(IllegalArgumentException.class, () -> ViewPresetJson.parse(""));
    }

    @Test void nonBooleanOverrideValuesAreIgnored() {
        String json = "{ \"version\": 1, \"default\": null, "
            + "\"presets\": { \"P\": { \"overrides\": { \"A\": \"nope\", \"B\": true } } } }";
        ViewPresetStore back = ViewPresetJson.parse(json);
        assertNull(back.getOverrides("P").get("A"));
        assertEquals(Boolean.TRUE, back.getOverrides("P").get("B"));
    }
}
