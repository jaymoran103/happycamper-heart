package com.echo.service.config;

import java.util.LinkedHashMap;

/** In-memory model of the persisted view-presets file. */
public final class ViewPresetStore {

    public static final int CURRENT_VERSION = 1;

    private int version = CURRENT_VERSION;
    private String defaultName; // nullable
    private final LinkedHashMap<String, LinkedHashMap<String, Boolean>> presets = new LinkedHashMap<>();

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getDefaultName() { return defaultName; }
    public void setDefaultName(String defaultName) { this.defaultName = defaultName; }

    public LinkedHashMap<String, LinkedHashMap<String, Boolean>> getPresets() { return presets; }

    /** Store a preset under name as a defensive copy of the provided overrides. */
    public void putPreset(String name, LinkedHashMap<String, Boolean> overrides) {
        presets.put(name, new LinkedHashMap<>(overrides));
    }

    public void removePreset(String name) { presets.remove(name); }

    public boolean hasPreset(String name) { return presets.containsKey(name); }

    /** Return the overrides for the named preset, or an empty map if absent (never null). */
    public LinkedHashMap<String, Boolean> getOverrides(String name) {
        LinkedHashMap<String, Boolean> ov = presets.get(name);
        return ov == null ? new LinkedHashMap<>() : ov;
    }
}
