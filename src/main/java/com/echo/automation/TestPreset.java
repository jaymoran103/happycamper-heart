package com.echo.automation;

import java.io.File;

public enum TestPreset {

    DEMO_S6(1001,
        "Authentic summer data with anonymized names and preferences.",
        TestFiles.DEMO_S6_CAMPERS_PREFERENCES,
        TestFiles.DEMO_S6_ACTIVITIES,
        new String[]{"preference","program"},
        6
    ),

    DEMO_S5(1005,
        "Authentic summer data with anonymized names.",
        TestFiles.DEMO_S5_CAMPERS,
        TestFiles.DEMO_S5_ACTIVITIES,
        new String[]{"activity", "program"},
        6
    ),

    MINI_NORMAL(1, TestFiles.MINI_CAMPERS, TestFiles.MINI_ACTIVITIES, 1,
            "Normal pair of mini rosters. 3 campers, 3 total assignments"),
    MINI_MALFORMED_EXTRA(2, TestFiles.MINI_CAMPERS_EXTRA_CELL, TestFiles.MINI_ACTIVITIES, 1,
            "Checks malformed data warning. Camper file has an extra cell"),
    MINI_MALFORMED_MISSING(3, TestFiles.MINI_CAMPERS_MISSING_CELL, TestFiles.MINI_ACTIVITIES, 1,
            "Checks malformed data warning. Camper file lacks a cell"),
    MINI_WRONG_FORMAT(4, TestFiles.MINI_CAMPERS_TEXT_FILE, TestFiles.MINI_ACTIVITIES, 1,
            "Checks invalid extension warning. Camper file has a .txt extension"),
    MINI_NO_ROWS(5, TestFiles.MINI_CAMPERS_NO_ROWS, TestFiles.MINI_ACTIVITIES, 1,
            "Checks missing data warning. Camper file has no rows"),
    MINI_EMPTY(6, TestFiles.MINI_EMPTY_FILE, TestFiles.MINI_ACTIVITIES, 1,
            "Checks missing data warning. Camper file has no data"),
    MINI_WRONG_SESSION(7, TestFiles.MINI_CAMPERS, TestFiles.MINI_ACTIVITIES, 2,
            "Checks program parsing warning. Campers lack programs for session 2"),
    MINI_UNMATCHED_CAMPERS(8, TestFiles.BASIC_CAMPERS, TestFiles.MINI_ACTIVITIES, 1,
            "Tests case with campers missing their respective activities. No warning but missing empty assignments"),
    MINI_UNMATCHED_ACTIVITIES(9, TestFiles.MINI_CAMPERS, TestFiles.BASIC_ACTIVITIES, 1,
            "Tests case with activity assignment missing their respective camper"),
    MINI_DUPLICATE_ACTIVITIES(10, TestFiles.MINI_CAMPERS, TestFiles.MINI_ACTIVITIES_DUPLICATES, 1,
            "Tests case with duplicate activities"),
    MINI_MISSING_HEADER(11, TestFiles.MINI_CAMPERS_MISSING_HEADER, TestFiles.MINI_ACTIVITIES, 1,
            "Tests case with missing required header"),
    MINI_BAD_HEADER_CHARACTERS(12, TestFiles.MERGED_SINGLE_ALLCOLUMNS_CASCADENULLS, TestFiles.MINI_ACTIVITIES, 1,
            "Tests case with bad header characters"),

    NEW_BASIC_TEST(101, TestFiles.BASIC_CAMPERS, TestFiles.BASIC_ACTIVITIES, 1),
    NEW_S5_TEST(105,
            "Authentic summer data with anonymized names. Many missing assignments.",
            TestFiles.DEMO_S5_CAMPERS,
            TestFiles.DEMO_S5_ACTIVITIES,
            new String[]{"activity", "program"},
            5),


    NEW_S6_TEST(106, TestFiles.DEMO_S6_CAMPERS, TestFiles.DEMO_S6_ACTIVITIES, 6,
            "Authentic summer data with anonymized names. No missing assignments."),
    BASIC_PREFERENCES(107,TestFiles.BASIC_CAMPERS_PREFERENCES,TestFiles.BASIC_ACTIVITIES,6,
            "Basic cabin roster, including preferences"),
    BASIC_PREFERENCES_MEDICAL(108,TestFiles.BASIC_CAMPERS_PREFERENCES_MEDICAL,TestFiles.BASIC_ACTIVITIES,6,
            "Basic cabin roster, including preferences and medical"),
    BASIC_SWIMCOLORS(109,
        "Basic cabin roster, including swim colors",
        TestFiles.BASIC_CAMPERS_SWIMCOLORS,
        TestFiles.BASIC_ACTIVITIES,
        new String[]{"swimlevel"},
        6),
        
