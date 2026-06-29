# Column Visibility Presets — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add named, persisted column-visibility presets (with an opt-in default applied on roster load) to happycamper-heart, established as a clean persistence canary for v3.

**Architecture:** A dependency-free persistence core (`com.echo.service.config`: cross-platform `ConfigLocation`, pure `VisibilityResolver`, hand-rolled `ViewPresetJson`, and `ViewPresetService`) sits beneath a Swing UI layer (a new `PresetSelector` inside the existing `ColumnVisibilityDialog`, plus a default-apply hook in `MainWindow.setRoster`). Presets are per-column *overrides* on the `RosterHeader.defaultVisibility` baseline, stored as JSON in the OS app-data dir.

**Tech Stack:** Java 22, Maven, Swing/AWT, JUnit (existing test conventions). No new dependencies.

**Companion spec:** `happycamper-public/docs/design/column-visibility-presets-design.md` — read it for rationale.

## Global Constraints

- **Java 22 / Maven.** Full suite: `mvn -B test`. Compile single tests with the surefire `-Dtest=` filter.
- **No new dependencies.** JSON is hand-rolled (mirrors `com.echo.web.RosterJson`). Do not add Jackson/Gson/etc.
- **No commits or releases by the implementer.** Per repo workflow (CLAUDE.md) the maintainer reviews the working tree and commits. Every task ends with a **Checkpoint**: run the tests, then surface a *suggested* commit message — do **not** `git commit`, tag, or bump the pom `<version>`.
- **Cross-platform paths:** use `java.nio.file.Path`/`Files` only; never pass `File.separator` to a regex (`String.split`). OS detection via `System.getProperty("os.name")` lowercased.
- **Headless test naming:** name pure/service tests `…ServiceTest`/`…ResolverTest`/`…LocationTest` and selector tests `…SelectorTest` so they land in CI's headless subset (`*SelectorTest, *FilterTest, *FeatureTest, *ServiceTest, *RosterTest`).
- **Forgiving by construction:** persistence/apply must never throw into the UI or fail app startup. Corrupt file ⇒ quarantine + empty store.
- **Visibility-only presets.** Sizing/sort/filter are out of scope.

---

## File Structure

**Persistence core (new package `com.echo.service.config`):**
- `ConfigLocation.java` — resolves the per-OS app-data dir + presets file path (pure resolver + production accessor).
- `VisibilityResolver.java` — pure functions: factory default per header, resolve(headers, overrides), computeOverrides(headers, working).
- `ViewPresetStore.java` — in-memory model (version, default name, name→overrides).
- `ViewPresetJson.java` — hand-rolled JSON write + forgiving parse.
- `ViewPresetService.java` — load/persist/mutate/resolve; quarantine; atomic write; production singleton.

**UI layer:**
- `com.echo.ui.selector.PresetSelector.java` — new selector (rows: default-radio · name(apply) · delete; Update/Revert/Save).
- `com.echo.ui.dialog.ColumnVisibilityDialog.java` — *modify*: add `PresetSelector`, chain dirty callback, refactor reset-to-default onto `VisibilityResolver`.
- `com.echo.ui.component.RosterTable.java` — *modify*: pass the service when opening the dialog.
- `com.echo.ui.MainWindow.java` — *modify*: apply default preset in `setRoster`; (optional) toolbar "Open presets folder…".

**Tests (new):**
- `src/test/java/com/echo/service/config/ConfigLocationTest.java`
- `src/test/java/com/echo/service/config/VisibilityResolverTest.java`
- `src/test/java/com/echo/service/config/ViewPresetJsonTest.java`
- `src/test/java/com/echo/service/config/ViewPresetServiceTest.java`
- `src/test/java/com/echo/ui/dialog/selector/PresetSelectorTest.java`

---

## Task 1: `ConfigLocation` — cross-platform path resolver

**Files:**
- Create: `src/main/java/com/echo/service/config/ConfigLocation.java`
- Test: `src/test/java/com/echo/service/config/ConfigLocationTest.java`

**Interfaces:**
- Produces:
  - `public static Path resolveDir(String osName, Map<String,String> env, Path home)` — pure, no I/O.
  - `public static Path presetsFilePath()` — production: creates the dir, returns `…/view-presets.json`.
  - `public static final String PRESETS_FILE = "view-presets.json"`.

- [ ] **Step 1: Write the failing test**

