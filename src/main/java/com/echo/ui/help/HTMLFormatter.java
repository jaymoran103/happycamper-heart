package com.echo.ui.help;

import java.util.ArrayList;

/**
 * HTMLFormatter provides a public static method that turns a sequence of HelpLine wrappers into a full help page with inline html formatting 
 */
public class HTMLFormatter{

    public static final String START = "<html>";
    public static final String END = "</html>";
    
    public static final String CSS_STYLE = "<style>"
                                            +   "h1{ font-weight:bold; font-size:16px }"
                                            +   "h2{ font-weight:bold; font-size:12px; margin-top: 0;margin-bottom: 0}"
                                            + "body{ margin-bottom: 0}"
                                         + "</style>";

    public static final String LINEBREAK = "<br>";
    public static final String INDENT = "&nbsp;&nbsp;&nbsp;";


    /**
     * Converts a series of HelpLines into a single string of text with inline html formatting
     * @param helpLines ArrayList of HelpLine instances, containing text and info about format
     * @return a String, featuring the given content in html format
     */
    public static String buildTextElement(ArrayList<HelpLine> helpLines){
        //If no lines were given, return a string saying so (this shouldnt happen)
        if (helpLines==null||helpLines.isEmpty()){
            return "(No text to display)";
        }
        //Iterate through given lines, using helper method to build said lines as HTML text
        StringBuilder builder = new StringBuilder();
        for (HelpLine line:helpLines){
            builder.append(formatHelpLine(line));
        }

        //Final string includes the html tag and inline style section
        String htmlText = START+CSS_STYLE+builder.toString()+END;
        return htmlText;
    }

    /**
     * Given an individual HelpLine, reformat that line as HTML text and return it
     * @param line HelpLine instance, containing info about text content, style, and indentation
     * @return text from the Helpline, formatted as HTML text
     */
    private static String formatHelpLine(HelpLine line){
        StringBuilder lineBuilder = new StringBuilder();

        //Append indents based on given int
        for (int i=0;i<line.indents;i++){
            lineBuilder.append(INDENT);
        }

        //Store text from line, converting newline escape characters to html field
        String displayText = line.text.replaceAll("\n",LINEBREAK);
        
        //Append line text to builder, including html tags if indicated by the enum.
        String tag = line.type.htmlTag;
        if (tag!=null){
            lineBuilder.append(String.format("<%s>%s</%s>",tag,displayText,tag));
        } else {
            lineBuilder.append(displayText);
        }
        
        //Append line break and return
        lineBuilder.append(LINEBREAK);
        return lineBuilder.toString();
    }
    
}
