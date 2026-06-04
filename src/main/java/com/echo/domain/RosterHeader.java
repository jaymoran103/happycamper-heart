package com.echo.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;



public enum RosterHeader {

    //Standard Camper Columns - used for display. Most are found on both input roster types
    FIRST_NAME("First Name",false,"First Name","First Name"),
    PREFERRED_NAME("Preferred Name",true,"Preferred Name","Preferred Name"),
    LAST_NAME("Last Name",true,"Last Name","Last Name"),
    GRADE("Grade",false,"Camp Grade","Grade"),
    ESP("Enrolled Sessions/Programs",false,"Enrolled Sessions/Programs",null),
    PROGRAM("Program",true), //Parsed from ESP field by ProgramFeature

    // Standard Activity Columns - Input-only columns from ActivityRoster
    ACTIVITY(ActivityRoster.class,"Activity"),
    ROUND(ActivityRoster.class,"Period"),

    // Enhanced Activity Columns - obtained by ActivityFeature from an ActivityRoster
    CABIN("Cabin",true,null,"Cabin"),
    ROUND_1("Round 1",true),//FUTURE - replace these with a more dynamic approach
    ROUND_2("Round 2",true),
    ROUND_3("Round 3",true),
    ROUND_COUNT("Rounds Assigned",false),

    // Preference Feature Columns
    PREFERENCES("Activity Preferences",false,"Activity Preferences",null),
    PREFERENCE_SCORE("Preference Score",true),
    PREFERENCE_PERCENTILE("Preference Percentile",false),
    UNREQUESTED_ACTIVITIES("Unrequested Activities",true),
    SCORE_BY_ROUND("Preference by Round",false),

    // Medical Feature Column
    MEDICAL_NOTES("Medical Notes",true,"Medical Notes","Medical Notes"),
    // CAMPER_ID(),

    // Swim Level Feature Columns
    SWIMCOLOR("SwimColor",true,"SwimColor",null),
    SWIMCONFLICTS("Swim Conflicts",true),
    ;


    public final String standardName;
    public final boolean defaultVisibility; //FUTURE - set private to force use of getter, enabling context-dependent visibility
    public final String camperRosterName;
    public final String activityRosterName;
    private boolean inputOnly = false;

    private static final String ROUND_BASE = "Round "; //Field is used to build round headers

    /**
     * Constructor for input-only headers.
     *
     * @param rosterClass The class type that this header is associated with
     * @param headerName The name of the header
     */
    RosterHeader(Class<?> rosterClass, String headerName) {
        this(headerName,
            true,
            rosterClass == CamperRoster.class ? headerName : null,
            rosterClass == ActivityRoster.class ? headerName : null
        );
        inputOnly = true;
    }
    /**
     * Constructor for display-only headers.
     * Visibility boolean is completely valid here, many are useful for sorting and filtering but would clutter the normal view
     *
     * @param headerName The name of the header
     * @param defaultVisibility The default visibility setting for the header.
     *      ^False for headers intended for sorting/filtering utility or added context, rather than primary display
     */
    RosterHeader(String standardName,boolean defaultVisibility){
        this(standardName,defaultVisibility,null,null);
    }

    /**
     * Constructor for headers with custom names for different roster types.
     *
     * @param standardName The standard name for the header
     * @param defaultVisibility The default visibility setting for the header
     * @param camperRosterName The name of the header in the CamperRoster
     * @param activityRosterName The name of the header in the ActivityRoster
     */
    RosterHeader(String standardName,Boolean defaultVisibility,String camperRosterName,String activityRosterName){
        this.standardName = standardName;
        this.defaultVisibility = defaultVisibility;
        this.camperRosterName = camperRosterName;
        this.activityRosterName = activityRosterName;
    }

    public boolean isInputOnly(){
        return inputOnly;
    }


    public boolean doNumericSort(){
        return switch (this){
            case GRADE, PREFERENCE_PERCENTILE, PREFERENCE_SCORE -> true;
            default -> false;
        };
    }

