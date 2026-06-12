package com.echo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.Camper;
import com.echo.domain.RosterHeader;
import com.echo.feature.PreferenceFeature;

/**
 * Tests for the standalone demand sort (B2): orders campers by their preference rank for one activity.
 */
public class DemandSortServiceTest {

    @BeforeEach
    public void setUp() {
        new PreferenceFeature(); // initialize static exempt list used by preference parsing
    }

    private Camper camper(String id, String prefsCsv) {
        Map<String, String> data = new HashMap<>();
        if (prefsCsv != null) data.put(RosterHeader.PREFERENCES.standardName, prefsCsv);
        return new Camper(id, data);
    }

    @Test
    @DisplayName("Rank is the original 1-based preference position")
    public void testRankOriginalPosition() {
        Camper c = camper("c", "A, B, C, D, E");
        assertEquals(1, DemandSort.rankFor(c, "A"));
        assertEquals(4, DemandSort.rankFor(c, "D"));
    }

    @Test
    @DisplayName("Campers who did not list the activity rank UNRANKED (sort last)")
    public void testUnranked() {
        Camper c = camper("c", "A, B, C");
        assertEquals(DemandSort.UNRANKED, DemandSort.rankFor(c, "Z"));

        Camper noPrefs = camper("c2", null);
        assertEquals(DemandSort.UNRANKED, DemandSort.rankFor(noPrefs, "A"));
    }

    @Test
    @DisplayName("Comparator orders most-wanted first, non-wanters last")
    public void testComparatorOrder() {
        Camper wantsFirst = camper("first", "Archery, Sailing");   // Archery rank 1
        Camper wantsThird = camper("third", "X, Y, Archery");      // Archery rank 3
        Camper doesntWant = camper("none", "Sailing, Biking");      // Archery unranked

        List<Camper> campers = new ArrayList<>(List.of(doesntWant, wantsThird, wantsFirst));
        campers.sort(DemandSort.comparator("Archery"));

        assertEquals("first", campers.get(0).getId());
        assertEquals("third", campers.get(1).getId());
        assertEquals("none", campers.get(2).getId());
    }

    @Test
    @DisplayName("Demand sort is independent of assignment (includes fulfilled wanters)")
    public void testIndependentOfAssignment() {
        // Whether the camper was assigned Archery is irrelevant; rank is purely from preferences.
        Camper c = camper("c", "Archery, Sailing");
        assertTrue(DemandSort.rankFor(c, "Archery") < DemandSort.rankFor(c, "Sailing"));
    }
}
