package com.echo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.feature.ActivityFeature;
import com.echo.feature.PreferenceFeatureUtils;

/**
 * Builds the derived activity catalog shared by the Activity filter (B2) and the demand reports (D1).
 *
 * There is no master list of activities in the data model, so the catalog is computed once from the
 * roster: the distinct, non-empty union of every camper's parsed preferences and their assigned
 * activities across all rounds. Returned sorted for stable, readable presentation.
 */
public final class ActivityCatalog {

    private ActivityCatalog() {
        // static utility
    }

    /**
     * Computes the distinct, non-empty activity catalog for a roster.
     *
     * @param roster the roster to scan (campers' preferences + round assignments)
     * @return alphabetically-sorted distinct activity names
     */
    public static List<String> build(EnhancedRoster roster) {
        TreeSet<String> catalog = new TreeSet<>();
        if (roster == null) {
            return new ArrayList<>();
        }

        for (Camper camper : roster.getCampers()) {
            // Parsed preferences
            String prefField = camper.getValue(RosterHeader.PREFERENCES.standardName);
            if (!DataConstants.isEmpty(prefField)) {
                for (String pref : PreferenceFeatureUtils.parsePreferenceField(prefField)) {
                    if (!DataConstants.isEmpty(pref)) {
                        catalog.add(pref);
                    }
                }
            }

            // Assigned activities, all rounds
            for (int round = 1; round <= ActivityFeature.MAX_ROUNDS; round++) {
                String assignment = ActivityFeature.getActivityForCamper(camper, round);
                if (!DataConstants.isEmpty(assignment)) {
                    catalog.add(assignment);
                }
            }
        }

        return new ArrayList<>(catalog);
    }
}