    BASIC_TRIPLE_FEATURE(110,
        "Basic cabin roster, including preferences, medical, and swim colors",
        TestFiles.BASIC_CAMPERS_TRIPLE_FEATURE,
        TestFiles.BASIC_ACTIVITIES,
        new String[]{"preference","program","swimlevel","medical"},
        6),
    // BASIC_SWIMCOLORS(109,TestFiles.BASIC_CAMPERS_SWIMCOLORS,TestFiles.BASIC_ACTIVITIES,6,
    //         "Basic cabin roster, including swim colors"),


    // Validation test presets
    VALIDATION_NORMAL(201, TestFiles.MINI_CAMPERS, TestFiles.MINI_ACTIVITIES, 1,
            "Valid pair of mini rosters for validation testing"),
    VALIDATION_MALFORMED_EXTRA(202, TestFiles.MINI_CAMPERS_EXTRA_CELL, TestFiles.MINI_ACTIVITIES, 5,
            "Malformed data for validation testing - has extra cell"),
    VALIDATION_MALFORMED_MISSING(203, TestFiles.MINI_CAMPERS_MISSING_CELL, TestFiles.MINI_ACTIVITIES, 5,
            "Malformed data for validation testing - lacks a cell"),
    VALIDATION_BAD_NAMES(204, TestFiles.BASIC_CAMPERS_BADNAMES, TestFiles.BASIC_ACTIVITIES, 1,
            "Bad name formats for validation testing"),
    PARSING_BADHEADERCHARACTERS(301,TestFiles.BADHEADER_CAMPERS,TestFiles.BASIC_ACTIVITIES,5,
            "Like real CM rosters, has characters in the header that break the parser.");

    

    private final int id;
    private final TestFiles camperFile;
    private final TestFiles activityFile;
    private final int session;
    private final String description;
    private final String[] features;


    TestPreset(int id, String description, TestFiles camperFile, TestFiles activityFile, String[] features, int session) {
        this.id = id;
        this.camperFile = camperFile;
        this.activityFile = activityFile;
        this.session = session;
        this.description = description;
        this.features = features==null ? new String[]{} : features;
    }
    TestPreset(int id, TestFiles camperFile, TestFiles activityFile, int session, String description) {
        this(id, description, camperFile, activityFile, null, session);
    }
    TestPreset(int id, TestFiles camperFile, TestFiles activityFile, int session) {
        this(id, camperFile, activityFile, session, null);
    }


    /**
     * Gets a File object for the camper file
     * @return File object for the camper file
     */
    public File getCamperFile() {
        // First try using the TestFileFinder
        File file = TestFileFinder.findFile(camperFile.filename);
        if (file != null) {
            return file;
        }

        // Fall back to the old method if TestFileFinder fails
        return camperFile.toFile();
    }

    /**
     * Gets a File object for the activity file
     * @return File object for the activity file
     */
    public File getActivityFile() {
        // First try using the TestFileFinder
        File file = TestFileFinder.findFile(activityFile.filename);
        if (file != null) {
            return file;
        }

        // Fall back to the old method if TestFileFinder fails
        return activityFile.toFile();
    }

    /**
     * Gets the path to the camper file as a string
     * @return String path to the camper file
     * @deprecated Use getCamperFile() instead to get a File object
     */
    @Deprecated
    public String getCamperFilePath() {
        return getCamperFile().getAbsolutePath();
    }

    /**
     * Gets the path to the activity file as a string
     * @return String path to the activity file
     * @deprecated Use getActivityFile() instead to get a File object
     */
    @Deprecated
    public String getActivityFilePath() {
        return getActivityFile().getAbsolutePath();
    }

    public int getSession() {
        return session;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        // return description==null ? "No Description" : description;
        return description;
    }

    public String[] getFeatures() {
        return features;
    }

    public static TestPreset fromId(int id) {
        for (TestPreset preset : values()) {
            if (preset.id == id) {
                return preset;
            }
        }
        throw new IllegalArgumentException("No test preset found for id: " + id);
    }


    public void printReport() {
        System.out.println("\n===\n");
        System.out.println("Running test mode: " + this.id + (getDescription() != null ? " - " + this.getDescription() : ""));
        System.out.println("Name: " + this.name());
        System.out.println("Camper file: " + this.getCamperFile().getName());
        System.out.println("Activity file: " + this.getActivityFile().getName());
        if (this.getFeatures() != null && this.getFeatures().length > 0) {
            System.out.println("Features: " + String.join(", ", this.getFeatures()));
        }
        System.out.println("Session: " + this.getSession());
        System.out.println("\n===\n");
    }
}