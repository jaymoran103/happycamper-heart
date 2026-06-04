package com.echo.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.logging.RosterWarning;
import com.echo.logging.WarningManager;
import com.echo.validation.RosterRegexBuilder;


/**
 * Feature that evaluates campers' activity preferences against their actual assignments.
 *
 * Main functions:
 * - Assigns each camper a score based on their preference satisfaction
 * - Lists any activities assignments that go against their requests
 * - Tracks 'exempt' activities that are independent of the preference ranking system, so mandatory activities dont skew scores or reports
 *
 * This feature extracts campers' ranked preferences from a comma-separated list, compares them
 * against their actual activity assignments, and calculates a satisfaction score based on how well
 * their preferences were met. The score is added to the roster as a new column.
 *
 * Score algorithm:
 *  Choice Points:
 *      1st choice = 10 points
 *      2nd choice = 9 points
 *      3rd choice = 8 points
 *      9th choice = 2 points
 *      10th choice = 1 point
 *      Not chosen = 0 points
 *
 *  Score = (sum of choice points) / (number of rounds assigned)
 *
 *  Max score = total # of possible points / (number of rounds assigned)
 *      3 rounds = 27 points
 *      2 rounds = 19 points
 *      1 round = 10 points
 *
 *  Satisfaction Score = (Score / Max Score) * 100 (as an integer percentage)
 *
 * Some activities (like Swimming and Horseback Riding) are exempt from preference scoring
 * because they are typically assigned based on signups outside the preference system
 *
 * Example preference fields to parse:
 * "Archery, Sports, Fishing, Water Polo, Skiing, Survivor, Sailing, Paddle Sports, Biking and Open Toed Shoes"
 * "Survivor, Sports, Arts & Crafts, Archery, Skiing, Biking, Paddle Sports, Fishing, Friendship Bracelet and Open Toed Shoes"
 * "Arts & Crafts, Friendship Bracelet, Archery, Sailing, Sports, Nature, Open Toed Shoes, Fairy, Paddle Sports and Gold Swimming"
 *
 * FUTURE - add support for cases with " and " in an activity name (check list of offerings and anticipate those names during the final parsing step)
 * FUTURE - add warnings for cases with name mismatches
 * FUTURE - add support for schedules with more than 3 rounds
 */
public class PreferenceFeature implements RosterFeature {

    private static final String FEATURE_ID = "preference";
    public static final String FEATURE_NAME = "Preference Evaluation";

    private final List<String> requiredHeaders = Arrays.asList(RosterHeader.PREFERENCES.camperRosterName,//FUTURE - Update header name upon conversion so standardName can be expected
                                                               RosterHeader.ROUND_COUNT.standardName);

    private final List<String> addedHeaders = Arrays.asList(RosterHeader.PREFERENCES.standardName,
                                                            RosterHeader.PREFERENCE_SCORE.standardName,
                                                            RosterHeader.PREFERENCE_PERCENTILE.standardName,
                                                            RosterHeader.UNREQUESTED_ACTIVITIES.standardName,
                                                            RosterHeader.SCORE_BY_ROUND.standardName);

// PREFERENCE_SCORE_HEADER, PREFERENCE_PERCENTILE_HEADER, SCORE_BY_ROUND_HEADER);

    public static final int PREFERENCE_COUNT = 10;
    public static final String PENULTIMATE_TOKEN = " and "; //campminder-generated preference sets use " and " to separate the last two items
    private final int ROUNDS_OFFERED = ActivityFeature.MAX_ROUNDS; // Maximum number of activity rounds supported

    // List of activities that are exempt from preference scoring
    // FUTURE - add config file for preference exemptions?
    private static List<String> DEFAULT_EXEMPT_ACTIVITIES = List.of("Swimming", "Horseback Riding");
    private static List<String> EXEMPT_ACTIVITIES;
    private Map<String, Double> camperScores;

    public PreferenceFeature() {
        resetExemptActivities();
    }

    /**
     * Adds an activity to the exempt list
     * @param activity The activity name to exempt from preference scoring
     */
    public static void addExemptActivity(String activity) {
        EXEMPT_ACTIVITIES.add(activity);
    }

