package com.echo.filter;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;

/**
 * The outcome of evaluating one filter's assertion over an entire roster.
 *
 * Part of the assertion-indicator prototype: each sidebar filter that carries a pass/fail meaning
 * reports one of these, and its header renders a green or red dot accordingly. Filters with no
 * pass/fail meaning return {@link #none()} and render unchanged.
 *
 * Always evaluated over the whole roster, never the currently-visible rows — a filter must not be
 * able to turn its own indicator green by hiding its failures.
 *
 * @param applicable   false means "this filter is not an assertion"; the header renders unchanged
 * @param failureCount how many units fail; 0 means satisfied
 * @param unit         the noun being counted, singular ("camper", "program"), for tooltip text
 * @param claim        the plain-English assertion, shown as the header tooltip
 */
public record AssertionResult(boolean applicable, int failureCount, String unit, String claim) {

    private static final AssertionResult NONE = new AssertionResult(false, 0, "", "");

    /**
     * @return the shared "this filter is not an assertion" result
     */
    public static AssertionResult none() {
        return NONE;
    }

    /**
     * @param failureCount how many units fail the assertion
     * @param unit singular noun being counted, e.g. "camper"
     * @param claim the plain-English assertion
     * @return an applicable result
     */
    public static AssertionResult of(int failureCount, String unit, String claim) {
        return new AssertionResult(true, failureCount, unit, claim);
    }

    /**
     * @return true if this is an assertion and no unit fails it
     */
    public boolean satisfied() {
        return applicable && failureCount == 0;
    }

    /**
     * Human-readable status for the header tooltip.
     *
     * @return "satisfied", "1 camper fails", "3 campers fail", or "" when not applicable
     */
    public String statusText() {
        if (!applicable) {
            return "";
        }
        if (failureCount == 0) {
            return "satisfied";
        }
        return failureCount == 1
            ? "1 " + unit + " fails"
            : failureCount + " " + unit + "s fail";
    }

    /**
     * The shared shape for flag-column assertions: the roster satisfies the assertion when the
     * given column is empty for every camper. Covers the preference, aquatic-conflict, swim-lesson,
     * and duplicate-activity filters, whose apply() methods all key on exactly this convention.
     *
     * @param roster the roster to evaluate; null yields {@link #none()}
     * @param header the flag column; a roster lacking it yields {@link #none()}
     * @param unit singular noun being counted, normally "camper"
     * @param claim the plain-English assertion
     * @return the assertion outcome over the whole roster
     */
    public static AssertionResult forFlagColumn(EnhancedRoster roster, RosterHeader header,
                                                String unit, String claim) {
        if (roster == null || !roster.hasHeader(header)) {
            return none();
        }

        int failures = 0;
        for (Camper camper : roster.getCampers()) {
            if (!DataConstants.isEmpty(camper.getValue(header.standardName))) {
                failures++;
            }
        }
        return of(failures, unit, claim);
    }
}
