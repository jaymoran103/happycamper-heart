package com.echo.ui.help;

import java.util.ArrayList;
import java.util.Map;

import com.echo.domain.RosterHeader;
import com.echo.feature.MedicalFeature;
import com.echo.feature.PreferenceFeature;
import com.echo.feature.ProgramFeature;
import com.echo.feature.SwimLevelFeature;
import com.echo.filter.MedicalFilter;
import com.echo.filter.PreferenceFilter;
import com.echo.filter.SortedProgramFilter;
import com.echo.filter.SwimLevelFilter;
import com.echo.ui.help.HelpLine.LineType;

/**
 * PageContentBuilder statically assembles sets of text to fill each help dialog page.
 */
public class PageContentBuilder {

    /**
     * Enum lists the different help pages assembled by this class
     */
    public enum HelpPage{
        WELCOME,
        CAMPER_FILE,ACTIVITY_FILE,FEATURES_OVERVIEW,
        EXPORT;
    }

    /**
     * Access method uses a switch to build the relevant set of pages
     * @param page Enum indicating page set to build
     * @return String[] of HTML formatted strings, each providing content for a separate help page
     */
    public static String[] getCardStrings(HelpPage page){
        return switch (page) {
            //Intro
            case WELCOME -> buildPage_Welcome();

            //Inputs Step
            case CAMPER_FILE -> buildPage_CamperFile();
            case ACTIVITY_FILE -> buildPage_ActivityFile();
            case FEATURES_OVERVIEW -> buildPage_FeatureOverview();

            case EXPORT -> new String[]{card_Exporter()};

            default -> buildPage_Welcome();
        };
    }

//MULTI PAGE BUILD METHODS

    /**
     * Assembles a set of help pages introducing the user to the program and its normal use.
     * @return String[] of HTML formatted strings, each providing content for a separate help page
     */
    private static String[] buildPage_Welcome(){
        return new String[]{card_Welcome_0(),card_Welcome_1(),card_Welcome_1b(),card_Welcome_2(),card_Welcome_3()};
    }

    /**
     * Assembles a set of help pages explaining the camper file's generation and use.
     * @return String[] of HTML formatted strings, each providing content for a separate help page
     */
    private static String[] buildPage_CamperFile(){
        return new String[]{camperPage1(),camperPage2(),camperPage3(),camperPage4()};
    }

    /**
     * Assembles a set of help pages explaining the activity file's generation and use.
     * @return String[] of HTML formatted strings, each providing content for a separate help page
     */
    private static String[] buildPage_ActivityFile(){
        return new String[]{activityPage1(),activityPage2()};
    }

    private static String[] buildPage_FeatureOverview(){
        return new String[]{featureOverview(),feature_programs(),feature_preferences(),feature_swimLevels(),feature_medical()};
    }

    




//SINGLE PAGE BUILD METHODS



