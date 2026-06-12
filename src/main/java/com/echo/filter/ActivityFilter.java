package com.echo.filter;

import java.util.LinkedHashSet;
import java.util.Set;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.feature.ActivityFeature;
import com.echo.ui.filter.ActivityFilterPanel;
import com.echo.ui.filter.CollapsibleFilterPanel;

/**
 * Activity restrict filter (B2): a conventional multi-select activity filter.
 *
 * A camper passes iff any selected activity is assigned to them within the round scope (Any / R1 / R2 /
 * R3). An empty selection is inactive (passes everyone). It is a plain visibility filter like the others
 * — it does not drive any sort. "Sort by demand for an activity" is a separate top-bar control
 * ({@link com.echo.service.DemandSort}); the two compose freely.
 *
 * Gated on the activity feature in {@code FilterManager.createFiltersForRoster}. The Swing sidebar panel
 * is built by {@link com.echo.ui.filter.ActivityFilterPanel}.
 */
public class ActivityFilter implements RosterFilter {
    private static final String FILTER_ID = "activity-select";
    public static final String FILTER_NAME = "Activity";

    /** Round scope sentinel meaning "any of R1/R2/R3". */
    public static final int ROUND_ANY = 0;

    private final Set<String> selectedActivities = new LinkedHashSet<>();
    private int roundScope = ROUND_ANY;

    public ActivityFilter() {
        // Empty constructor; inactive until activities are selected
    }

    @Override
    public boolean apply(Camper camper) {
        if (selectedActivities.isEmpty()) {
            return true; // inactive
        }
        if (roundScope == ROUND_ANY) {
            for (int round = 1; round <= ActivityFeature.MAX_ROUNDS; round++) {
                if (selectedActivities.contains(ActivityFeature.getActivityForCamper(camper, round))) {
                    return true;
                }
            }
            return false;
        }
        return selectedActivities.contains(ActivityFeature.getActivityForCamper(camper, roundScope));
    }

    // ---- selection / scope accessors ----

    public Set<String> getSelectedActivities() {
        return selectedActivities;
    }

    public void addActivity(String activity) {
        selectedActivities.add(activity);
    }

    public void removeActivity(String activity) {
        selectedActivities.remove(activity);
    }

    public void setRoundScope(int roundScope) {
        this.roundScope = roundScope;
    }

    public int getRoundScope() {
        return roundScope;
    }

    /** Resets to the inactive default (empty set, Any round). */
    public void clear() {
        selectedActivities.clear();
        roundScope = ROUND_ANY;
    }

    /** @return the OR-joined activity summary (e.g. "Archery or Sailing"), or "" when empty. */
    public String getActivitySummary() {
        return String.join(" or ", selectedActivities);
    }

    /** @return the round label (e.g. "Round 2"), or null for Any. */
    public String getRoundLabel() {
        if (roundScope == ROUND_ANY) {
            return null;
        }
        return RosterHeader.buildRoundString(roundScope);
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
    public CollapsibleFilterPanel createFilterPanel(EnhancedRoster roster) {
        // The panel needs the roster to compute the derived activity catalog.
        return ActivityFilterPanel.build(this, roster);
    }

    @Override
    public CollapsibleFilterPanel createFilterPanel() {
        // Fallback when no roster is available; the catalog will be empty.
        return new CollapsibleFilterPanel(FILTER_NAME);
    }
}
