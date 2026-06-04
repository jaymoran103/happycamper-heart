package com.echo.ui.help;

/**
 * HelpLine wraps a line of text to be featured in a help dialog page. 
 * Each instance uses a LineType enum to indicate the format the HTMLFormatter should apply.
 */
public class HelpLine{

    /**
     * Enum indicates a line's HTML format tag
     */
    public enum LineType{
        TITLE("h1"),
        HEADING("h2"),
        BOLD("b"),
        BASIC(null),
        ITALICS("em");

        String htmlTag;
        LineType(String _htmlTag){
            htmlTag=_htmlTag;
        }
    }

    public final LineType type;
    public final String text;
    public final int indents;

    
    /**
     * Constructor sets given args as fields
     * @param lineType Enum indicating HTML format
     * @param lineText String text making up main line content.
     * @param lineIndents int indicating number of indents to add
     */
    public HelpLine(LineType lineType,String lineText,int lineIndents){
        type = lineType;
        text = lineText;
        indents = lineIndents;
    }

    /**
     * Alternate constructor assumes 0 indents
     * @param lineType Enum indicating HTML format
     * @param lineText String text making up main line content.
     */
    public HelpLine(LineType lineType,String lineText){
        this(lineType,lineText,0);
    }
}