```java
// src/test/java/com/echo/service/config/ConfigLocationTest.java
package com.echo.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigLocationTest {
    private static final Path HOME = Path.of("/home/user");

    @Test void macUsesApplicationSupport() {
        Path dir = ConfigLocation.resolveDir("Mac OS X", Map.of(), HOME);
        assertEquals(Path.of("/home/user/Library/Application Support/HappyCamper"), dir);
    }

    @Test void windowsUsesAppData() {
        Path dir = ConfigLocation.resolveDir("Windows 11",
            Map.of("APPDATA", "C:\\Users\\u\\AppData\\Roaming"), HOME);
        assertEquals(Path.of("C:\\Users\\u\\AppData\\Roaming", "HappyCamper"), dir);
    }

    @Test void windowsFallsBackToHomeWhenAppDataMissing() {
        Path dir = ConfigLocation.resolveDir("Windows 10", Map.of(), HOME);
        assertEquals(HOME.resolve("HappyCamper"), dir);
    }

    @Test void linuxUsesXdgWhenSet() {
        Path dir = ConfigLocation.resolveDir("Linux",
            Map.of("XDG_CONFIG_HOME", "/home/user/.config"), HOME);
        assertEquals(Path.of("/home/user/.config", "happycamper"), dir);
    }

    @Test void linuxFallsBackToDotConfig() {
        Path dir = ConfigLocation.resolveDir("Linux", Map.of(), HOME);
        assertEquals(HOME.resolve(".config").resolve("happycamper"), dir);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=ConfigLocationTest`
Expected: FAIL — `ConfigLocation` does not exist / compilation error.

- [ ] **Step 3: Write minimal implementation**

```java
// src/main/java/com/echo/service/config/ConfigLocation.java
package com.echo.service.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Resolves the OS-native app-data location for HappyCamper's persisted config. */
public final class ConfigLocation {

    public static final String PRESETS_FILE = "view-presets.json";

    private ConfigLocation() {}

    /** Pure: decide the config directory from inputs, with no filesystem access. */
    public static Path resolveDir(String osName, Map<String, String> env, Path home) {
        String os = osName == null ? "" : osName.toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            return home.resolve("Library").resolve("Application Support").resolve("HappyCamper");
        }
        if (os.contains("win")) {
            String appData = env.get("APPDATA");
            Path base = (appData != null && !appData.isBlank()) ? Path.of(appData) : home;
            return base.resolve("HappyCamper");
        }
        String xdg = env.get("XDG_CONFIG_HOME");
        Path base = (xdg != null && !xdg.isBlank()) ? Path.of(xdg) : home.resolve(".config");
        return base.resolve("happycamper");
    }

    /** Production accessor: resolves, creates the directory, returns the presets file path. */
    public static Path presetsFilePath() {
        Path dir = resolveDir(System.getProperty("os.name"),
                              System.getenv(),
                              Path.of(System.getProperty("user.home")));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("[HappyCamper] could not create config dir " + dir + ": " + e.getMessage());
        }
        return dir.resolve(PRESETS_FILE);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -Dtest=ConfigLocationTest`
Expected: PASS (5 tests).

- [ ] **Step 5: Checkpoint (maintainer commits)**

Run full suite `mvn -B test` (sanity). Surface suggested message — do not commit:
`feat(config): cross-platform ConfigLocation resolver for app-data dir`

---

## Task 2: `VisibilityResolver` — pure baseline/override logic

**Files:**
- Create: `src/main/java/com/echo/service/config/VisibilityResolver.java`
- Test: `src/test/java/com/echo/service/config/VisibilityResolverTest.java`

**Interfaces:**
- Consumes: `com.echo.domain.RosterHeader.determineHeaderType(String)` → enum|null; `RosterHeader.defaultVisibility` (boolean field).
- Produces:
  - `public static boolean factoryDefault(String header)` — enum default, else `true`.
  - `public static LinkedHashMap<String,Boolean> resolve(List<String> headers, Map<String,Boolean> overrides)`.
  - `public static LinkedHashMap<String,Boolean> computeOverrides(List<String> headers, Map<String,Boolean> working)`.

- [ ] **Step 1: Write the failing test**

```java
// src/test/java/com/echo/service/config/VisibilityResolverTest.java
package com.echo.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VisibilityResolverTest {

    // "First Name" is defaultVisibility=false in RosterHeader; "Last Name" is true.
    @Test void factoryDefaultReadsEnum() {
        assertFalse(VisibilityResolver.factoryDefault("First Name"));
        assertTrue(VisibilityResolver.factoryDefault("Last Name"));
    }

    @Test void factoryDefaultUnknownColumnIsVisible() {
        assertTrue(VisibilityResolver.factoryDefault("Custom CSV Column"));
    }

    @Test void resolveInheritsDefaultsWhenNoOverride() {
        var resolved = VisibilityResolver.resolve(List.of("First Name", "Last Name"), Map.of());
        assertFalse(resolved.get("First Name")); // factory hidden
        assertTrue(resolved.get("Last Name"));   // factory visible
    }

    @Test void resolveAppliesOverridesBothDirections() {
        Map<String,Boolean> ov = Map.of("First Name", true, "Last Name", false);
        var resolved = VisibilityResolver.resolve(List.of("First Name", "Last Name"), ov);
        assertTrue(resolved.get("First Name"));  // revealed
        assertFalse(resolved.get("Last Name"));  // hidden
    }

    @Test void resolveDropsOverridesForAbsentColumns() {
        Map<String,Boolean> ov = Map.of("Gone Column", false);
        var resolved = VisibilityResolver.resolve(List.of("Last Name"), ov);
        assertEquals(1, resolved.size());
        assertTrue(resolved.containsKey("Last Name"));
        assertFalse(resolved.containsKey("Gone Column"));
    }

    @Test void computeOverridesRecordsOnlyDeviations() {
        // working: First Name shown (deviates, default false), Last Name shown (matches default true)
        LinkedHashMap<String,Boolean> working = new LinkedHashMap<>();
        working.put("First Name", true);
        working.put("Last Name", true);
        var ov = VisibilityResolver.computeOverrides(List.of("First Name", "Last Name"), working);
        assertEquals(Map.of("First Name", true), ov);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=VisibilityResolverTest`
