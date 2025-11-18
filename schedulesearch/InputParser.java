package schedulesearch;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * the InputParser class has a static methode for parsing the input file and returning the environment "Env" and the starting state "s0"
 */
public final class InputParser
{
    // the names of input variables
    private static final String[] HEADINGS = {"Name:", "Lecture slots:", "Tutorial slots:", "Lectures:", "Tutorials:", "Not compatible:", "Unwanted:", "Preferences:", "Pair:", "Partial assignments:"};
    private static final String[] DAYPREFIX = {"MO", "TU", "FR"};
    /**
     * parses an input file for the environment variables and the starting state
     * The parser first gets all information from the file and creates the environment
     * Then it uses any partial assignments to populate the starting state. if there are
     * any error in parsing the file, or if the partial assignments are un-satisfiable then return an error
     * @param input_file: the input file to parse
     * @param env: the environment to return
     * @param s0: the start state to return
     * @return: false if there was an error, otherwise true
     */
    public static boolean ParseInputFile(String input_file, Environment env, Problem s0)
    {
        // sudo code:
        // parse the file for the information in environment, return false on any parsing error
        // parse the file for the partial assignments, return false on any parsing error
        // try adding the partial assignments to s0 
        // if any partial assignment leads to an invalid assignment in s0 return false
        // if all partial assignments are valid then return true

        // create the file object
        File file = new File(input_file);

        // check that the file exists
        if(!file.exists() || !file.isFile() || !file.canRead())
        {
            System.out.println("Could not load from file: " + input_file);
          
            return false;
        }

        System.out.println("\n\nReading from file: " + input_file);
        
        // create the file reader and file buffer
        FileReader reader;
        BufferedReader bufferedReader;

        // try to read the file with a FileReader and BufferedReader
        try{
            reader = new FileReader(file);
            bufferedReader = new BufferedReader(reader);
        }catch (IOException e)
        {
            System.out.println("Could not load from file: " + input_file);
  
            return false;
        }

        // Get the dataset name ###########################################################################################
        String name = ParseForName(bufferedReader);
        if(name == null || name.isEmpty())
        {
            System.out.println("Could not get dataset name from file: " + input_file);
            return false;
        }

        System.out.println("dataset name: " + name);

        // Get the Lecture Slots ###########################################################################################
        HashMap<Integer, Slot> lecture_slots = new HashMap<Integer, Slot>();
        if(!ParseLectureSlots(bufferedReader, lecture_slots))
        {
            System.out.println("Could not get lecture slots from file: " + input_file);
            return false;
        }
        

        System.out.println("\nLecture slots:\nsize: " + lecture_slots.size());
        for(Slot elm : lecture_slots.values())
        {
            elm.PrintSlot();
        }

        // Get the Tutorial Slots #############################################################################################
        HashMap<Integer, Slot> tutorial_slots = new HashMap<Integer, Slot>();
        if(!ParseTutorialSlots(bufferedReader, tutorial_slots))
        {
            System.out.println("Could not get tutorial slots from file: " + input_file);
            return false;
        }


        System.out.println("\nTutorial slots:\nsize: " + tutorial_slots.size());
        for(Slot elm : tutorial_slots.values())
        {
            elm.PrintSlot();
        }


        // close the file reader and file buffer
        try{
            reader.close();
            bufferedReader.close();
        } catch (IOException e)
        {
            System.out.println("could not close buffered reader");
        }
        System.out.println("Finished loading from file: " + input_file);

        return true;
    }

    /**
     * Parse the input file for the data set name bellow the keyword "Name:"
     * @param bufferedReader: the file reader to get the lines from
     * @return: the name of the dataset, null or empty if not found
     */
    private static String ParseForName(BufferedReader bufferedReader)
    {
        // read each line from the file
        String nextLine;
        try{
            nextLine = bufferedReader.readLine();
        }catch (IOException e)
        {
            return null;
        }

        // loop through each line until we find the first heading "NAME"
        while((nextLine != null) && !nextLine.contains(HEADINGS[0]))
        {
            // read the next line from the file
            try{
                nextLine = bufferedReader.readLine();
            }catch (IOException e)
            {
                return null;
            }
        }

        // read the next line from the file
        try{
            nextLine = bufferedReader.readLine();
        }catch (IOException e)
        {
            return null;
        }

        // get the dataset name (the first non-empty line after the "Name:" heading)
        while((nextLine != null) && (nextLine.length() == 0))
        {
            // read the next line from the file
            try{
                nextLine = bufferedReader.readLine();
            }catch (IOException e)
            {
                return null;
            }
        }

        return nextLine;
    }

