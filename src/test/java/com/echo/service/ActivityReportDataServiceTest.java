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
import com.echo.service.ActivityReportData.ActivityRow;

public class ActivityReportDataServiceTest {

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
        return new Camper("test_" + Math.abs(prefsCsv.hashCode()), data);
    }

    private ActivityRow rowFor(List<ActivityRow> rows, String activity) {
        return rows.stream().filter(r -> r.activity().equals(activity)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Null roster returns empty list")
    public void testNullRoster() {
        assertTrue(ActivityReportData.compute(null).isEmpty());
    }

    @Test
    @DisplayName("Round counts are correct per round")
    public void testRoundCounts() {
        EnhancedRoster roster = new EnhancedRoster();
        // Archery: one in R1, one in R2
        roster.addCamper(camper("Archery, Sailing", "Archery", null, null));
        roster.addCamper(camper("Sailing, Archery", null, "Archery", null));
        roster.addCamper(camper("Sailing", "Sailing", null, null));

        List<ActivityRow> rows = ActivityReportData.compute(roster);
        ActivityRow archery = rowFor(rows, "Archery");

        assertEquals(1, archery.r1());
        assertEquals(1, archery.r2());
        assertEquals(0, archery.r3());
    }

    @Test
    @DisplayName("Avg rank (in): mean rank among enrolled campers who preferenced the activity")
    public void testAvgRankIn() {
        EnhancedRoster roster = new EnhancedRoster();
        // Camper 1: Archery is rank 1 (first pref), enrolled in Archery R1
        roster.addCamper(camper("Archery, Sailing", "Archery", null, null));
        // Camper 2: Archery is rank 2 (second pref), enrolled in Archery R1
        roster.addCamper(camper("Sailing, Archery", "Archery", null, null));

        ActivityRow archery = rowFor(ActivityReportData.compute(roster), "Archery");

        // avg rank in = (1 + 2) / 2 = 1.5
        assertEquals(1.5, archery.avgRankIn(), 0.001);
    }

    @Test
    @DisplayName("Avg rank (not in): mean rank among non-enrolled campers who preferenced the activity")
    public void testAvgRankOut() {
        EnhancedRoster roster = new EnhancedRoster();
        // Enrolled in Archery (rank 1)
        roster.addCamper(camper("Archery, Sailing", "Archery", null, null));
        // NOT enrolled in Archery; preferenced it at rank 1
        roster.addCamper(camper("Archery, Sailing", "Sailing", null, null));
        // NOT enrolled in Archery; preferenced it at rank 2
        roster.addCamper(camper("Sailing, Archery", "Sailing", null, null));

        ActivityRow archery = rowFor(ActivityReportData.compute(roster), "Archery");

        // avg rank out = (1 + 2) / 2 = 1.5
        assertEquals(1.5, archery.avgRankOut(), 0.001);
        // avg rank in = 1 / 1 = 1.0 (only the enrolled camper who requested it)
        assertEquals(1.0, archery.avgRankIn(), 0.001);
    }

    @Test
    @DisplayName("Campers not preferencing an activity don't contribute to either average")
    public void testNonPreferencerExcluded() {
        EnhancedRoster roster = new EnhancedRoster();
        // Enrolled in Archery but did NOT preference it
        roster.addCamper(camper("Sailing", "Archery", null, null));

        ActivityRow archery = rowFor(ActivityReportData.compute(roster), "Archery");

        assertEquals(1, archery.r1());
        assertTrue(Double.isNaN(archery.avgRankIn()),  "avg rank in should be NaN when no preferencers enrolled");
        assertTrue(Double.isNaN(archery.avgRankOut()), "avg rank out should be NaN when no preferencers not enrolled");
    }

    @Test
    @DisplayName("toRow returns numeric types for integer columns and String for avg cols")
    public void testToRowTypes() {
        EnhancedRoster roster = new EnhancedRoster();
        roster.addCamper(camper("Archery", "Archery", null, null));

        ActivityRow row = rowFor(ActivityReportData.compute(roster), "Archery");
        List<Object> tableRow = ActivityReportData.toRow(row);

        assertFalse(tableRow.isEmpty());
        assertEquals("Archery", tableRow.get(0));
        assertTrue(tableRow.get(1) instanceof Integer, "R1 should be Integer");
        assertTrue(tableRow.get(2) instanceof Integer, "R2 should be Integer");
        assertTrue(tableRow.get(3) instanceof Integer, "R3 should be Integer");
    }

    @Test
    @DisplayName("toCsvRow formats NaN avg as dash")
    public void testCsvNaN() {
        EnhancedRoster roster = new EnhancedRoster();
        // Enrolled but not preferenced → both averages undefined
        roster.addCamper(camper("Sailing", "Archery", null, null));

        ActivityRow row = rowFor(ActivityReportData.compute(roster), "Archery");
        List<String> csv = ActivityReportData.toCsvRow(row);

        assertEquals("—", csv.get(4), "Avg Rank (In) should be — when no data");
        assertEquals("—", csv.get(5), "Avg Rank (Not In) should be — when no data");
    }

    @Test
    @DisplayName("Header row has correct column count and order")
    public void testHeaderRow() {
        List<String> headers = ActivityReportData.headerRow();
        assertEquals(6, headers.size());
        assertEquals("Activity",       headers.get(0));
        assertEquals("R1",             headers.get(1));
        assertEquals("R2",             headers.get(2));
        assertEquals("R3",             headers.get(3));
        assertEquals("Avg Rank (In)",      headers.get(4));
        assertEquals("Avg Rank (Out)",  headers.get(5));
    }
}
