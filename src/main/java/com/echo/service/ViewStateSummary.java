package com.echo.service;

/**
 * Non-UI helper that composes the shared view-state status line shown beneath the roster table.
 *
 * All segment formatting and empty-omission logic lives here so it is fully unit-testable without a
 * display. The status-bar Swing component is a thin label that simply renders {@link #compose()}.
 *
 * Example output:
 * {@code Showing 12 of 40 campers · Search: "ar" · Activity: Archery or Sailing · unmet only · Sorted by: demand for Archery or Sailing}
 *
 * In its reset state (no search, no activity set, no sort) it renders the bare
 * {@code Showing N of N campers}.
 */
public class ViewStateSummary {

    private static final String SEP = " · ";

    private int visibleCount;
    private int totalCount;
    private String searchTerm;
    private String searchScope;
    private String activitySummary;
    private String roundScope;
    private boolean unmetOnly;
    private String sortDescription;

    public ViewStateSummary setCounts(int visibleCount, int totalCount) {
        this.visibleCount = visibleCount;
        this.totalCount = totalCount;
        return this;
    }

    /** @param term the active search text (null/blank omits the search segment) */
    public ViewStateSummary setSearch(String term, String scope) {
        this.searchTerm = term;
        this.searchScope = scope;
        return this;
    }

    /**
     * @param activitySummary the OR-joined activity set (e.g. "Archery or Sailing"); null/blank omits
     * @param roundScope round label (e.g. "Round 2") or null when N/A
     * @param unmetOnly true in demand mode where the population is restricted to unmet wanters
     */
    public ViewStateSummary setActivity(String activitySummary, String roundScope, boolean unmetOnly) {
        this.activitySummary = activitySummary;
        this.roundScope = roundScope;
        this.unmetOnly = unmetOnly;
        return this;
    }

    /** @param sortDescription human description of the active ordering (e.g. "demand for Archery"); null omits */
    public ViewStateSummary setSort(String sortDescription) {
        this.sortDescription = sortDescription;
        return this;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Composes the full status line from the current state, omitting any empty segment.
     */
    public String compose() {
        StringBuilder sb = new StringBuilder();
        sb.append("Showing ").append(visibleCount).append(" of ").append(totalCount).append(" campers");

        if (!isBlank(searchTerm)) {
            sb.append(SEP).append("Search: \"").append(searchTerm.trim()).append("\"");
            if (!isBlank(searchScope)) {
                sb.append(" in ").append(searchScope);
            }
        }

        if (!isBlank(activitySummary)) {
            sb.append(SEP).append("Activity: ").append(activitySummary);
            if (!isBlank(roundScope)) {
                sb.append(" (").append(roundScope).append(")");
            }
            if (unmetOnly) {
                sb.append(SEP).append("unmet only");
            }
        }

        if (!isBlank(sortDescription)) {
            sb.append(SEP).append("Sorted by: ").append(sortDescription);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return compose();
    }
}
