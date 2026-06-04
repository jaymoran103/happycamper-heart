package com.echo.domain;

import java.util.HashSet;
import java.util.Set;

/**
 * This class adjusts and shortens program names for functional and display purposes.
 * 
 * - "All Gender" groups are listed as separate programs for enrollment purposes, but should usually be treated as part of the main program.
 * - Leadership program names are listed with inconsistent formatting, resulting in program names that still differed after the all gender adjustment
 * - Other names are shortened for displaying in narrow components without obscuring meaning
 * 
 */
public class ProgramNameAdjuster{

    /**
     * Enum distinguishes between shortening and standardizing modes.
     */
    public enum Mode{
        SHORTEN,
        STANDARDIZE
    }

    /**
     * Replacement sets to be applied to program names. 
     */
    public enum ReplacementPairs{
        STANDARDIZE_ALLGENDER(Mode.STANDARDIZE," - All Gender","",true),
        STANDARDIZE_LEADERSHIP(Mode.STANDARDIZE,"(?i)[-\\s]in[-\\s]Training","-In-Training",true),

        SHORTEN_ALLGENDER(Mode.SHORTEN,"All Gender","AG",true),
        SHORTEN_BACKPACKING(Mode.SHORTEN," Backpacking","",true),
        SHORTEN_CIT(Mode.SHORTEN,"Counselor-In-Training","CIT",false),
        SHORTEN_LIT(Mode.SHORTEN,"LEADER-In-Training","LIT",false);

        public final String original;
        public final String replacement;
        public final boolean enabled;
        public final Mode mode;

        ReplacementPairs(Mode mode,String original,String replacement,boolean enabled){
            this.original = original;
            this.replacement = replacement;
            this.enabled = enabled;
            this.mode = mode;
        }
    }

    //Set of replacement pairs to apply when used
    private final Set<ReplacementPairs> replacementPairs;

    /**
     * Constructor initializes the replacement pair based on enum defaults
     */
    public ProgramNameAdjuster(Mode givenMode){
        replacementPairs = new HashSet<>();
        //Add each pair of the relevant mode, indicated as enabled
        for (ReplacementPairs pair : ReplacementPairs.values()){
            if (givenMode==pair.mode && pair.enabled){
                replacementPairs.add(pair);
            }
        }
    }

    /**
     * Alternate constructor uses a given set as the replacementPairs field
     * @param replacementPairs the pairs to apply to given Strings
     */
    public ProgramNameAdjuster(Set<ReplacementPairs> replacementPairs){
        this.replacementPairs = Set.copyOf(replacementPairs);
    }

    /**
     * Adjusts a given name if possible, based on replacement sets 
     * @param name String to adjust
     * @return adjusted String, shortened or standardized based on specified mode / replacement pair set
     */
    public String adjustIfPossible(String name){
        if (name==null){return null;}
        for (ReplacementPairs pair : replacementPairs){
            name = name.replaceAll(pair.original, pair.replacement); //does this handle immutability right? could i do it without the assignment?
        }
        return name;
    }
    



}