Expected: FAIL — `VisibilityResolver` not defined.

- [ ] **Step 3: Write minimal implementation**

```java
// src/main/java/com/echo/service/config/VisibilityResolver.java
package com.echo.service.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.echo.domain.RosterHeader;

/** Pure logic mapping the factory baseline + per-column overrides to concrete visibility. */
public final class VisibilityResolver {

    private VisibilityResolver() {}

    /** Factory-default visibility for a column: the RosterHeader default, or visible for unknown columns. */
    public static boolean factoryDefault(String header) {
        RosterHeader rh = RosterHeader.determineHeaderType(header);
        return rh == null ? true : rh.defaultVisibility;
    }

    /** Resolve each present header to override-if-named, else factory default. Absent overrides are ignored. */
    public static LinkedHashMap<String, Boolean> resolve(List<String> headers, Map<String, Boolean> overrides) {
        LinkedHashMap<String, Boolean> out = new LinkedHashMap<>();
        for (String h : headers) {
            out.put(h, overrides.containsKey(h) ? overrides.get(h) : factoryDefault(h));
        }
        return out;
    }

    /** Record only the columns whose working visibility deviates from the factory default. */
    public static LinkedHashMap<String, Boolean> computeOverrides(List<String> headers, Map<String, Boolean> working) {
        LinkedHashMap<String, Boolean> out = new LinkedHashMap<>();
        for (String h : headers) {
            if (!working.containsKey(h)) continue;
            boolean v = working.get(h);
            if (v != factoryDefault(h)) out.put(h, v);
        }
        return out;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -Dtest=VisibilityResolverTest`
Expected: PASS (6 tests). If `First Name`/`Last Name` default assumptions mismatch the enum, open `RosterHeader.java` and pick one column known-hidden and one known-visible; adjust the test literals (the resolver code is unaffected).

- [ ] **Step 5: Checkpoint (maintainer commits)**

`mvn -B test`. Suggested message:
`feat(config): pure VisibilityResolver (baseline + overrides)`

---

## Task 3: `ViewPresetStore` + `ViewPresetJson` — model and hand-rolled JSON

**Files:**
- Create: `src/main/java/com/echo/service/config/ViewPresetStore.java`
- Create: `src/main/java/com/echo/service/config/ViewPresetJson.java`
- Test: `src/test/java/com/echo/service/config/ViewPresetJsonTest.java`

**Interfaces:**
- Produces (`ViewPresetStore`):
  - `static final int CURRENT_VERSION = 1`
  - `int getVersion()` / `void setVersion(int)`
  - `String getDefaultName()` / `void setDefaultName(String)` (nullable)
  - `LinkedHashMap<String,LinkedHashMap<String,Boolean>> getPresets()`
  - `void putPreset(String name, LinkedHashMap<String,Boolean> overrides)`
  - `void removePreset(String name)`
  - `boolean hasPreset(String name)`
  - `LinkedHashMap<String,Boolean> getOverrides(String name)` (empty map if absent)
- Produces (`ViewPresetJson`):
  - `static String toJson(ViewPresetStore store)`
  - `static ViewPresetStore parse(String json)` — throws `IllegalArgumentException` on malformed input.

- [ ] **Step 1: Write the failing test**

```java
// src/test/java/com/echo/service/config/ViewPresetJsonTest.java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=ViewPresetJsonTest`
Expected: FAIL — classes not defined.

- [ ] **Step 3a: Write `ViewPresetStore`**

```java
// src/main/java/com/echo/service/config/ViewPresetStore.java
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

    public void putPreset(String name, LinkedHashMap<String, Boolean> overrides) {
        presets.put(name, new LinkedHashMap<>(overrides));
    }

    public void removePreset(String name) { presets.remove(name); }

    public boolean hasPreset(String name) { return presets.containsKey(name); }

    public LinkedHashMap<String, Boolean> getOverrides(String name) {
        LinkedHashMap<String, Boolean> ov = presets.get(name);
        return ov == null ? new LinkedHashMap<>() : ov;
    }
}
```

