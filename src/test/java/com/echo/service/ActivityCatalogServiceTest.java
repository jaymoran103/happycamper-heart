package com.echo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.feature.PreferenceFeature;

/**
 * Tests for the derived ActivityCatalog (shared by B2 and D1).
 */
public class ActivityCatalogServiceTest {

    @BeforeEach
    public void setUp() {
        new PreferenceFeature(); // initialize static exempt list used by preference parsing
    }

    private Camper camper(String prefsCsv, String r1, String r2, String r3) {
        Map<String, String> data = new HashMap<>();
        if (prefsCsv != null) data.put(RosterHeader.PREFERENCES.standardName, prefsCsv);
        if (r1 != null) data.put(RosterHeader.ROUND_1.standardName, r1);
        if (r2 != null) data.put(RosterHeader.ROUND_2.standardName, r2);
        if (r3 != null) data.put(RosterHeader.ROUND_3.standardName, r3);
        return new Camper("id_", data);
    }

    @Test
    @DisplayName("Catalog is the distinct, sorted union of preferences and assignments")
    public void testCatalogUnion() {
        EnhancedRoster roster = new EnhancedRoster();
        roster.addCamper(camper("Archery, Sailing", "Archery", "Biking", null)); // Biking only assigned
        roster.addCamper(camper("Sailing, Fishing", "Fishing", null, null));

        List<String> catalog = ActivityCatalog.build(roster);

        // Distinct union: Archery, Biking, Fishing, Sailing — alphabetical
        assertEquals(List.of("Archery", "Biking", "Fishing", "Sailing"), catalog);
    }

    @Test
    @DisplayName("Empty/blank values are excluded")
    public void testExcludesEmpty() {
        EnhancedRoster roster = new EnhancedRoster();
        roster.addCamper(camper("Archery", "Archery", " - ", null));

        List<String> catalog = ActivityCatalog.build(roster);
        assertTrue(catalog.contains("Archery"));
        assertFalse(catalog.contains(" - "));
        assertEquals(1, catalog.size());
    }

    @Test
    @DisplayName("Null roster yields empty catalog")
    public void testNullRoster() {
        assertTrue(ActivityCatalog.build(null).isEmpty());
    }
}
