package com.echo.filter;

import java.util.Arrays;
import java.util.List;

import com.echo.domain.Camper;
import com.echo.domain.RosterHeader;
import com.echo.ui.filter.CollapsibleFilterPanel;

/**
 * Universal text search, modelled as an always-on filter (B1).
 *
 * Unlike every other filter, this one is registered unconditionally (not behind a {@code hasFeature}
 * gate) — see docs/sprint-conventions.md. It is inert until a search term is set: an empty term passes
 * every camper. Matching is case-insensitive substring.
 *
 * A scope narrows which fields are searched. The two named presets ("All Names", "Assigned Activities")
 * map to fixed header sets; a per-column scope searches a single header; a null/empty scope searches all
 * of a camper's fields.
 */
public class TextSearchFilter implements RosterFilter {
    private static final String FILTER_ID = "textsearch";
    public static final String FILTER_NAME = "Search";

    public static final String SCOPE_ALL_NAMES = "All Names";
    public static final String SCOPE_ASSIGNED_ACTIVITIES = "Assigned Activities";

    public static final List<String> NAME_FIELDS = List.of(
        RosterHeader.FIRST_NAME.standardName,
        RosterHeader.PREFERRED_NAME.standardName,
        RosterHeader.LAST_NAME.standardName);

    public static final List<String> ACTIVITY_FIELDS = List.of(
        RosterHeader.ROUND_1.standardName,
        RosterHeader.ROUND_2.standardName,
        RosterHeader.ROUND_3.standardName);

    private String searchTerm = "";
    private String scopeLabel = SCOPE_ALL_NAMES;
    private List<String> scopeFields = NAME_FIELDS;

    public TextSearchFilter() {
        // Empty constructor; inert until a term is set
    }

    @Override
    public boolean apply(Camper camper) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return true; // always-on but inert with no term
        }
        String needle = searchTerm.trim().toLowerCase();

        // null/empty scope -> search every field of the camper
        Iterable<String> values = (scopeFields == null || scopeFields.isEmpty())
            ? camper.getData().values()
            : fieldValues(camper, scopeFields);

        for (String value : values) {
            if (value != null && value.toLowerCase().contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> fieldValues(Camper camper, List<String> fields) {
        return fields.stream().map(camper::getValue).toList();
    }

    /** Sets the raw search text. Blank/empty makes the filter inert. */
    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm == null ? "" : searchTerm;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    /** Convenience for the two named presets and "search all fields". */
    public void setScope(String label) {
        this.scopeLabel = label;
        if (SCOPE_ALL_NAMES.equals(label)) {
            this.scopeFields = NAME_FIELDS;
        } else if (SCOPE_ASSIGNED_ACTIVITIES.equals(label)) {
            this.scopeFields = ACTIVITY_FIELDS;
        } else if (label == null) {
            this.scopeFields = null; // all fields
        } else {
            // per-column scope: a single header name
            this.scopeFields = Arrays.asList(label);
        }
    }

    public String getScopeLabel() {
        return scopeLabel;
    }

    /** Directly sets the searched fields, used for a per-column scope. */
    public void setScopeFields(String label, List<String> fields) {
        this.scopeLabel = label;
        this.scopeFields = fields;
    }

    @Override
    public String getFilterId() {
        return FILTER_ID;
    }

    @Override
    public String getFilterName() {
        return FILTER_NAME;
    }

    @Override
    public CollapsibleFilterPanel createFilterPanel() {
        // The universal search renders in the top bar, not the sidebar; no collapsible panel needed.
        return null;
    }
}