- [ ] **Step 3b: Write `ViewPresetJson` (writer + forgiving parser)**

```java
// src/main/java/com/echo/service/config/ViewPresetJson.java
package com.echo.service.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dependency-free JSON for the view-presets file (mirrors com.echo.web.RosterJson's hand-rolled style).
 * Schema is shallow on purpose: { version:int, default:string|null, presets:{ name:{ overrides:{ col:bool } } } }.
 */
public final class ViewPresetJson {

    private ViewPresetJson() {}

    // ---------- write ----------

    public static String toJson(ViewPresetStore store) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\n");
        sb.append("  \"version\": ").append(store.getVersion()).append(",\n");
        sb.append("  \"default\": ");
        if (store.getDefaultName() == null) sb.append("null");
        else sb.append('"').append(escape(store.getDefaultName())).append('"');
        sb.append(",\n");
        sb.append("  \"presets\": {");
        var presets = store.getPresets();
        if (!presets.isEmpty()) sb.append('\n');
        int i = 0;
        for (Map.Entry<String, LinkedHashMap<String, Boolean>> e : presets.entrySet()) {
            sb.append("    \"").append(escape(e.getKey())).append("\": { \"overrides\": {");
            var ov = e.getValue();
            int j = 0;
            for (Map.Entry<String, Boolean> oe : ov.entrySet()) {
                sb.append(j == 0 ? " " : ", ");
                sb.append('"').append(escape(oe.getKey())).append("\": ").append(oe.getValue().booleanValue());
                j++;
            }
            sb.append(ov.isEmpty() ? "} }" : " } }");
            sb.append(++i < presets.size() ? ",\n" : "\n");
        }
        if (!presets.isEmpty()) sb.append("  ");
        sb.append("}\n}\n");
        return sb.toString();
    }

    private static String escape(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        return b.toString();
    }

    // ---------- read ----------

    public static ViewPresetStore parse(String json) {
        if (json == null) throw new IllegalArgumentException("null json");
        P p = new P(json);
        Object root = p.parseValueTop();
        if (!(root instanceof Map)) throw new IllegalArgumentException("root is not an object");
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) root;

        ViewPresetStore store = new ViewPresetStore();
        Object v = m.get("version");
        if (v instanceof Long l) store.setVersion(l.intValue());
        Object d = m.get("default");
        store.setDefaultName(d instanceof String s ? s : null);

        Object presetsObj = m.get("presets");
        if (presetsObj instanceof Map<?, ?> pm) {
            for (Map.Entry<?, ?> e : pm.entrySet()) {
                LinkedHashMap<String, Boolean> ov = new LinkedHashMap<>();
                if (e.getValue() instanceof Map<?, ?> body && body.get("overrides") instanceof Map<?, ?> om) {
                    for (Map.Entry<?, ?> oe : om.entrySet()) {
                        if (oe.getValue() instanceof Boolean b) ov.put(String.valueOf(oe.getKey()), b);
                    }
                }
                store.putPreset(String.valueOf(e.getKey()), ov);
            }
        }
        return store;
    }

    /** Minimal recursive-descent parser for objects/strings/numbers/booleans/null (no arrays needed). */
    private static final class P {
        private final String s;
        private int i;

        P(String s) { this.s = s; }

        Object parseValueTop() {
            ws();
            Object val = value();
            ws();
            if (i != s.length()) throw err("trailing content");
            return val;
        }

        private Object value() {
            ws();
            if (i >= s.length()) throw err("unexpected end");
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> object();
                case '"' -> string();
                case 't', 'f' -> bool();
                case 'n' -> nul();
                default -> {
                    if (c == '-' || (c >= '0' && c <= '9')) yield number();
                    throw err("unexpected char '" + c + "'");
                }
            };
        }

        private Map<String, Object> object() {
            expect('{');
            LinkedHashMap<String, Object> m = new LinkedHashMap<>();
            ws();
            if (peek() == '}') { i++; return m; }
            while (true) {
                ws();
                if (peek() != '"') throw err("expected key string");
                String key = string();
                ws(); expect(':');
                m.put(key, value());
                ws();
                char c = next();
                if (c == ',') continue;
                if (c == '}') break;
                throw err("expected ',' or '}'");
            }
            return m;
        }

        private String string() {
            expect('"');
            StringBuilder b = new StringBuilder();
            while (true) {
                if (i >= s.length()) throw err("unterminated string");
                char c = s.charAt(i++);
                if (c == '"') break;
                if (c == '\\') {
                    char e = next();
                    switch (e) {
                        case '"' -> b.append('"');
                        case '\\' -> b.append('\\');
                        case '/' -> b.append('/');
                        case 'b' -> b.append('\b');
                        case 'f' -> b.append('\f');
                        case 'n' -> b.append('\n');
                        case 'r' -> b.append('\r');
                        case 't' -> b.append('\t');
                        case 'u' -> {
                            if (i + 4 > s.length()) throw err("bad unicode escape");
                            b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> throw err("bad escape \\" + e);
                    }
                } else {
                    b.append(c);
                }
            }
            return b.toString();
        }

        private Object number() {
            int start = i;
            if (peek() == '-') i++;
            while (i < s.length() && "0123456789+-.eE".indexOf(s.charAt(i)) >= 0) i++;
            String num = s.substring(start, i);
            try {
                if (num.matches("-?\\d+")) return Long.parseLong(num);
                return Double.parseDouble(num);
            } catch (NumberFormatException ex) {
                throw err("bad number '" + num + "'");
            }
        }

        private Object bool() {
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            throw err("bad literal");
        }

        private Object nul() {
            if (s.startsWith("null", i)) { i += 4; return null; }
            throw err("bad literal");
        }

        private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        private char peek() { if (i >= s.length()) throw err("unexpected end"); return s.charAt(i); }
        private char next() { if (i >= s.length()) throw err("unexpected end"); return s.charAt(i++); }
        private void expect(char c) { if (next() != c) throw err("expected '" + c + "'"); }
        private IllegalArgumentException err(String msg) { return new IllegalArgumentException("JSON parse error at " + i + ": " + msg); }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -Dtest=ViewPresetJsonTest`
