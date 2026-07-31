package com.echo.filter;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;

/**
 * Tests for the assertion-indicator prototype: AssertionResult, the shared flag-column check,
 * and each filter's checkAssertion outcome.
 *
 * See docs/superpowers/specs/2026-07-31-assertion-indicators-design.md
 */
public class AssertionTest {

    /**
     * Builds a roster registering only the given flag column, with one camper per supplied value.
     */
    static EnhancedRoster rosterWithFlagValues(RosterHeader header, String... values) {
        EnhancedRoster roster = new EnhancedRoster();
        roster.addHeader(header);
        int i = 1;
        for (String value : values) {
            Map<String, String> data = new HashMap<>();
            data.put(header.standardName, value);
            roster.addCamper(new Camper("c" + i++, data));
        }
        return roster;
    }

    @Test
    @DisplayName("none() is not applicable and never satisfied")
    public void testNone() {
        AssertionResult none = AssertionResult.none();
        assertFalse(none.applicable());
        assertFalse(none.satisfied());
        assertEquals("", none.statusText());
    }

    @Test
    @DisplayName("of() with zero failures is satisfied")
    public void testSatisfied() {
        AssertionResult result = AssertionResult.of(0, "camper", "Nothing is wrong");
        assertTrue(result.applicable());
        assertTrue(result.satisfied());
        assertEquals(0, result.failureCount());
        assertEquals("satisfied", result.statusText());
    }

    @Test
    @DisplayName("of() with failures is applicable but not satisfied")
    public void testViolated() {
        AssertionResult result = AssertionResult.of(3, "camper", "Nothing is wrong");
        assertTrue(result.applicable());
        assertFalse(result.satisfied());
        assertEquals(3, result.failureCount());
    }

    @Test
    @DisplayName("statusText pluralizes the unit")
    public void testStatusTextPluralization() {
        assertEquals("1 camper fails", AssertionResult.of(1, "camper", "c").statusText());
        assertEquals("3 campers fail", AssertionResult.of(3, "camper", "c").statusText());
        assertEquals("1 program fails", AssertionResult.of(1, "program", "c").statusText());
        assertEquals("2 programs fail", AssertionResult.of(2, "program", "c").statusText());
    }

    @Test
    @DisplayName("forFlagColumn counts campers whose flag column is non-empty")
    public void testForFlagColumnCountsNonEmpty() {
        EnhancedRoster roster = rosterWithFlagValues(RosterHeader.SWIMCONFLICTS,
            DataConstants.DISPLAY_EMPTY, "Canoeing", "", "Swimming", null);

        AssertionResult result = AssertionResult.forFlagColumn(
            roster, RosterHeader.SWIMCONFLICTS, "camper", "no conflicts");

        assertTrue(result.applicable());
        assertEquals(2, result.failureCount());
        assertFalse(result.satisfied());
    }

    @Test
    @DisplayName("forFlagColumn is satisfied when every flag column value is empty")
    public void testForFlagColumnAllEmpty() {
        EnhancedRoster roster = rosterWithFlagValues(RosterHeader.SWIMCONFLICTS,
            DataConstants.DISPLAY_EMPTY, "", "   ");

        AssertionResult result = AssertionResult.forFlagColumn(
            roster, RosterHeader.SWIMCONFLICTS, "camper", "no conflicts");

        assertTrue(result.satisfied());
        assertEquals(0, result.failureCount());
    }

    @Test
    @DisplayName("forFlagColumn returns none() when the column is absent")
    public void testForFlagColumnMissingColumn() {
        EnhancedRoster roster = rosterWithFlagValues(RosterHeader.SWIMCONFLICTS, "Canoeing");

        AssertionResult result = AssertionResult.forFlagColumn(
            roster, RosterHeader.DUPLICATE_ACTIVITY, "camper", "no duplicates");

        assertFalse(result.applicable());
    }

    @Test
    @DisplayName("forFlagColumn returns none() for a null roster")
    public void testForFlagColumnNullRoster() {
        AssertionResult result = AssertionResult.forFlagColumn(
            null, RosterHeader.SWIMCONFLICTS, "camper", "no conflicts");

        assertFalse(result.applicable());
    }

    @Test
    @DisplayName("A filter that does not override checkAssertion returns none()")
    public void testDefaultCheckAssertionIsNone() {
        RosterFilter bare = new RosterFilter() {
            @Override
            public boolean apply(Camper camper) {
                return true;
            }

            @Override
            public String getFilterId() {
                return "bare";
            }

            @Override
            public String getFilterName() {
                return "Bare";
            }

            @Override
            public com.echo.ui.filter.CollapsibleFilterPanel createFilterPanel() {
                return null;
            }
        };

        assertFalse(bare.checkAssertion(new EnhancedRoster()).applicable());
    }
}