    /**
     * Parse the input file for the lecture slots
     * @param bufferedReader: the file reader to get the lines from
     * @param lecture_slots: the hashmap of lecture slots to put the results in
     * @return: true if parse was successfull, false if errors occured
     */
    private static boolean ParseLectureSlots(BufferedReader bufferedReader, HashMap<Integer, Slot> lecture_slots)
    {
        // read each line from the file
        String nextLine;
        try{
            nextLine = bufferedReader.readLine();
        }catch (IOException e)
        {
            return false;
        }

        // loop through each line until we find the first heading "Lecture slots"
        while((nextLine != null) && !nextLine.contains(HEADINGS[1]))
        {
            // read the next line from the file
            try{
                nextLine = bufferedReader.readLine();
            }catch (IOException e)
            {
                return false;
            }
        }

        // read the next line from the file
        try{
            nextLine = bufferedReader.readLine();
        }catch (IOException e)
        {
            return false;
        }

        // load the lecture slots
        while((nextLine != null) && (!nextLine.contains(HEADINGS[2])) )
        {
            Slot temp_slot = new Slot();
            if(TryGetSlotFromLine(nextLine, temp_slot))
            {
                lecture_slots.put(temp_slot.lec_hash,temp_slot);
            }
            // read the next line from the file
            try{
                nextLine = bufferedReader.readLine();
            }catch (IOException e)
            {
                return false;
            }
        }

        // if the next heading was not found then return false
        if(!nextLine.contains(HEADINGS[2]))
        {
            return false;
        }

        return true;
    } 

    /**
     * Parse the input file for the lecture slots
     * @param bufferedReader: the file reader to get the lines from
     * @param lecture_slots: the hashmap of lecture slots to put the results in
     * @return: true if parse was successfull, false if errors occured
     */
    private static boolean ParseTutorialSlots(BufferedReader bufferedReader, HashMap<Integer, Slot> tutorial_slots)
    {
        // read each line from the file
        String nextLine;

        // read the next line from the file
        try{
            nextLine = bufferedReader.readLine();
        }catch (IOException e)
        {
            return false;
        }

        // load the lecture slots
        while((nextLine != null) && (!nextLine.contains(HEADINGS[3])) )
        {
            Slot temp_slot = new Slot();
            if(TryGetSlotFromLine(nextLine, temp_slot))
            {
                tutorial_slots.put(temp_slot.lec_hash,temp_slot);
            }
            // read the next line from the file
            try{
                nextLine = bufferedReader.readLine();
            }catch (IOException e)
            {
                return false;
            }
        }

        // if the next heading was not found then return false
        if(!nextLine.contains(HEADINGS[3]))
        {
            return false;
        }

        return true;
    } 


    /**
     * takes a string and tries to convert it to a lecture slot
     * @param line: the line to parse for the slot information
     * @param slot: the slot to put the return data into
     * @return: true if the line could be parsed for slot information, false otherwise
     */
    private static boolean TryGetSlotFromLine(String line, Slot return_slot)
    {
        // split the string by ',' deliminator
        String[] elements = line.split(",");
        // there must be 5 elements (day,time,lecturemax,lecturemin,allecturemax)
        if(elements.length != 5)
        {
            return false;
        }

        // get the day
        switch(elements[0])
        {
            case "MO":
                return_slot.day = 0;
                break;
            case "TU":
                return_slot.day = 1;
                break;
            case "WE":
                return_slot.day = 2;
                break;
            case "TR":
                return_slot.day = 3;
                break;
            case "FR":
                return_slot.day = 4;
                break;
            default:
                // if the day is not found then return false
                return false;
        }

        // get the time
        elements[1] = elements[1].strip();
        elements[2] = elements[2].strip();
        elements[3] = elements[3].strip();
        elements[4] = elements[4].strip();
        String[] hr_min = elements[1].split(":");
        // there should be 2 elements
        if(hr_min.length != 2)
        {
            return false;
        }

        // convert the strings to numbers
        int[] buffer = new int[1];
        if(!GetSafeIntFromString(hr_min[0], buffer))
        {
            return false;
        }
        
        // record the hour
        return_slot.hour = buffer[0];
        if(!GetSafeIntFromString(hr_min[1], buffer))
        {
            return false;
        }
        // record the minute
        return_slot.min = buffer[0];
        
        // get the lecturemax value
        if(!GetSafeIntFromString(elements[2], buffer))
        {
            return false;
        }
        // record the minute
        return_slot.max = buffer[0];

        // get the lecturemin value
        if(!GetSafeIntFromString(elements[3], buffer))
        {
            return false;
        }
        // record the minute
        return_slot.min = buffer[0];

        // get the allecturemax value
        if(!GetSafeIntFromString(elements[4], buffer))
        {
            return false;
        }
        // record the minute
        return_slot.almax = buffer[0];

        // setup the slot
        return_slot.SetupSlot();

        return true;
    }

    private static boolean GetSafeIntFromString(String input_string, int[] float_return)
    {
        // check that the given array is not null and has at least one index for assigning values
        if(float_return == null || float_return.length == 0)
        {
            return false;
        }

        // try to get an integer from the string
        try{
            float_return[0] = Integer.parseInt(input_string);
            // if the string was a float then return the float by reference and return true
            return true;
        }catch(NumberFormatException e)
        {
            // if the string was not a float then return false and an error message to the user
            float_return[0] = -1;
            return false;
        }
    }
}


class LectureData
{
    String course_descriptor; // e.g. CPSC 433
    int lec_num; // e.g LEC 01 -> lec_num = 1
    boolean is_al; // is this an active learning lecture or not
}


class TutorialData
{
    String course_descriptor; // e.g. CPSC 433
    int lec_num; // e.g. LEC 01 -> lec_num = 1 (defualt to 1 if not included)
    int tut_num; // e.g. TUT/LAB 04 -> tut_num = 4
    boolean is_al; // is this an active learning tutorial or not
}
