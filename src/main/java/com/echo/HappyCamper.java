package com.echo;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.echo.automation.TestPreset;
import com.echo.service.ExportService;
import com.echo.service.ImportService;
import com.echo.service.RosterService;
import com.echo.ui.MainWindow;

/**
 * Main entry point for the Happy Camper application.
 */
public class HappyCamper {
    public static final String NAME = "HappyCamper";
    public static final String VERSION = "2.2.0";
    public static String NAME_VERSION = NAME+" "+VERSION;

    public static boolean WAIT_TO_AUTOMATE = false;
    public static boolean PRINT_LOGGED_ERRORS = true;

    private static MainWindow mainWindowInstance;


    public static MainWindow setupApp(boolean andStart){
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Error setting look and feel: " + e.getMessage());
        }

        // Create services
        ImportService importService = new ImportService();
        ExportService exportService = new ExportService();
        RosterService rosterService = new RosterService(importService, exportService);

        // Create UI immediately instead of using invokeLater
        createSingleWindow(rosterService);

        if (andStart) {
            // Only make it visible on EDT
            SwingUtilities.invokeLater(() -> {
                mainWindowInstance.setVisible(true);
            });
        }

        return mainWindowInstance;
    }

    private static MainWindow createSingleWindow(RosterService rosterService){
        if (mainWindowInstance==null){
            mainWindowInstance = new MainWindow(rosterService);
            mainWindowInstance.setTitle(NAME+" "+VERSION);
            mainWindowInstance.setDefaultCloseOperation(MainWindow.EXIT_ON_CLOSE);
        }
        return mainWindowInstance;
    }
    public static MainWindow accessSingleWindow(){
        if (mainWindowInstance==null){
            throw new Error("Main window has not been created yet");
        }
        return mainWindowInstance;
    }

    

    /**
     * Main method.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        if (args.length==0){
            setupApp(true);
        }
        else if (args.length==1 && args[0].matches("^-?\\d+$")){
            mainTest(args);
        }
        else {
            System.err.println("Invalid arguments");
            System.out.println("Expects a single integer indicating test preset");
        }
    }

    public static void mainTest(String[] args) {
        if (args.length==0){
            main(args);
            return;
        }
        // if (args.length>1){
        //     if (args[1].equals("-wait")){
        //         WAIT_TO_AUTOMATE = true;
        //     }
        // }


        int testMode = Integer.parseInt(args[0]);
        TestPreset preset = TestPreset.fromId(testMode);

        preset.printReport();

        MainWindow window = setupApp(true);
        window.automateImport(preset);
    }


}