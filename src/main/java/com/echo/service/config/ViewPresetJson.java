package com.echo.service.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dependency-free, self-contained JSON for the view-presets file.
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