    /**
     * Sets the list of exempt activities
     * @param activities List of activity names to exempt from preference scoring
     */
    public static void setExemptActivities(List<String> activities) {
        EXEMPT_ACTIVITIES.clear();
        EXEMPT_ACTIVITIES.addAll(activities);
    }

    /**
     * Gets the list of exempt activities
     * @return The list of exempt activities
     * @return
     */
    public static List<String> getExemptActivities() {
        return EXEMPT_ACTIVITIES;
    }

    /**
     * Resets the exempt activities to the default list;
     */
    public static void resetExemptActivities() {
        EXEMPT_ACTIVITIES = new ArrayList<>(DEFAULT_EXEMPT_ACTIVITIES);
    }

    @Override
    public String getFeatureId() {
        return FEATURE_ID;
    }

    @Override
    public String getFeatureName() {
        return FEATURE_NAME;
    }

    @Override
    public List<String> getRequiredHeaders() {
        return requiredHeaders;
    }

    @Override
    public List<String> getAddedHeaders() {
        return addedHeaders;
    }

    @Override
    public Map<String, String> getRequiredFormats() {
        // Convert Pattern objects to String representations for the interface
        Map<String, Pattern> patternMap = RosterRegexBuilder.buildPreferenceFormats();
        Map<String, String> formats = new HashMap<>();

        for (Map.Entry<String, Pattern> entry : patternMap.entrySet()) {
            formats.put(entry.getKey(), entry.getValue().pattern());
        }

        return formats;
    }

    @Override
    public void applyFeature(EnhancedRoster roster, WarningManager warningManager) {

        camperScores = new HashMap<>();


        for (String header : getAddedHeaders()) {
            roster.addHeader(header);
        }

        // First pass: Calculate and store preference scores for all campers

        for (Camper camper : roster.getCampers()) {
            String preferenceString = camper.getValue(RosterHeader.PREFERENCES.standardName);

            if (!DataConstants.isEmpty(preferenceString)){
                // Apply basic preference calculations
                applyToCamper(camper);
                // camperScores.put(camper.getId(), score);
            } else {
                RosterWarning warning = RosterWarning.create_camperMissingField(camper.getData(),RosterHeader.PREFERENCES.standardName,FEATURE_NAME);
                warningManager.logWarning(warning);
            }
        }

        // Second pass: Calculate percentiles and update campers
        calculateAndSetPercentiles(roster, camperScores);
        camperScores = null;

        //System.out.println("PreferenceFeature.applyFeature: Enabling preference feature");
        roster.enableFeature(FEATURE_ID);
        //System.out.println("PreferenceFeature.applyFeature: Preference feature enabled");
    }

    @Override
    public boolean preValidate(EnhancedRoster roster, WarningManager warningManager) {
        //System.out.println("PreferenceFeature.preValidate: Validating preference feature");

        // Ensure all required headers are present
        boolean lacksHeader = false;
        for (String header : requiredHeaders) {
            //System.out.println("PreferenceFeature.preValidate: Checking for required header: " + header);
            if (!roster.hasHeader(header)) {
                //System.out.println("PreferenceFeature.preValidate: Missing required header: " + header);
                RosterWarning warning = RosterWarning.create_missingFeatureHeader(header, FEATURE_NAME);
                warningManager.logWarning(warning);
                lacksHeader = true; // Mark that we're missing a required header
            } else {
                //System.out.println("PreferenceFeature.preValidate: Found required header: " + header);
            }
        }

        boolean result = !lacksHeader;
        //System.out.println("PreferenceFeature.preValidate: Validation result: " + result);
        return result; // Returns false if any required headers are missing
    }

    @Override
    public boolean postValidate(EnhancedRoster roster, WarningManager warningManager) {
        return true;
    }


