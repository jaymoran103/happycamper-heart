package com.echo.service.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Loads, mutates, and persists the view-presets file. Constructor-injected path keeps it unit-testable;
 * {@link #getInstance()} provides the production singleton. Never throws into callers.
 */
public final class ViewPresetService {

    private static ViewPresetService instance;

    private final Path file;
    private ViewPresetStore store = new ViewPresetStore();

    /**
     * Open the service against a specific presets file and load it immediately. A missing file yields an
     * empty store; an unreadable one is quarantined (never thrown). This is the DI-friendly entry point
     * used by tests.
     *
     * @param presetsFile the JSON file to read/write (its parent dir is created lazily on first save)
     */
    public ViewPresetService(Path presetsFile) {
        this.file = presetsFile;
        load();
    }

    /**
     * Lazily build and return the process-wide singleton over the OS-native presets path. 
     * Logs the resolved path once so QA/users can find the file and any quarantined backups
     *
     * FUTURE - this is interim production wiring, not the target design. noted in patches log.
     *
     * @return the shared instance, created on first call
     */
    public static synchronized ViewPresetService getInstance() {
        if (instance == null) {
            Path f = ConfigLocation.presetsFilePath();
            System.out.println("[HappyCamper] view presets file: " + f);
            instance = new ViewPresetService(f);
        }
        return instance;
    }

    /** @return the preset names in stable insertion order (a fresh list copy; safe to iterate/mutate). */
    public List<String> listPresets() { return new ArrayList<>(store.getPresets().keySet()); }

    /** @return the name of the auto-apply default preset, or {@code null} if none is set. */
    public String getDefaultName() { return store.getDefaultName(); }

    /**
     * Set (or clear) the auto-apply default preset and persist immediately.
     *
     * @param nameOrNull the preset name to auto-apply on roster load, or null to clear the default.
     */
    public void setDefault(String nameOrNull) { store.setDefaultName(nameOrNull); persist(); }

    /**
     * Create or overwrite a preset from its per-column overrides and persist immediately. 
     * 
     * The store keeps a defensive copy, so the caller's map may be mutated afterward without affecting the saved preset.
     *
     * @param name the preset name (also the UI label and JSON key)
     * @param overrides per-column desired visibility, already reduced to deviations-from-factory
     */
    public void savePreset(String name, LinkedHashMap<String, Boolean> overrides) {
        store.putPreset(name, overrides);
        persist();
    }

    /**
     * Remove a preset and persist immediately. If the deleted preset was the current default, the default
     * pointer is cleared in the same write so it can't dangle.
     *
     * @param name the preset to delete; a no-op if absent
     */
    public void deletePreset(String name) {
        store.removePreset(name);
        if (name != null && name.equals(store.getDefaultName())){
            store.setDefaultName(null);
        }
        persist();
    }

    /** Return the raw stored overrides for name (empty map if absent), without resolving against headers. */
    public LinkedHashMap<String, Boolean> getOverrides(String name) { return store.getOverrides(name); }

    /**
     * Resolve a preset to concrete visibility for a specific roster: each present header takes the preset's
     * override if it names that column, else the factory default. 
     *
     * @param headers the roster's current headers, in display order
     * @param name the preset to resolve (an absent/unknown name resolves to pure factory defaults)
     * @return an insertion-ordered {@code header -> visible} map covering exactly {@code headers}
     */
    public LinkedHashMap<String, Boolean> resolveFor(List<String> headers, String name) {
        return VisibilityResolver.resolve(headers, store.getOverrides(name));
    }

    // ---------- internals ----------

    /**
     * Load the store from disk, replacing the in-memory state. 
     * 
     * Resets to empty when the file is missing.
     * A parse failure or too-new version quarantines the file and falls back to an empty store, never throwing errors.
     */
    private void load() {
        store = new ViewPresetStore();
        if (file == null || !Files.exists(file)) return;
        try {
            ViewPresetStore parsed = ViewPresetJson.parse(Files.readString(file));
            if (parsed.getVersion() > ViewPresetStore.CURRENT_VERSION) {
                throw new IllegalArgumentException("unsupported version " + parsed.getVersion());
            }
            store = parsed; // migrate-on-load is a no-op while only v1 exists
        } catch (Exception ex) {
            quarantine(ex);
            store = new ViewPresetStore();
        }
    }

    /**
     * Move an unreadable presets file aside to view-presets.bak so the app self-heals while preserving the original 
     * for recovery.
     * 
     * @param ex the failure that triggered quarantine, included in the log line
     */
    private void quarantine(Exception ex) {
        Path bak = file.resolveSibling("view-presets.bak");
        System.err.println("[HappyCamper] unreadable presets file; quarantined to " + bak + " (" + ex.getMessage() + ")");
        try {
            Files.move(file, bak, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException io) {
            try { Files.move(file, bak, StandardCopyOption.REPLACE_EXISTING); }
            catch (IOException io2) { System.err.println("[HappyCamper] could not quarantine: " + io2.getMessage()); }
        }
    }

    /**
     * Write the current store to disk atomically: serialize to a .tmp file, then move it
     * onto the target so a crash mid-save can't corrupt the live file.
     * Falls back to a non-atomic move on filesystems that reject it.
     */
    private void persist() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("view-presets.json.tmp");
            Files.writeString(tmp, ViewPresetJson.toJson(store));
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            System.err.println("[HappyCamper] could not save presets: " + ex.getMessage());
        }
    }
}
