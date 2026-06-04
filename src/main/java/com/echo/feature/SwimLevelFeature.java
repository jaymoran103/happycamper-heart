package com.echo.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.logging.RosterWarning;
import com.echo.logging.WarningManager;
import com.echo.validation.RosterRegexBuilder;


/**
 * Feature that checks campers' swim level against assigned activities to ensure safe schedules.
 *
 * Main functions:
 * - Checks each camper's swim level against their assigned activities, listing any incompatible activities
 * - Produces column indicating each camper's compatiblity. Currently disabled due to overlap with prior column
 */
public class SwimLevelFeature implements RosterFeature {

    private static final String FEATURE_ID = "swimlevel";
    public static final String FEATURE_NAME = "Swim Level Validation";

    private final List<String> requiredHeaders = Arrays.asList(RosterHeader.SWIMCOLOR.camperRosterName);

    private final List<String> addedHeaders = Arrays.asList(RosterHeader.SWIMCONFLICTS.standardName);
                                                             

    // Maps of required level for each activity, and the names that map to each level
    // FUTURE - add config file for preference exemptions?
    private Map<String, Integer> activityRequirements;
    private Map<String, Integer> levelNameMappings;

    private final int ROUNDS_OFFERED = ActivityFeature.MAX_ROUNDS; // Maximum number of activity rounds supported

    // Configuration options for handling unknown activities.
    public static final boolean REQUIRE_ALL_DEFINITIONS = true;
    public static final boolean FLAG_UNKNOWN_ACTIVITIES = false;

    private Set<String> unknownActivities = new HashSet<>();


    /**
     * Constructor reates a new SwimLevelFeature and sets up the activity requirements.
     */
    public SwimLevelFeature() {
        //FUTURE - add config file for activity requirements?
        activityRequirements = getDefaultActivityRequirements();
        levelNameMappings = getDefaultLevelNameMappings();

    }

    /**
     * Gets the default activity requirements for the SwimLevelFeature.
     *
     * @return A map of activity names to their required swim level
     */
    public Map<String, Integer> getDefaultActivityRequirements() {
        
        //Map activities to integers representing minimum swim level
        Map<String, Integer> defaultActivityRequirements = new HashMap<>();
        defaultActivityRequirements.put("Sailing", 2);
        defaultActivityRequirements.put("Paddlesports",1);
        defaultActivityRequirements.put("Paddle Sports",1);

        defaultActivityRequirements.put("Skiing",2);
        defaultActivityRequirements.put("Gold Swimming", 2);

        //Non-swimming activities shouldnt need mapping like this, but it makes the requirement warnings more helpful with a shorter list of unknowns
        defaultActivityRequirements.put("Archery",0);
        defaultActivityRequirements.put("Arts & Crafts",0);
        defaultActivityRequirements.put("Biking", 0);
        defaultActivityRequirements.put("Challenge",0);
        defaultActivityRequirements.put("Dance",0);
        defaultActivityRequirements.put("Drama",0);
        defaultActivityRequirements.put("Horseback Riding",0);
        defaultActivityRequirements.put("Fishing",0);
        defaultActivityRequirements.put("Friendship Bracelet",0);
        defaultActivityRequirements.put("Nature",0);
        defaultActivityRequirements.put("Sports",0);
        return defaultActivityRequirements;
    }

    /**
     * Gets the default level name mappings for the SwimLevelFeature.
     *
     * @return A map of swim level names to their integer values
     */
    public Map<String, Integer> getDefaultLevelNameMappings() {
        
        //Map Swim level names to integer values for inclusive comparisons
        Map<String, Integer> defaultLevelNameMappings = new HashMap<>();
        defaultLevelNameMappings.put("Blue",2);
        defaultLevelNameMappings.put("White",1);
        defaultLevelNameMappings.put("Red",0);
        return defaultLevelNameMappings;
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

        for (String header : getAddedHeaders()) {
            roster.addHeader(header);
        }

        for (Camper camper : roster.getCampers()) {
            String swimLevel = camper.getValue(RosterHeader.SWIMCOLOR.camperRosterName);

            if (!DataConstants.isEmpty(swimLevel)){
                applyToCamper(camper,warningManager);
            } else {
                RosterWarning warning = RosterWarning.create_camperMissingField(camper.getData(),RosterHeader.SWIMCOLOR.standardName,FEATURE_NAME);
                warningManager.logWarning(warning);
            }
        }

        //If definitions for all activities are required in the configuration, each unknown activity found generates a warning.
        if (REQUIRE_ALL_DEFINITIONS){
            //Sort cached activities
            List<String> sortedActivities = new ArrayList<>(unknownActivities);
            Collections.sort(sortedActivities);

            //Create a warning for each
            for (String header : sortedActivities){
                RosterWarning warning = RosterWarning.create_unknownSwimActivityWarning(header,FLAG_UNKNOWN_ACTIVITIES);
                warningManager.logWarning(warning);
            }
        }

        roster.enableFeature(FEATURE_ID);
    }

