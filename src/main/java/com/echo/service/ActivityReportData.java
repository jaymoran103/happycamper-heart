package com.echo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.feature.ActivityFeature;
import com.echo.feature.PreferenceFeatureUtils;

/**
 * Read-only aggregation for the Activity Report (D1).
 *
 * Produces one row per catalog activity: enrollment counts by round, and average preference rank
 * split between campers who are in the activity vs. those who are not. Campers who did not
 * preference an activity are excluded from both averages.
 *
 * See docs/sprint-conventions.md §D1.
 */
public final class ActivityReportData {

    private static final String BLANK_RANK = "—";

    private ActivityReportData() {}

    /** One row of the Activity Report. NaN avg fields mean no preferencers in that group. */
    public record ActivityRow(String activity, int r1, int r2, int r3,
                              double avgRankIn, double avgRankOut) {}

    /**
     * Computes one ActivityRow per catalog activity, in alphabetical order.
     */
    public static List<ActivityRow> compute(EnhancedRoster roster) {
        if (roster == null) {
            return new ArrayList<>();
        }

        List<String> catalog = ActivityCatalog.build(roster);

        // Per-activity accumulators: [r1Count, r2Count, r3Count, rankInSum, rankInCount, rankOutSum, rankOutCount]
        Map<String, int[]> acc = new HashMap<>();
        for (String activity : catalog) {
            acc.put(activity, new int[7]);
        }

        for (Camper camper : roster.getCampers()) {
            String prefField = camper.getValue(RosterHeader.PREFERENCES.standardName);
            List<String> prefs = DataConstants.isEmpty(prefField)
                    ? new ArrayList<>() : PreferenceFeatureUtils.parsePreferenceField(prefField);

            String[] assignments = new String[ActivityFeature.MAX_ROUNDS];
            for (int i = 0; i < ActivityFeature.MAX_ROUNDS; i++) {
                assignments[i] = ActivityFeature.getActivityForCamper(camper, i + 1);
            }

            // Enrollment counts
            for (int i = 0; i < ActivityFeature.MAX_ROUNDS; i++) {
                String a = assignments[i];
                if (!DataConstants.isEmpty(a) && acc.containsKey(a)) {
                    acc.get(a)[i]++; // r1=0, r2=1, r3=2
                }
            }

            // Preference rank contributions
            for (int i = 0; i < prefs.size(); i++) {
                String activity = prefs.get(i);
                if (!acc.containsKey(activity)) {
                    continue;
                }
                int rank = i + 1;
                boolean enrolled = isAssigned(assignments, activity);
                int[] a = acc.get(activity);
                if (enrolled) {
                    a[3] += rank; // rankInSum
                    a[4]++;       // rankInCount
                } else {
                    a[5] += rank; // rankOutSum
                    a[6]++;       // rankOutCount
                }
            }
        }

        List<ActivityRow> rows = new ArrayList<>();
        for (String activity : catalog) {
            int[] a = acc.get(activity);
            double avgIn  = a[4] > 0 ? (double) a[3] / a[4] : Double.NaN;
            double avgOut = a[6] > 0 ? (double) a[5] / a[6] : Double.NaN;
            rows.add(new ActivityRow(activity, a[0], a[1], a[2], avgIn, avgOut));
        }
        return rows;
    }

    private static boolean isAssigned(String[] assignments, String activity) {
        for (String a : assignments) {
            if (activity.equals(a)) return true;
        }
        return false;
    }

    public static List<String> headerRow() {
        return List.of("Activity", "R1", "R2", "R3", "Avg Rank (In)", "Avg Rank (Out)");
    }

    /** Returns Object types for correct numeric sort in JTable. */
    public static List<Object> toRow(ActivityRow row) {
        List<Object> cells = new ArrayList<>();
        cells.add(row.activity());
        cells.add(row.r1());
        cells.add(row.r2());
        cells.add(row.r3());
        cells.add(Double.isNaN(row.avgRankIn())  ? BLANK_RANK : String.format("%.2f", row.avgRankIn()));
        cells.add(Double.isNaN(row.avgRankOut()) ? BLANK_RANK : String.format("%.2f", row.avgRankOut()));
        return cells;
    }

    /** Returns String values for CSV export. */
    public static List<String> toCsvRow(ActivityRow row) {
        List<String> cells = new ArrayList<>();
        cells.add(row.activity());
        cells.add(Integer.toString(row.r1()));
        cells.add(Integer.toString(row.r2()));
        cells.add(Integer.toString(row.r3()));
        cells.add(Double.isNaN(row.avgRankIn())  ? BLANK_RANK : String.format("%.2f", row.avgRankIn()));
        cells.add(Double.isNaN(row.avgRankOut()) ? BLANK_RANK : String.format("%.2f", row.avgRankOut()));
        return cells;
    }
}
