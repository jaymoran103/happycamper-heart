package com.echo.filter;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.feature.SwimLevelFeature;

/**
 * Tests for the SwimLessonFilter (C1) and its coexistence with SwimLevelFilter under one feature gate.
 */
public class SwimLessonFilterTest {
    private SwimLessonFilter filter;

    @BeforeEach
    public void setUp() {
        filter = new SwimLessonFilter();
    }

    private Camper createCamper(String lessonValue) {
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.camperRosterName, "Test");
        data.put(RosterHeader.SWIMLESSON.standardName, lessonValue);
        return new Camper("id_", data);
    }

    @Test
    @DisplayName("Filter id and name")
    public void testFilterIdAndName() {
        assertEquals("swimlesson", filter.getFilterId());
        assertEquals("Swim Lesson Validity", filter.getFilterName());
    }

    @Test
    @DisplayName("Valid and flagged campers toggle independently")
    public void testApplyToggles() {
        Camper valid = createCamper(DataConstants.DISPLAY_EMPTY);
        Camper flagged = createCamper(SwimLevelFeature.FLAG_RED_MISSING);

        assertTrue(filter.apply(valid));
        assertTrue(filter.apply(flagged));

        filter.setShowValidCampers(false);
        assertFalse(filter.apply(valid));
        assertTrue(filter.apply(flagged));

        filter.setShowValidCampers(true);
        filter.setShowFlaggedCampers(false);
        assertTrue(filter.apply(valid));
        assertFalse(filter.apply(flagged));
    }

    @Test
    @DisplayName("Both swim filters registered under one swimlevel gate, toggling independently")
    public void testBothFiltersUnderOneGate() {
        EnhancedRoster roster = new EnhancedRoster();
        roster.enableFeature("swimlevel");
        FilterManager manager = new FilterManager();
        manager.createFiltersForRoster(roster);

        assertTrue(manager.hasFilter("swimlevel"));
        assertTrue(manager.hasFilter("swimlesson"));

        // Each apply()s independently: hide lesson-flagged, keep aquatic filter open
        SwimLessonFilter lessonFilter = (SwimLessonFilter) manager.getFilter("swimlesson");
        lessonFilter.setShowFlaggedCampers(false);

        Camper flaggedLesson = createCamper(SwimLevelFeature.FLAG_NONRED_ASSIGNED);
        // The lesson filter rejects it; the aquatic (swimlevel) filter has no opinion on a SWIMLESSON-only camper
        assertFalse(manager.getFilter("swimlesson").apply(flaggedLesson));
        assertTrue(manager.getFilter("swimlevel").apply(flaggedLesson));
    }
}