Expected: PASS (6 tests).

- [ ] **Step 5: Checkpoint (maintainer commits)**

`mvn -B test`. Suggested message:
`feat(config): ViewPresetStore model + dependency-free JSON read/write`

---

## Task 4: `ViewPresetService` — load, persist, mutate, resolve

**Files:**
- Create: `src/main/java/com/echo/service/config/ViewPresetService.java`
- Test: `src/test/java/com/echo/service/config/ViewPresetServiceTest.java`

**Interfaces:**
- Consumes: `ConfigLocation.presetsFilePath()`, `ViewPresetJson.toJson/parse`, `VisibilityResolver.resolve`, `ViewPresetStore`.
- Produces:
  - `public ViewPresetService(Path presetsFile)` — testable; loads immediately (quarantining corrupt files).
  - `public static ViewPresetService getInstance()` — lazy production singleton over `ConfigLocation.presetsFilePath()`.
  - `public List<String> listPresets()`
  - `public String getDefaultName()`
  - `public void setDefault(String nameOrNull)` — persists.
  - `public void savePreset(String name, LinkedHashMap<String,Boolean> overrides)` — persists.
  - `public void deletePreset(String name)` — persists; clears default if it pointed here.
  - `public LinkedHashMap<String,Boolean> getOverrides(String name)`
  - `public LinkedHashMap<String,Boolean> resolveFor(List<String> headers, String name)`

- [ ] **Step 1: Write the failing test**

```java
// src/test/java/com/echo/service/config/ViewPresetServiceTest.java
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
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=ViewPresetServiceTest`
Expected: FAIL — `ViewPresetService` not defined.

- [ ] **Step 3: Write minimal implementation**

```java
// src/main/java/com/echo/service/config/ViewPresetService.java
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

    public ViewPresetService(Path presetsFile) {
        this.file = presetsFile;
        load();
    }

    public static synchronized ViewPresetService getInstance() {
        if (instance == null) {
            Path f = ConfigLocation.presetsFilePath();
            System.out.println("[HappyCamper] view presets file: " + f);
            instance = new ViewPresetService(f);
        }
        return instance;
    }

    public List<String> listPresets() { return new ArrayList<>(store.getPresets().keySet()); }

    public String getDefaultName() { return store.getDefaultName(); }

    public void setDefault(String nameOrNull) { store.setDefaultName(nameOrNull); persist(); }

    public void savePreset(String name, LinkedHashMap<String, Boolean> overrides) {
        store.putPreset(name, overrides);
        persist();
    }

    public void deletePreset(String name) {
        store.removePreset(name);
        if (name != null && name.equals(store.getDefaultName())) store.setDefaultName(null);
        persist();
    }

    public LinkedHashMap<String, Boolean> getOverrides(String name) { return store.getOverrides(name); }

    public LinkedHashMap<String, Boolean> resolveFor(List<String> headers, String name) {
        return VisibilityResolver.resolve(headers, store.getOverrides(name));
    }

    // ---------- internals ----------

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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -Dtest=ViewPresetServiceTest`
Expected: PASS (5 tests).

- [ ] **Step 5: Checkpoint (maintainer commits)**

`mvn -B test`. Suggested message:
`feat(config): ViewPresetService with quarantine + atomic writes`

---

## Task 5: `PresetSelector` — the dialog widget

**Files:**
- Create: `src/main/java/com/echo/ui/selector/PresetSelector.java`
- Test: `src/test/java/com/echo/ui/dialog/selector/PresetSelectorTest.java`