    @Override
    public boolean preValidate(EnhancedRoster roster, WarningManager warningManager) {

        // Ensure all required headers are present
        boolean lacksHeader = false;
        for (String header : requiredHeaders) {
            if (!roster.hasHeader(header)) {
                RosterWarning warning = RosterWarning.create_missingFeatureHeader(header, FEATURE_NAME);
                warningManager.logWarning(warning);
                lacksHeader = true; // Mark that we're missing a required header
            }
        }

        return !lacksHeader; // Returns false if any required headers are missing
    }

    @Override
    public boolean postValidate(EnhancedRoster roster, WarningManager warningManager) {
        return true;
    }



    /**
     * Main logic for applying the feature to a single camper
     * 
     * Sets 'Swim Validity' field to 'Yes' if camper has compatible activities, 'No' if a discrepancy is caught
     * Sets 'Swim Conflicts' field to display any incompatible activities
     *
     * @param camper The camper to apply the feature to
     * @return The calculated preference score (as a decimal between 0 and 1)
     */
    private void applyToCamper(Camper camper,WarningManager warningManager) {

        // Determine camper's swim level and assignments
        String swimLevelName = camper.getValue(RosterHeader.SWIMCOLOR.camperRosterName);
        String[] assignments = new String[ROUNDS_OFFERED];
        for (int i = 0; i < ROUNDS_OFFERED; i++) {
            assignments[i] = ActivityFeature.getActivityForCamper(camper, i+1);
        }

        // Determine camper's incompatible activities
        List<String> incompatibleActivities = determineIncompatibleActivities(swimLevelName,assignments,camper,warningManager);
        boolean allActivitiesApproved = incompatibleActivities==null || incompatibleActivities.isEmpty();

        // // Report presence of incompatible activities as field, add to camper. Currently disabled due to overlap with prior column
        // String swimValidity = allActivitiesApproved 
        //     ? "Yes" 
        //     : "No";
        // camper.setValue(RosterHeader.SWIMVALIDITY.standardName, swimValidity);

        // Report specific incompatible activities as field, add to camper.
        String activityConflicts = allActivitiesApproved 
            ? DataConstants.DISPLAY_EMPTY
            : String.join(", ",incompatibleActivities);
        camper.setValue(RosterHeader.SWIMCONFLICTS.standardName, activityConflicts);

    }

    /**
     * Helper method to determine if an activity is appropriate for a given swim level
     *
     * @param activity The activity to check
     * @param swimLevel The swim level to check against
     * @return True if the activity is appropriate for the swim level, false otherwise
     */
    private boolean approveActivity(String activity,int swimLevel){
        //If activity is empty, return true - no assignment to contradict swim level
        if (DataConstants.isEmpty(activity)){
            return true;
        }
        //If activity and requirement is known, check against swim level
        else if (activityRequirements.containsKey(activity)){
            return activityRequirements.get(activity) <= swimLevel;
        } 
        //If activity is unknown, note it, 
        else if (REQUIRE_ALL_DEFINITIONS){
            unknownActivities.add(activity);
        }
        ///then reject it if definitions are mandatory and unknown activities are flagged
        boolean shouldRejectActivity = REQUIRE_ALL_DEFINITIONS && FLAG_UNKNOWN_ACTIVITIES; //If all activities must be defined, and unknown activities are invalid, reject the activity
        return !shouldRejectActivity; //Rejected activity returns false;
    }

    /**
     * Helper method to determine which (if any) activities assigned to a camper are incompatible with their swim level
     *
     * @param camper The camper to check
     * @param swimLevelName The name of the swim level to check against
     * @param assignments The array of assignments to check
     * @return List of incompatible activities
     */
    private List<String> determineIncompatibleActivities(String swimLevelName,String[] assignments,Camper camper,WarningManager warningManager){

        Integer swimLevelInt = levelNameMappings.get(swimLevelName);
        if (swimLevelInt==null){
            RosterWarning warning = RosterWarning.create_unknownSwimLevelWarning(camper.getData(),swimLevelName,getDefaultLevelNameMappings().keySet());
            warningManager.logWarning(warning);
            return null;
        }
        //TODO validate swimLevelName. Likely caused by case where swim level is written incorrectly in config/given map

        List<String> incompatibleActivities = new ArrayList<>();
        for (String activity : assignments) {
            //If the activity is empty, skip it
            if (DataConstants.isEmpty(activity)){
                continue;
            }
            // If the activity isn't approved
            if (!approveActivity(activity,swimLevelInt)){
                incompatibleActivities.add(activity);
            }
        }
        return incompatibleActivities;
    }


}
