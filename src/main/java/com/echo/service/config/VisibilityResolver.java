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