**Interfaces:**
- Consumes: `InputSelector<String>` (base: `setUpdateCallback`, `notifyUpdateCallback`, `setComponentHeight`, abstract `getValue/setValue/hasSelection/buildSelectorPanel`); `CheckBoxSelector` (`getValue()/setValue(Map)`); `ViewPresetService`; `VisibilityResolver.computeOverrides`; `HoverButton`, `HoverRadioButton` (`com.echo.ui.elements`); `DialogUtils.createAlignedBoxPanel()`.
- Produces:
  - `public PresetSelector(String title, ViewPresetService service, CheckBoxSelector visibilitySelector, List<String> headers)`
  - `public String getValue()` — active preset name (nullable)
  - `public void onWorkingStateChanged()` — recompute dirty + refresh Update/Revert enabled state
  - `public boolean isDirty()` — for tests

**Behavioral contract (encode all of this):**
- Rows come from `service.listPresets()`. Each row: a `HoverRadioButton` (default, single `ButtonGroup` shared with a "None" radio) + a `HoverButton` showing the preset name (apply on click) + a `HoverButton` "×" (delete).
- Apply: `visibilitySelector.setValue(service.resolveFor(headers, name))`, set `activeName=name`, `onWorkingStateChanged()`, `notifyUpdateCallback()`.
- Default radio: selecting a row → `service.setDefault(name)`; selecting "None" → `service.setDefault(null)`.
- Delete "×": `service.deletePreset(name)`; if it was active, `activeName=null`; rebuild rows.
- Save (`+ Save current columns as preset…`): `JOptionPane.showInputDialog` for a name (ignore null/blank); `overrides = VisibilityResolver.computeOverrides(headers, visibilitySelector.getValue())`; `service.savePreset(name, overrides)`; `activeName=name`; rebuild.
- Update (enabled only when dirty): `service.savePreset(activeName, computeOverrides(...))`; refresh.
- Revert (enabled only when dirty): `visibilitySelector.setValue(service.resolveFor(headers, activeName))`; refresh.
- Dirty = `activeName != null` AND `visibilitySelector.getValue()` differs from `service.resolveFor(headers, activeName)`.

- [ ] **Step 1: Write the failing test**

```java
// src/test/java/com/echo/ui/dialog/selector/PresetSelectorTest.java
package com.echo.ui.dialog.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.echo.service.ColumnSettings; // not needed; placeholder removed below
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
```

