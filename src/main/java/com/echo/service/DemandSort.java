package com.echo.service;

import java.util.Comparator;
import java.util.List;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.RosterHeader;
import com.echo.feature.PreferenceFeatureUtils;

/**
 * "Sort by demand for an activity" (B2) — a standalone ordering, independent of any filter.
 *
 * Orders campers by how much they wanted one chosen activity: their <b>original</b> preference rank for
 * it ({@code parsedPreferences.indexOf(activity) + 1}). Lower = more wanted, so most-wanted sorts first;
 * campers who did not list the activity sort last. It is a pure sort — it hides no one.
 *
 * This is the headless-testable core; the top-bar control and the table ordering hook are thin wiring.
 */
public final class DemandSort {

    /** Rank used for campers who did not list the activity (sorts them last). */
    public static final int UNRANKED = Integer.MAX_VALUE;

    private DemandSort() {
        // static utility
    }

    /**
     * The camper's original preference rank for an activity (1 = first choice), or {@link #UNRANKED}
     * if they did not list it.
     *
     * @param camper the camper
     * @param activity the activity to rank demand for
     * @return 1-based original preference rank, or UNRANKED
     */
    public static int rankFor(Camper camper, String activity) {
        if (DataConstants.isEmpty(activity)) {
            return UNRANKED;
        }
        String prefField = camper.getValue(RosterHeader.PREFERENCES.standardName);
        if (DataConstants.isEmpty(prefField)) {
            return UNRANKED;
        }
        List<String> preferences = PreferenceFeatureUtils.parsePreferenceField(prefField);
        int idx = preferences.indexOf(activity);
        return idx < 0 ? UNRANKED : idx + 1;
    }

    /**
     * @param activity the activity to order demand for
     * @return a comparator ordering campers by ascending demand rank (most wanted first)
     */
    public static Comparator<Camper> comparator(String activity) {
        return Comparator.comparingInt(camper -> rankFor(camper, activity));
    }
}
