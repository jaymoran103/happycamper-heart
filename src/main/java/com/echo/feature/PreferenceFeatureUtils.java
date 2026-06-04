package com.echo.feature;

import java.util.ArrayList;
import java.util.List;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.RosterHeader;

public class PreferenceFeatureUtils {

    public static double determinePreferenceScore(Camper camper,int[] roundPoints,String[] assignments) {
        // Sum points for each round - used Arrays.stream(roundPoints, 0, roundCount).sum(); but that seemed less efficient for a 3 index array
        int totalPoints = 0;
        for (int i = 0; i < roundPoints.length; i++) {
            totalPoints += roundPoints[i];
        }

        // Calculate preference score as totalPoints' percentage of maxPoints
        int maxPoints = determineMaxPoints(camper,assignments);
        double preferenceScore = maxPoints > 0 ? (double)totalPoints / maxPoints
                                               : 1.0;

        return preferenceScore;
    }

    /**
     * Determines the activities assigned to a camper that were not requested in their preferences
     *
     * Treats "Exempt" activities as requested, as they are separate from the preference system and would make for misleading data
     * Ignores empty assignments - they don't actively go against a preference
     * @param camper Camper to check data for
     * @param preferences List of camper preferences, in order
     * @param assignments Array of assignments for each round
     * @return List of activities that were not requested
     */
    public static List<String> determineUnrequestedActivities(Camper camper,List<String> preferences,String[] assignments){
        List<String> unrequestedActivities = new ArrayList<>();
        for (String assignment : assignments) {
            if ( DataConstants.isEmpty(assignment)
                    || preferences.contains(assignment)
                    || PreferenceFeature.getExemptActivities().contains(assignment) ){
                continue;
            }
            unrequestedActivities.add(assignment);
        }
        return unrequestedActivities;
    }

    public static int[] determineRoundPoints(List<String> preferences,String[] assignments){
        int[] roundPoints = new int[assignments.length];
        for (int i = 0; i < assignments.length; i++) {
            roundPoints[i] = isExemptActivity(assignments[i]) ? 0 : scoreActivity(preferences, assignments[i]);
        }
        return roundPoints;
    }

    public static int determineMaxPoints(Camper camper,String[] assignments){
        int roundCount = Integer.parseInt(camper.getValue(RosterHeader.ROUND_COUNT.standardName));

        //Determine # of elligible (non-exempt) activities
        int nonExemptCount = 0;
        for (int i = 0; i < roundCount; i++) {
            if (!isExemptActivity(assignments[i])) {
                nonExemptCount++;
            }
        }

        return switch (nonExemptCount) {
            case 0 -> 0;
            case 1 -> 10; // 1st choice = 10 points
            case 2 -> 19; // 1st + 2nd choice = 10 + 9 = 19 points
            case 3 -> 27; // 1st + 2nd + 3rd choice = 10 + 9 + 8 = 27 points
            // case 4 -> 34; // 1st + 2nd + 3rd + 4th choice = 10 + 9 + 8 + 7 = 34 points
            // case 5 -> 40; // 1st + 2nd + 3rd + 4th + 5th choice = 10 + 9 + 8 + 7 + 6 = 40 points
            // case 6 -> 45; // 1st + 2nd + 3rd + 4th + 5th + 6th choice = 10 + 9 + 8 + 7 + 6 + 5 = 45 points
            // case 7 -> 49; // 1st + 2nd + 3rd + 4th + 5th + 6th + 7th choice = 10 + 9 + 8 + 7 + 6 + 5 + 4 = 49 points
            // case 8 -> 52; // 1st + 2nd + 3rd + 4th + 5th + 6th + 7th + 8th choice = 10 + 9 + 8 + 7 + 6 + 5 + 4 + 3 = 52 points
            // case 9 -> 54; // 1st + 2nd + 3rd + 4th + 5th + 6th + 7th + 8th + 9th choice = 10 + 9 + 8 + 7 + 6 + 5 + 4 + 3 + 2 = 54 points
            // case 10 -> 55; // 1st + 2nd + 3rd + 4th + 5th + 6th + 7th + 8th + 9th + 10th choice = 10 + 9 + 8 + 7 + 6 + 5 + 4 + 3 + 2 + 1 = 55 points
            default -> {
                // Simplified: 11n - n(n+1)/2
                yield 11 * roundCount - roundCount * (roundCount + 1) / 2;
            }
        };
    }

    /**
     * Parses a preference field string into a list of individual preferences.
     * Handles the special case where the last two items are separated by "and" instead of a comma.
     *
     * @param preferences The raw preference string from the camper's data
     * @return A list of individual preference strings, each trimmed of whitespace
     */
    static List<String> parsePreferenceField(String preferences) {

        // Split the preferences string into individual preferences and trim whitespace
        String[] preferencesArray = preferences.split(",");
        List<String> preferencesList = new ArrayList<>();
        for (String pref : preferencesArray) {
            preferencesList.add(pref.trim());
        }

        // Account for preference syntax where the last item is separated by " and " instead of a comma
        String lastItem = preferencesList.get(preferencesList.size()-1);
        if (lastItem.contains(PreferenceFeature.PENULTIMATE_TOKEN)) {
            String[] splitItem = lastItem.split(PreferenceFeature.PENULTIMATE_TOKEN);
            if (splitItem.length > 2) {
                // This is a case where there are multiple "and" tokens in the last item
                // For now, we just log it and proceed with the simple split
                System.out.println("Potential issue: multiple 'and' tokens in last item");
            }
            preferencesList.removeLast();
            // Add the split items, making sure to trim each one
            for (String item : splitItem) {
                preferencesList.add(item.trim());
            }
        }

        return preferencesList;
    }

    /**
     * Scores an activity based on its position in the camper's preference list.
     *
     * @param preferences The camper's list of preferences
     * @param activity The activity to score
     * @return Points for this activity (10 for 1st choice, 9 for 2nd, etc., 0 if not in preferences)
     */
    static int scoreActivity(List<String> preferences, String activity) {
        if (preferences.contains(activity)) {
            return PreferenceFeature.PREFERENCE_COUNT - preferences.indexOf(activity);
        } else {
            return 0; // Activity not in preferences
        }
    }

    /**
     * Advanced scoring method that takes into account the number of preferences provided.
     * This approach gives higher scores to campers with few input assignments, so minimal lists don't drastically impact scores
     *
     * @param preferences The camper's list of preferences
     * @param activity The activity to score
     * @return Points for this activity with adjustments for incomplete preference lists
     */
    static int scoreActivityAdvanced(List<String> preferences, String activity) {
        if (preferences.contains(activity)) {
            return PreferenceFeature.PREFERENCE_COUNT - preferences.indexOf(activity);
        } else {
            // If preference count was met, indicate very low satisfaction with 0
            if (preferences.size() >= PreferenceFeature.PREFERENCE_COUNT) {
                return 0;
            }
            // If preference count was not met, indicate low satisfaction balanced with difficulty of few preference options
            else {
                return (PreferenceFeature.PREFERENCE_COUNT - preferences.size()) / 2;
            }
        }
    }

    /**
     * Checks if an activity is in the exempt list
     * @param activity The activity to check
     * @return true if the activity is exempt, false otherwise
     */
    static boolean isExemptActivity(String activity) {
        return PreferenceFeature.getExemptActivities().contains(activity) || DataConstants.isEmpty(activity);
    }
    
}