> NOTE: delete the stray `import com.echo.service.ColumnSettings;` line — it slipped in; it is not used.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=PresetSelectorTest`
Expected: FAIL — `PresetSelector` not defined.

- [ ] **Step 3: Write minimal implementation**

```java
// src/main/java/com/echo/ui/selector/PresetSelector.java
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

    @Override public void setValue(String name) {
        if (name == null || !service.listPresets().contains(name)) return;
        visibilitySelector.setValue(service.resolveFor(headers, name));
        activeName = name;
        onWorkingStateChanged();
        notifyUpdateCallback();
    }

    @Override public boolean hasSelection() { return true; } // never blocks the dialog's Apply

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

    @Override
    protected void buildSelectorPanel(JPanel panel) {
        this.content = panel;
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        rebuild();
    }

    private void rebuild() {
        content.removeAll();
        ButtonGroup defaultGroup = new ButtonGroup();

        HoverRadioButton none = new HoverRadioButton("Default: none");
        none.setSelected(service.getDefaultName() == null);
        none.addActionListener(e -> service.setDefault(null));
        defaultGroup.add(none);
        content.add(none);

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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -Dtest=PresetSelectorTest`
Expected: PASS (3 tests). These run headless (no dialog shown) — building the panel and calling methods directly, per the existing selector-test pattern. If `HoverRadioButton("")` rejects empty text, pass a single space `" "`.

- [ ] **Step 5: Checkpoint (maintainer commits)**

`mvn -B test`. Suggested message:
`feat(ui): PresetSelector widget for managing column-visibility presets`

---

## Task 6: Wire `PresetSelector` into `ColumnVisibilityDialog`

**Files:**
- Modify: `src/main/java/com/echo/ui/dialog/ColumnVisibilityDialog.java`
- Modify: `src/main/java/com/echo/ui/component/RosterTable.java` (pass the service when opening the dialog)

**Interfaces:**
- Consumes: `PresetSelector`, `ViewPresetService.getInstance()`, `VisibilityResolver.resolve`.
- Produces: dialog now constructs with a `ViewPresetService`; preset selector at `selectors[0]`.

- [ ] **Step 1: Add the service parameter + selector to `ColumnVisibilityDialog`**

Change the constructor and `createSelectors` to accept a `ViewPresetService`, prepend a `PresetSelector`, and re-index the casts. Replace the constructor header/cast block (lines ~42-51) and the `createSelectors` signature + return array.

```java
// constructor — add ViewPresetService param, pass into createSelectors
public ColumnVisibilityDialog(Window parent, EnhancedRoster roster, JTable table, ViewPresetService presetService) {
    super(parent, true, "Column Settings", createSelectors(roster, table, presetService), COLUMN_DIALOG_WIDTH, "Apply");
    this.roster = roster;
    this.table = table;

    presetSelector        = (PresetSelector) selectors[0];
    actionButtonSelector  = (ActionButtonSelector) selectors[1];
    columnVisibilitySelector = (CheckBoxSelector) selectors[2];
    columnSizingSelector  = (RadioButtonSelector<ColumnSizingOption>) selectors[3];
    customWidthSelector   = (NumberInputSelector) selectors[4];

    // Chain the visibility callback so dirty state recomputes alongside the Apply-button validity.
    columnVisibilitySelector.setUpdateCallback(() -> {
        updateContinueButton();
        presetSelector.onWorkingStateChanged();
    });

    columnSizingSelector.setUpdateCallback(() -> {
        boolean enableCustomWidth = columnSizingSelector.getValue() == ColumnSizingOption.CUSTOM_WIDTH;
        customWidthSelector.setEnabled(enableCustomWidth);
        updateContinueButton();
    });
    boolean enableCustomWidth = columnSizingSelector.getValue() == ColumnSizingOption.CUSTOM_WIDTH;
    customWidthSelector.setEnabled(enableCustomWidth);
}
```

Add the field near the other selector fields (top of class):
```java
private PresetSelector presetSelector;
```

Add imports:
```java
import com.echo.service.config.ViewPresetService;
import com.echo.service.config.VisibilityResolver;
import com.echo.ui.selector.PresetSelector;
```

- [ ] **Step 2: Update `createSelectors` to build + prepend the PresetSelector**

```java
private static InputSelector<?>[] createSelectors(EnhancedRoster roster, JTable table, ViewPresetService presetService) {
    // ... existing code that builds columnMap and `visibilitySelector` (unchanged) ...
    CheckBoxSelector visibilitySelector = new CheckBoxSelector("Column Visibility", columnMap, false);

    PresetSelector presetSelector = new PresetSelector(
        "Presets", presetService, visibilitySelector, roster.getAllHeaders());

    ActionButtonSelector actionSelector = new ActionButtonSelector(/* ... unchanged ... */);
    RadioButtonSelector<ColumnSizingOption> sizingSelector = /* ... unchanged ... */;
    NumberInputSelector widthSelector = /* ... unchanged ... */;

    return new InputSelector<?>[] {
        presetSelector,   // [0]
        actionSelector,   // [1]
        visibilitySelector, // [2]
        sizingSelector,   // [3]
        widthSelector     // [4]
    };
}
```

- [ ] **Step 3: Refactor `visibility_resetToDefault` onto `VisibilityResolver` (kills the duplicate baseline logic)**

```java
private static void visibility_resetToDefault(EnhancedRoster roster, CheckBoxSelector visibilitySelector){
    visibilitySelector.setValue(VisibilityResolver.resolve(roster.getAllHeaders(), java.util.Map.of()));
}
```

- [ ] **Step 4: Update the dialog's caller in `RosterTable`**

In `RosterTable.showColumnVisibilityDialog()` (~line 248), pass the service:
```java
ColumnVisibilityDialog dialog = new ColumnVisibilityDialog(
    parentWindow, roster, this, com.echo.service.config.ViewPresetService.getInstance());
```
(Keep the surrounding `resetCachedSettings()` call and parent-window lookup exactly as they are; only the constructor call gains the 4th argument.)

- [ ] **Step 5: Build to verify it compiles + run the headless suite**

Run: `mvn -B test -Dtest=*SelectorTest,*ServiceTest,*ResolverTest,*LocationTest`
Expected: PASS. Then a full compile: `mvn -B test-compile`.
Expected: BUILD SUCCESS (the dialog wiring compiles; dialog-opening tests need a display and run under CI xvfb).

- [ ] **Step 6: Checkpoint (maintainer commits)**

`mvn -B test`. Suggested message:
`feat(ui): integrate PresetSelector into ColumnVisibilityDialog`

---

## Task 7: Apply the default preset on roster load (`MainWindow.setRoster`)

**Files:**
- Modify: `src/main/java/com/echo/ui/MainWindow.java`

**Interfaces:**
- Consumes: `ViewPresetService.getInstance()`, `resolveFor`, `getDefaultName`, `listPresets`.

- [ ] **Step 1: Add a service field to `MainWindow`**

Near the other fields:
```java
private final com.echo.service.config.ViewPresetService viewPresetService =
    com.echo.service.config.ViewPresetService.getInstance();
```

- [ ] **Step 2: Apply the default preset inside `setRoster`**

In `setRoster(EnhancedRoster roster)`, immediately after `roster.resetHeaderVisibility();` (the line that applies factory defaults, ~line 546) and before `rosterTable.setRoster(roster, filterManager);` (~line 552), insert:

```java
// Apply the opt-in default preset over the factory baseline (forgiving: only affects existing columns).
String defaultPreset = viewPresetService.getDefaultName();
if (defaultPreset != null && viewPresetService.listPresets().contains(defaultPreset)) {
    var resolved = viewPresetService.resolveFor(roster.getAllHeaders(), defaultPreset);
    for (var entry : resolved.entrySet()) {
        roster.setHeaderVisibility(entry.getKey(), entry.getValue());
    }
}
```

- [ ] **Step 3: Compile + sanity-test**

Run: `mvn -B test-compile`
Expected: BUILD SUCCESS.
Run: `mvn -B test -Dtest=*ServiceTest,*ResolverTest`
Expected: PASS (no regressions in the persistence core).

- [ ] **Step 4: Manual smoke (maintainer, optional but recommended)**

Run the app (`mvn -B package -DskipTests` then launch the jar). Save a preset that hides a couple columns, mark it default, reload a roster → those columns start hidden. Confirms the load hook end-to-end.

- [ ] **Step 5: Checkpoint (maintainer commits)**

`mvn -B test`. Suggested message:
`feat(ui): apply opt-in default preset on roster load`

---

## Task 8 (optional): "Open presets folder…" toolbar button

> Optional discoverability/recovery affordance (spec §5.4 / §6). The startup `System.out` log from Task 4 already prints the path; this adds a one-click way to reach it (and any quarantined `.bak`). Include it if you want the recovery story complete now; otherwise defer.

**Files:**
- Modify: `src/main/java/com/echo/ui/MainWindow.java`

- [ ] **Step 1: Add the button in `createControlPanel()` (~line 207)**

```java
HoverButton presetsFolderButton = new HoverButton("Presets Folder…");
presetsFolderButton.addActionListener(e -> openPresetsFolder());
buttonPanel.add(presetsFolderButton); // match the local panel/variable name used by neighboring buttons
```
(Use whatever local panel variable the surrounding buttons are added to, and the same `HoverButton` import already present in MainWindow.)

- [ ] **Step 2: Add the handler method**

```java
private void openPresetsFolder() {
    try {
        java.nio.file.Path dir = com.echo.service.config.ConfigLocation.presetsFilePath().getParent();
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(dir.toFile());
        }
    } catch (Exception ex) {
        System.err.println("[HappyCamper] could not open presets folder: " + ex.getMessage());
    }
}
```

- [ ] **Step 3: Compile**

Run: `mvn -B test-compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Checkpoint (maintainer commits)**

