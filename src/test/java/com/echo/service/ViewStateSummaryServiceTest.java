package com.echo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the non-UI view-state status line composer (B1 / B2).
 */
public class ViewStateSummaryServiceTest {

    @Test
    @DisplayName("Reset state renders bare count")
    public void testBareCount() {
        String s = new ViewStateSummary().setCounts(40, 40).compose();
        assertEquals("Showing 40 of 40 campers", s);
    }

    @Test
    @DisplayName("Search segment renders with quoted term")
    public void testSearchSegment() {
        String s = new ViewStateSummary()
                .setCounts(12, 40)
                .setSearch("ar", null)
                .compose();
        assertEquals("Showing 12 of 40 campers · Search: \"ar\"", s);
    }

    @Test
    @DisplayName("Search segment includes scope when provided")
    public void testSearchSegmentWithScope() {
        String s = new ViewStateSummary()
                .setCounts(12, 40)
                .setSearch("ar", "Assigned Activities")
                .compose();
        assertEquals("Showing 12 of 40 campers · Search: \"ar\" in Assigned Activities", s);
    }

    @Test
    @DisplayName("Blank search term is omitted")
    public void testBlankSearchOmitted() {
        String s = new ViewStateSummary()
                .setCounts(40, 40)
                .setSearch("   ", "All Names")
                .compose();
        assertEquals("Showing 40 of 40 campers", s);
    }

    @Test
    @DisplayName("Assignment-mode activity segment with round scope")
    public void testActivityAssignmentMode() {
        String s = new ViewStateSummary()
                .setCounts(8, 40)
                .setActivity("Archery or Sailing", "Round 2", false)
                .compose();
        assertEquals("Showing 8 of 40 campers · Activity: Archery or Sailing (Round 2)", s);
    }

    @Test
    @DisplayName("Demand-mode activity segment: unmet only, no round, named sort")
    public void testActivityDemandMode() {
        String s = new ViewStateSummary()
                .setCounts(5, 40)
                .setActivity("Archery or Sailing", null, true)
                .setSort("demand for Archery or Sailing")
                .compose();
        assertEquals(
            "Showing 5 of 40 campers · Activity: Archery or Sailing · unmet only · Sorted by: demand for Archery or Sailing",
            s);
    }

    @Test
    @DisplayName("Empty activity set contributes nothing")
    public void testEmptyActivityOmitted() {
        String s = new ViewStateSummary()
                .setCounts(40, 40)
                .setActivity("", null, false)
                .compose();
        assertEquals("Showing 40 of 40 campers", s);
    }

    @Test
    @DisplayName("Combined search + activity + sort")
    public void testCombined() {
        String s = new ViewStateSummary()
                .setCounts(3, 40)
                .setSearch("ar", null)
                .setActivity("Archery", null, true)
                .setSort("demand for Archery")
                .compose();
        assertEquals(
            "Showing 3 of 40 campers · Search: \"ar\" · Activity: Archery · unmet only · Sorted by: demand for Archery",
            s);
    }
}
