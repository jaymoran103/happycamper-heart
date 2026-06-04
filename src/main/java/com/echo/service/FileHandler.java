package com.echo.service;

import java.awt.FileDialog;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JOptionPane;


/**
 * COPIED FROM VERSION 1 FOR CONVENIENCE
 * File Handler statically takes care of file selection and file writing, used mainly in the input and export steps.
 */
public class FileHandler {

    private static boolean SHOW_DIALOGS = true;
    
    /**
     * Wrapper method uses private selectFile to make a selection with a dialog.
     * @param parentFrame parent JFrame for dialog. Null is accepted
     * @param defaultFile default file for selector. Null is accepted
     * @return file path determined by a LOAD FileDialog.
     */
    public static File getLoadFile(JFrame parentFrame,File defaultFile){
        return selectFile(FileDialog.LOAD,"Select File", parentFrame, defaultFile);
    }

    /**
     * Wrapper method uses private selectFile to make a selection with a dialog.
     * @param parentFrame parent JFrame for dialog. Null is accepted
     * @param defaultFile default file for selector. Null is accepted
     * @return file path determined by a SAVE FileDialog.
     */
    public static File getSaveFile(JFrame parentFrame,File defaultFile){
        return selectFile(FileDialog.SAVE,"Select Save Destination", parentFrame, defaultFile);
    }

    /**
     * Accessed by wrappers, selectFile makes a file selection based on a given mode.
     * Selects an existing file or gets user input for a new file name and directory
     * @param mode selection mode corresponding with FileDialog.SAVE or FileDialog.LOAD
     * @param titleText String indicating title to display in popup dialog 
     * @return File selected by user, null upon cancellation
     */
    private static File selectFile(int mode, String titleText, JFrame parentFrame, File defaultFile){
        if (mode!=0 && mode!=1){
            throw new IllegalArgumentException("Bad mode given to FileHandler.SelectFile() - should be 0 or 1");
        }
        //Setup dialog for selection
        FileDialog dialog = new FileDialog(parentFrame, titleText, mode);

        if (defaultFile!=null && defaultFile.getParent()!=null){
            dialog.setFile(defaultFile.getName());
            dialog.setDirectory(defaultFile.getParent());
        }
        dialog.setVisible(true);

        // Get file, store components
        String directoryPath = dialog.getDirectory();
        String filePath = dialog.getFile();

        // Dispose dialog
        dialog.dispose();

        // If contents were valid, return non-null object
        if (directoryPath!=null && filePath!=null){
            return new File(directoryPath,filePath);
        }
        return null;
    }


    /**
     * writeFile uses a given file path and data array to write to a file.
     * Method is static as no file 
     */
	public static void writeFile(File file, String[][] dataMatrix) {
        String filePath = file.getAbsolutePath();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Iterate through data matrix and write each row
            for (String[] row : dataMatrix) {
                // Replace nulls with empty strings before joining - shouldn't be needed since Exporter handles empty values first
                String[] processedRow = Arrays.stream(row)
                    .map(s -> s == null ? "" : s)
                    .toArray(String[]::new);
                writer.write(String.join(",", processedRow));
                writer.newLine();
            }
            reportResult("Data Saved Successfully",false);
        } catch (IOException ex) {  
            reportResult("Error Exporting Data: "+ex.getMessage(),true);
        }
    }

    /**
     * Reports the result of an export attempt to the user
     * Displays as a dialog if SHOW_DIALOGS is true, otherwise printing to the console.
     * 
     * @param message message to display
     * @param hadError true if the operation had an error, false otherwise
     */
    public static void reportResult(String message, boolean hadError){
        if (SHOW_DIALOGS){
            String title = hadError ? "Export Error" : "Export Success";
            int messageType = hadError ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE;
            JOptionPane.showMessageDialog(null,message, title, messageType);
        } else {
            System.out.println("FileHandler: "+message);
        }
    }

    /**
     * Sets the static SHOW_DIALOGS to a given value.
     * Using a dialog to display messages is the norm, but gets in the way of automated testing.
     * 
     * @param status new value for SHOW_DIALOGS. 
     * true displays results with dialog, false prints results to console.
     */
    public static void setShowDialogs(boolean status){
        SHOW_DIALOGS = status;
    }

}