Suggested message: `feat(ui): toolbar shortcut to open the presets folder`

---

## Post-implementation (maintainer)

- Log the change in `~/Desktop/HappyCamper-pending-patches.md` (symptoms/root-cause/diff/porting notes) for the v3 port — include the v3 boundary (sibling to `ExportConfig`, not `CampConfig`) and deferred follow-ups (Explore/show-all starter preset; per-checkbox diff highlighting).
- **Patch note must flag the `ViewPresetService` singleton as an interim wiring choice, not the clean solution.** The testable core is DI-friendly (constructor-injected `Path`); the production `getInstance()` singleton exists only to fit heart's static-heavy dialog code. v3 should wire the service via proper dependency injection, not global state (see design §5.3).
- Do **not** bump the pom `<version>` or release as part of this work — those are separate, explicitly-instructed steps.

---

## Self-Review (completed by plan author)

**Spec coverage:** ConfigLocation (§5.1→T1) · forgiving resolve/overrides (§4,§6→T2) · JSON schema + hand-rolled read/write (§4,§5.2→T3) · service load/persist/quarantine/atomic/version (§5.3,§6→T4) · PresetSelector with disabled-not-hidden Update/Revert, radio default, per-row delete, save-via-prompt (§7→T5,T6) · default-on-roster-load (§3,§8→T7) · discoverability affordance + path log (§5.4→T4 log, T8 button) · cross-platform QA via pure resolver + headless-named tests (§5.4→T1,T2,T4) · no commits/release (global constraint, §10) · v3 boundary + patch record (§10,§11→Post-implementation). No gaps found.

**Placeholder scan:** No TBD/TODO; every code step shows complete code. One intentional inline note (stray import to delete in the PresetSelector test) is called out explicitly, not left as a guess.

**Type consistency:** `resolveFor(List<String>, String)`, `computeOverrides(List<String>, Map)`, `resolve(List<String>, Map)`, `savePreset(String, LinkedHashMap)`, `setDefault(String)` are used identically across T4–T7. Selector indices (`selectors[0..4]`) match the return array order in T6. `ViewPresetService` constructor `(Path)` vs `getInstance()` used consistently (tests use the former, UI the latter).
