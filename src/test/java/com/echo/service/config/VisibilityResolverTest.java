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