    /**
     * Main logic for applying the feature to a single camper
     *
     * First uses 'determine' helper methods to assemble information about camper's preferences and outcome
     * Then uses 'setValue_' helper methods to update the roster with the results
     *
     * @param camper The camper to apply the feature to
     * @return The calculated preference score (as a decimal between 0 and 1)
     */
    private void applyToCamper(Camper camper) {

        // Determine camper's preferences and assignments
        List<String> preferences = PreferenceFeatureUtils.parsePreferenceField(camper.getValue(RosterHeader.PREFERENCES.standardName));
        String[] assignments = new String[ROUNDS_OFFERED];
        for (int i = 0; i < ROUNDS_OFFERED; i++) {
            assignments[i] = ActivityFeature.getActivityForCamper(camper, i+1);
        }

        // Determine camper's unrequested activities
        List<String> unrequestedActivities = PreferenceFeatureUtils.determineUnrequestedActivities(camper,preferences,assignments);

        // Determine points for each round
        int[] roundPoints = PreferenceFeatureUtils.determineRoundPoints(preferences,assignments);

        // Determine preference score
        double preferenceScore = PreferenceFeatureUtils.determinePreferenceScore(camper,roundPoints,assignments);

        // Add results to the roster
        setValue_unrequestedActivities(camper,unrequestedActivities);
        setValue_roundScores(camper,roundPoints);
        setValue_mainScore(camper,preferenceScore);

        //Map camper + preference score, enabling percentile checks later
        camperScores.put(camper.getId(), preferenceScore);
        // return preferenceScore;
    }





    /**
     * Adds the preference score to the roster
     *
     * @param camper The camper to update
     * @param preferenceScore The preference score to add
     */
    private void setValue_mainScore(Camper camper,double preferenceScore){
        String value = String.format("%.0f", preferenceScore*100);
        // String value = Double.toString(preferenceScore*100);
        // String value = Integer.toString((int)(preferenceScore*100));
        camper.setValue(RosterHeader.PREFERENCE_SCORE.standardName, value);
    }

    /**
     * Calculates percentiles for all campers based on their preference scores
     * and sets the percentile values in the roster.
     *
     * @param roster The roster to update
     * @param camperScores Map of camper IDs to their preference scores
     */
    private void calculateAndSetPercentiles(EnhancedRoster roster, Map<String, Double> camperScores) {
        // Extract all scores and sort them
        List<Double> allScores = new ArrayList<>(camperScores.values());
        Collections.sort(allScores);

        // Calculate percentile for each camper
        for (Camper camper : roster.getCampers()) {
            Double score = camperScores.get(camper.getId());
            if (score != null) {
                // Skip campers with missing preference data (they already have percentile set to 0)
                if (DataConstants.isEmpty(camper.getValue(RosterHeader.PREFERENCES.standardName))) {
                    continue;
                }

                // Calculate percentile (percentage of scores that are less than or equal to this score)
                int rank = 0;
                for (Double otherScore : allScores) {
                    if (otherScore <= score) {
                        rank++;
                    }
                }

                double percentile = (double) rank / allScores.size() * 100;
                String percentileValue = String.format("%.0f", percentile);
                camper.setValue(RosterHeader.PREFERENCE_PERCENTILE.standardName, percentileValue);
            }
        }
    }

    /**
     * Adds the unrequested activities set to the roster.
     * For empty sets, returns the DISPLAY_EMPTY constant
     *
     * @param camper The camper to update
     * @param unrequestedActivities The set of unrequested activities to format and add
     */
    private void setValue_unrequestedActivities(Camper camper,List<String> unrequestedActivities){
        String value = unrequestedActivities.isEmpty() ? DataConstants.DISPLAY_EMPTY : String.join(", ",unrequestedActivities);
        camper.setValue(RosterHeader.UNREQUESTED_ACTIVITIES.standardName,value);
    }

    /**
     * Adds round-by-round scores to the roster
     *
     * @param camper The camper to update
     * @param roundPoints The array of round-by-round scores to add
     */
    private void setValue_roundScores(Camper camper,int[] roundPoints){
        StringBuilder valueBuilder = new StringBuilder();
        for (int i = 0; i < roundPoints.length; i++) {
            valueBuilder.append(roundPoints[i]);
            if (i < roundPoints.length - 1) {
                valueBuilder.append(", ");
            }
        }
        camper.setValue(RosterHeader.SCORE_BY_ROUND.standardName,valueBuilder.toString());
    }


}