    public static RosterHeader determineHeaderType(String headerText){
        for (RosterHeader header : RosterHeader.values()){
            if (header.standardName != null && header.standardName.equals(headerText)){
                return header;
            }
        }
        for (RosterHeader header : RosterHeader.values()){
            if (header.camperRosterName != null && header.camperRosterName.equals(headerText)){
                return header;
            }
        }
        for (RosterHeader header : RosterHeader.values()){
            if (header.activityRosterName != null && header.activityRosterName.equals(headerText)){
                return header;
            }
        }
        return null;
    }

    public static RosterHeader determineHeaderType(String headerText, Class<?> rosterClass){
        if (rosterClass == EnhancedRoster.class){
            for (RosterHeader header : RosterHeader.values()){
                if (header.standardName.equals(headerText)){
                    return header;
                }
            }
        }
        else if (rosterClass == CamperRoster.class){
            for (RosterHeader header : RosterHeader.values()){
                if (header.camperRosterName != null && header.camperRosterName.equals(headerText)){
                    return header;
                }
            }
        } else if (rosterClass == ActivityRoster.class){
            for (RosterHeader header : RosterHeader.values()){
                if (header.activityRosterName != null && header.activityRosterName.equals(headerText)){
                    return header;
                }
            }
        }

        return null;
    }

    // public int getInherentPosition(){
    //     int index = values().indexOf(this);
    //     return index;
    // }

    public static List<RosterHeader> getSortedHeaders(List<RosterHeader> headers){
        // return headers.stream()
        //         .sorted(Comparator.comparingInt(RosterHeader::getInherentPosition))
        //         .collect(Collectors.toList());
        List<RosterHeader> sortedHeaders = new ArrayList<>();
        for (RosterHeader header:values()){
            if (headers.contains(header)){
                sortedHeaders.add(header);
            }
        }
        return sortedHeaders;
    }

    /**
     * Builds a round header string from an activity row.
     * Extracts the round number from the activity data and creates the corresponding header.
     *
     * @param activityRow Map containing activity data with the round field
     * @return The corresponding round header string (e.g., "Round 1")
     */
    public static String buildRoundString(Map<String, String> activityRow) {
        String round = activityRow.get(ROUND.activityRosterName);
        if (round == null) {
            // Fallback to looking for "Period" directly if the activityRosterName is null
            round = activityRow.get(ROUND.activityRosterName);
        }
        return ROUND_BASE + round;
    }

    /**
     * Builds a round header string from a round number.
     *
     * @param round The round number
     * @return The corresponding round header string ("Round 1","Round 2"...)
     */
    public static String buildRoundString(int round) {
        return ROUND_BASE + round;
    }

    /**
     * Updates a header map to reflect the default display ordering.
     * This method modifies the integer values in the map to match the sorted order.
     * After this method is called, the integer values in the map will be sequential
     * starting from 0, with headers ordered according to their position values.
     *
     * @param headerMap A map of header names to position integers (typically from Roster.getHeaderMap())
     */
    public static void updateHeaderMapOrder(Map<String, Integer> headerMap) {
        // Convert header names to RosterHeader enums where possible
        List<String> headerNames = new ArrayList<>(headerMap.keySet());
        List<String> sortedHeaders = new ArrayList<>();

        // First add headers that match RosterHeader enums in their defined order
        for (RosterHeader header : RosterHeader.values()) {
            if (headerNames.contains(header.standardName)) {
                sortedHeaders.add(header.standardName);
            }
        }

        // Then add any remaining headers that weren't matched
        for (String header : headerNames) {
            if (!sortedHeaders.contains(header)) {
                sortedHeaders.add(header);
            }
        }

        // Clear and reassign indices based on sorted order
        headerMap.clear();
        for (int i = 0; i < sortedHeaders.size(); i++) {
            headerMap.put(sortedHeaders.get(i), i);
        }
    }

    /**
     * Checks if a header name represents a round header.
     * FUTURE - imperfect solution, find a better approach
     * @param headerName The header name to check
     * @return true if the header represents a round
     */
    public static boolean isRound(String headerName) {
        return headerName.startsWith(ROUND_BASE);
    }
}
