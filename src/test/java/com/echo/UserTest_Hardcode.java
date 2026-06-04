package com.echo;

import com.echo.automation.TestFileFinder;
import com.echo.automation.TestPreset;


public class UserTest_Hardcode {
    public static final int TESTMODE = 110;

    public static void main(String[] args) {
        // Clear the file cache to ensure fresh file lookups
        TestFileFinder.clearCache();

        try {
            if (args.length > 0) {
                printPresetInfo(args);
                HappyCamper.mainTest(args);
            } else {
                System.out.println("Running with hardcoded argument id -> "+TESTMODE);
                HappyCamper.mainTest(new String[]{""+TESTMODE});
            }
        } catch (Exception e) {
            System.err.println("Error running test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void printPresetInfo(String[] args){
        int testMode = Integer.parseInt(args[0]);
        TestPreset preset = TestPreset.fromId(testMode);

        System.out.println("\n===\n");
        System.out.println("Running test mode: "+testMode+" - "+preset.getDescription());
        System.out.println("Name: "+preset.name());
        System.out.println("Camper file: "+preset.getCamperFile().getName());
        System.out.println("Activity file: "+preset.getActivityFile().getName());
        System.out.println("Features: "+String.join(", ", preset.getFeatures()));
        System.out.println("Session: "+preset.getSession());
        System.out.println("\n===\n");
    }

}