    /**
     * Assembles an introductory help page with a broad overview of program functionality.
     * @return String with HTML formatting, to be shown as a help page.
     */
    private static String card_Welcome_0(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Welcome to HappyCamper"));
        lines.add(new HelpLine(LineType.BASIC,"HappyCamper is a tool for catching blind spots in schedules generated on Campminder.com. This version is configured to compare two rosters:"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"- The <b>Camper Roster</b> list participants who need assignments.",3));
        lines.add(new HelpLine(LineType.BASIC,"- The <b>Activity Roster</b> lists elective assignments.",3));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"The following pages contain a step by step overview of the intended workflow."));
        return HTMLFormatter.buildTextElement(lines);
    }

    /**
     * Assembles an introductory help page explaining the input selection step.
     * @return String with HTML formatting, to be shown as a help page.
     */
    private static String card_Welcome_1(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Step 1: Select Inputs"));
        lines.add(new HelpLine(LineType.BASIC,"This app compares two rosters, each a .csv file generated on Campminder.com. Instructions on generating these can be found on the [ ? ] buttons on the Import page."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"To begin, click the <b>'Import'</b> button at the top left."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Once you select inputs, HappyCamper makes sure the data is usable, then compares the rosters, checking for missing assignments and compiling some statistics."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"If there's an issue with the data provided, you'll see a prompt with instructions for fixing it. Otherwise the <b>'Data View'</b> page will open automatically."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,""));
        // lines.add(new HelpLine(LineType.BASIC,""));

        return HTMLFormatter.buildTextElement(lines);
    }

    private static String card_Welcome_1b(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Additional Features"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"The <b>'Optional Features'</b> menu on the <b>'Import'</b> allows you to enable additional data processing and analysis features before you import files."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Features allow you to track assignment issues across programs, see where preferences aren't being met, and ensure campers are in activities that are safe for their abilities."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Most features require extra data columns, obtained from Campminder alongside the basic columns."));
        lines.add(new HelpLine(LineType.BASIC,"Once you've exported your data, you can return to the input selection step at any time to compare new rosters."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"If you select a feature that requires additional columns, but those columns are missing from your data, you'll see a prompt with instructions for fixing it."));
        lines.add(new HelpLine(LineType.BASIC,""));
        return HTMLFormatter.buildTextElement(lines);
    }
		
    /**
     * Assembles an introductory help page explaining the data view step.
     * @return String with HTML formatting, to be shown as a help page.
     */
    private static String card_Welcome_2(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.HEADING,"Step 2: View and Sort Data"));
    

        lines.add(new HelpLine(LineType.BASIC,"After selecting input files, the <b>'Data Table'</b> displays the contents of both rosters as a single spreadsheet. The data can be exported for use elsewhere, but in-app features can make it easy to sift through contents and isolate problems first."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"On this page you can filter, sort, and highlight data to show exactly what you need. The sidebar features collapsible filters, and clicking on table headers sorts the table by that column's contents."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"The <b>'Column Settings'</b> button allows you select which column visibility and width, and <b>'View Settings'</b> modifies qualities like data highlighting and row contrast, and placeholders for empty values"));

        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"The table itself can be modified with two settings menus. The <b>'Column Settings'</b> button allows you to show or hide columns, and the 'View Settings' button allows you to modify visual settings like data highlighting and row contrast."));
        lines.add(new HelpLine(LineType.BASIC,""));


        return HTMLFormatter.buildTextElement(lines);
    }

    /**
     * Assembles an introductory help page explaining the data export step.
     * @return String with HTML formatting, to be shown as a help page.
     */
    private static String card_Welcome_3(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Step 3: Export Data"));
        lines.add(new HelpLine(LineType.BASIC,"Clicking the <b>'Export'</b> button will prompted you to choose some export settings and a save destination."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Data can be exported to match the presentation on the view page, or with all available data, making it easy to export what matters most."));
        return HTMLFormatter.buildTextElement(lines);
    }


    

    



    /**
     * Assembles a help page explaining the basic role of a camper roster.
     * @return String with HTML formatting, to be shown as a help page.
     */
    private static String camperPage1(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Camper Roster"));
        lines.add(new HelpLine(LineType.BASIC,"The Camper Roster lists enrolled campers for a given session, enabling HappyCamper to find campers who weren't assigned activities."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"This roster should be a .csv file generated by campminder as a 'User Report'."));
        return HTMLFormatter.buildTextElement(lines);
    }
    
    /**
     * Assembles a help page detailing the process of generating a camper roster.
     * @return String with HTML formatting, to be shown as a help page.
     */
    private static String camperPage2(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Generating a Camper Roster"));
        lines.add(new HelpLine(LineType.ITALICS,"(This process assumes you have access to a Campminder.com admin account, and basic familiarity with the platform)"));

        lines.add(new HelpLine(LineType.BASIC,"On Campminder.com, navigate to Reporting > User Reports > Camper"));
        lines.add(new HelpLine(LineType.BASIC,"Select the green 'New Report' button at the top."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BOLD,"1. Selection Criteria"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Under Partition, select 'Camper Management'"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Under Session, choose the programs you want to check for."));
        lines.add(new HelpLine(LineType.ITALICS,"If you're not sure which to include, don't be afraid to select them all. HappyCamper's sorting features make it easy to hide programs aren't relevant to you later."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Under 'Session Status' select 'Enrolled'"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Under 'Person Status' Select 'Active'"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BOLD,"2. Fields/Sort"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Add the following fields from the left menu:"));
        lines.add(new HelpLine(LineType.BASIC,"Last Name",3));
        lines.add(new HelpLine(LineType.BASIC,"First Name",3));
        lines.add(new HelpLine(LineType.BASIC,"Preferred Name",3));
        lines.add(new HelpLine(LineType.BASIC,"Camp Grade",3));
        lines.add(new HelpLine(LineType.BASIC,"Enrolled Sessions/Programs",3));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"The order you add them doesn't matter. You can add other fields if you'd like more info in the spreadsheet HappyCamper generates."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BOLD,"3. Options/Finish"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Click 'Generate Report' and Export as CSV"));
        return HTMLFormatter.buildTextElement(lines);
    }



    /**
     * Assembles a help page explaining the basic format requirements of an activity roster.
     * @return String with HTML formatting, to be shown as a help page.
     */
    private static String camperPage3(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Basic Requirements"));
        lines.add(new HelpLine(LineType.BASIC,"Every Camper roster should have columns with the following headers:"));
        // lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,RosterHeader.FIRST_NAME.camperRosterName,3));
        lines.add(new HelpLine(LineType.BASIC,RosterHeader.PREFERRED_NAME.camperRosterName,3));
        lines.add(new HelpLine(LineType.BASIC,RosterHeader.LAST_NAME.camperRosterName,3));
        lines.add(new HelpLine(LineType.BASIC,RosterHeader.GRADE.camperRosterName,3));
        lines.add(new HelpLine(LineType.BASIC,RosterHeader.ESP.camperRosterName,3));
        lines.add(new HelpLine(LineType.BASIC,""));        
        lines.add(new HelpLine(LineType.BASIC,"If you're generating the files directly from campminder, there shouldn't be any issues with file format or contents."));
        return HTMLFormatter.buildTextElement(lines);
    }

    /**
     * Assembles a help page explaining the basic format requirements of an activity roster.
     * @return String with HTML formatting, to be shown as a help page.
     */
    private static String camperPage4(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Additional Features"));
        lines.add(new HelpLine(LineType.BOLD,"Program Feature:"));
             
        lines.add(new HelpLine(LineType.BOLD,"Preference Feature:"));
        lines.add(new HelpLine(LineType.BASIC,"The Preference Feature compares a camper's <b>"+RosterHeader.PREFERENCES.camperRosterName+"</b> field to their enrolled activities, compiling some takeaways."));
        lines.add(new HelpLine(LineType.BASIC,""));
        return HTMLFormatter.buildTextElement(lines);
    }



    /**
     * Assembles a help page explaining the basic role of an activity roster.
     * @return String with HTML formatting, to be shown as a help page.
     */
    private static String activityPage1(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Activity Roster"));
        lines.add(new HelpLine(LineType.BASIC,"The Activity roster lists activities assigned to campers."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"This roster should be a .csv file generated by campminder as part of the scheduling process"));
        return HTMLFormatter.buildTextElement(lines);
    }

    /**
     * Assembles a help page detailing the process of generating an activity roster.
     * @return String with HTML formatting, to be shown as a help page.
     */
    private static String activityPage2(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Generating an Activity Roster"));
        lines.add(new HelpLine(LineType.BASIC,"On Campminder.com, navigate to Scheduling > Reports > Elective Rosters"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Select the 'Assignment Set' and 'Cabin Plan' that pertain to your current schedule."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"For other settings, there's no need to narrow down or sort this report. HappyCamper has more flexible filters and sorting features, making it easy to choose exactly what ends up in the file you export."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"When finished, click 'Generate CSV Report'"));
        return HTMLFormatter.buildTextElement(lines);
    }


    private static String featureOverview(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Features Menu"));
        lines.add(new HelpLine(LineType.BASIC,"The <b>Optional Features</b> menu allows you to enable additional data processing and analysis features before you import files."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Most require additional columns in the Camper Roster, obtained from Campminder just like the basic columns."));
        // lines.add(new HelpLine(LineType.BASIC,""));
        // lines.add(new HelpLine(LineType.BASIC,"There is little drawback to adding or excluding features, as you can change your selection at any time by clicking 'import' again and changing your settings"));
        return HTMLFormatter.buildTextElement(lines);
    }

    private static String feature_programs(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Program Feature"));
        lines.add(new HelpLine(LineType.BASIC,"The <b>"+ProgramFeature.FEATURE_NAME+"</b> determines each camper's enrolled program, and provides a filter that separates programs based on their camptivity assignment trends"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Column Required: <b>"+RosterHeader.ESP.camperRosterName+"</b>"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Column Created: <b>"+RosterHeader.PROGRAM.standardName+"</b> (camper's current program)"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Filter Created: <b>"+SortedProgramFilter.FILTER_NAME+"</b> (toggles programs and groups)"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"The program is determined based on the 'current' session, which is detected from all campers' ESP fields. If a camper isn't enrolled in the current session, their full ESP field is copied to the Program column."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"This feature isn't confused by A and B sessions, but if you added campers from different numeric sessions (like 2 vs 3) it might get confused."));
        return HTMLFormatter.buildTextElement(lines);
    }

    private static String feature_preferences(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Preference Feature"));
        lines.add(new HelpLine(LineType.BASIC,"The <b>"+PreferenceFeature.FEATURE_NAME+"</b> compares a camper's activity preferences to the assignments they actually got, quantifying preference satisfaction in a few ways"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Column Required: <b>"+RosterHeader.PREFERENCES.camperRosterName+"</b>"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Columns Created: "));
        lines.add(new HelpLine(LineType.BASIC,"   - <b>"+RosterHeader.PREFERENCE_SCORE.standardName+"</b> - Ranks preference satisfaction out of 100."));
        lines.add(new HelpLine(LineType.BASIC,"   - <b>"+RosterHeader.PREFERENCE_PERCENTILE.standardName+"</b> - Compares "+RosterHeader.PREFERENCE_SCORE.standardName+" to other campers."));
        lines.add(new HelpLine(LineType.BASIC,"   - <b>"+RosterHeader.UNREQUESTED_ACTIVITIES.standardName+"</b> - Lists activities the camper did NOT request."));
        lines.add(new HelpLine(LineType.BASIC,"   - <b>"+RosterHeader.SCORE_BY_ROUND.standardName+"</b> - Shows how the camper ranked each round."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Filter Created: <b>"+PreferenceFilter.FILTER_NAME+"</b>"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"The Preference Filter allows you to show or hide campers based on whether they have unrequested activities. This can help identify issues with the scheduling algorithm, and highlight campers who may need to be talked to or reassigned."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Note that non-signup activites like Swim Lessons and Horseback Riding are not considered when calculating preference scores."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Since this feature adds so many columns, most are hidden by default. Change this by clicking <b>Column Settings</b> in the top bar"));
        return HTMLFormatter.buildTextElement(lines);
    }


    private static String feature_swimLevels(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Swim Level Feature"));
        lines.add(new HelpLine(LineType.BASIC,"The <b>"+SwimLevelFeature.FEATURE_NAME+"</b> checks that each camper is enrolled in activities appropriate for their swim level."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Column Required: <b>"+RosterHeader.SWIMCOLOR.camperRosterName+"</b>"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Column Created: <b>"+RosterHeader.SWIMCONFLICTS.standardName+"</b> (lists incompatible activities)"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Filter Created: <b>"+SwimLevelFilter.FILTER_NAME+"</b> (shows or hides campers with incompatible activities)"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Similar to the Preference Feature, the Swim Level Feature lists any activities that aren't a good fit."));
        lines.add(new HelpLine(LineType.BASIC, ""));
        lines.add(new HelpLine(LineType.BASIC,"For now, swim levels are built into the program, as follows: "));
        lines.add(new HelpLine(LineType.BASIC,"" ));
        lines.add(new HelpLine(LineType.BOLD,"Activity - Level"));
        
        Map<String,Integer> activityLevels = new SwimLevelFeature().getDefaultActivityRequirements();
        Map<String,Integer> levelNames = new SwimLevelFeature().getDefaultLevelNameMappings();
        
        for(Map.Entry<String,Integer> entry: activityLevels.entrySet()){
            String activity = entry.getKey();
            Integer level = entry.getValue();
            String stringlevel = "?";
            for (Map.Entry<String,Integer> innerEntry: levelNames.entrySet()){
                if (innerEntry.getValue().equals(level)){
                    stringlevel = innerEntry.getKey();
                    break;
                }
            }

            lines.add(new HelpLine(LineType.BASIC,activity + " - " + stringlevel));

        }

        return HTMLFormatter.buildTextElement(lines);
    }

    private static String feature_medical(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Medical Feature"));
        lines.add(new HelpLine(LineType.BASIC,"The <b>"+MedicalFeature.FEATURE_NAME+"</b> checks for any medical notes in the camper roster, and provides a filter to show campers with data there. "));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Column Required: <b>"+RosterHeader.MEDICAL_NOTES.camperRosterName+"</b>"));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"Filter Created: <b>"+MedicalFilter.FILTER_NAME+"</b> (shows or hides campers with medical notes)"));
        return HTMLFormatter.buildTextElement(lines);
    }



    /**
     * Assembles a help page detailing table features and the settings and options buttons.
     * @return String with HTML formatting, to be shown as a help page.
     */
    private static String card_OtherOptions(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        // lines.add(new HelpLine(LineType.HEADING,"Other View Options"));
        lines.add(new HelpLine(LineType.HEADING,"Table Features"));
        lines.add(new HelpLine(LineType.BASIC,"You can click a header to sort the table by that column, and dragging the header's edges will resize the column."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.HEADING,"Settings Menu"));
        lines.add(new HelpLine(LineType.BASIC,"The 'Settings' button allows you select which columns are shown and hidden, and modify viws settings like data highlighting and row contrast."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"The default columns are typically most helpful for finding roster issues, other rows may have helpful info, especially any non-essential columns you added when generating the Camper Roster."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.HEADING,"Exporting"));
        lines.add(new HelpLine(LineType.BASIC,"The 'Export' button prompts you to choose what data to export, and to choose a file name and destination."));
        lines.add(new HelpLine(LineType.BASIC,""));
        lines.add(new HelpLine(LineType.BASIC,"To hide or show columns for the final export, filter rows and columns using the sidebar and settings menu before clicking 'Export'."));
        return HTMLFormatter.buildTextElement(lines);
    }
    

    /**
     * Assembles a help page detailing table features and the settings and options buttons.
     * @return String with HTML formatting, to be shown as a help page.
     */

    private static String card_Exporter(){
        ArrayList<HelpLine> lines = new ArrayList<>();
        lines.add(new HelpLine(LineType.HEADING,"Export Step"));
        lines.add(new HelpLine(LineType.BASIC,"This one currently isn't shown. Export Dialog should be faily self explanatory"));
        return HTMLFormatter.buildTextElement(lines);
    }

}
