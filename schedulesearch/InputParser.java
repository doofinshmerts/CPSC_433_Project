package schedulesearch;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * the InputParser class has a static methode for parsing the input file and returning the environment "Env" and the starting state "s0"
 */
public final class InputParser
{
    // the names of input variables
    private static final String[] HEADINGS = {"Name:", "Lecture slots:", "Tutorial slots:", "Lectures:", "Tutorials:", "Not compatible:", "Unwanted:", "Preferences:", "Pair:", "Partial assignments:"};
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
        env.dataset_name = name;

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
        env.lecture_slots = lecture_slots;

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
        env.tutorial_slots = tutorial_slots;

        // Get the Lectures ######################################################################################################
        // key is the course identifier (name and number), The key for the inner map is the lecture number 
        HashMap<String, HashMap<Integer, LectureData>> lec_tut_data = new HashMap<String, HashMap<Integer, LectureData>>();
        if(!ParseLectureData(bufferedReader, lec_tut_data))
        {
            System.out.println("Could not get the lecture data from file: " + input_file);
            return false;
        }

        System.out.println("\nLecture data:\nsize: " + lec_tut_data.size());

        int count = 0;
        for(HashMap<Integer, LectureData> elm : lec_tut_data.values())
        {
            System.out.println("");
            count += elm.size();
            for(LectureData elem : elm.values())
            {
                elem.PrintData();
            }
        }
        env.num_lectures = count;
        
        // Get the tutorials ######################################################################################################
        if(!ParseTutorialData(bufferedReader, lec_tut_data))
        {
            System.out.println("Could not get the tutorial data from file: " + input_file);
            return false;
        }

        count = 0;
        System.out.println("\nTutorial data:\nsize: " + lec_tut_data.size());
        for(HashMap<Integer, LectureData> elm : lec_tut_data.values())
        {
            System.out.println("");
            for(LectureData elem : elm.values())
            {
                elem.PrintData();
                ArrayList<TutorialData> temp = elem.tutorials;
                count += temp.size();
                for(TutorialData item: temp)
                {
                    item.PrintData();
                } 
            }
        }
        env.num_tutorials = count;


        // Convert TutorialData and lectureData to Tutorials and Lectures #########################################################################################
        
        int t_count = 0;
        int l_count = 0;
        int s_count = 0;
        
        env.tutorials = new Tutorial[env.num_tutorials];
        env.lectures = new Lecture[env.num_lectures];

        for(HashMap<Integer, LectureData> elm : lec_tut_data.values())
        {
            for(LectureData lec : elm.values())
            {
                ArrayList<TutorialData> temp = lec.tutorials;
                for(TutorialData tut: temp)
                {
                    // convert this Tutorial data to a Tutorial
                    env.tutorials[t_count] = tut.ConvertToTutorial(t_count);            
                    // increment the tutorial count
                    t_count++;

                } 
                // convert this lecture data to a lecture
                env.lectures[l_count] = lec.ConvertToLecture(l_count, s_count);
                // increment the lecture count
                l_count++;
            }

            // increment the section count
            s_count++;
        }
        
        // set parent lecture number in each tutorial for backwards lookup
        for(int i = 0; i < env.num_lectures; i++)
        {
            // get the ids of the tutorials associated with this lecture
            Integer[] tuts = env.lectures[i].tutorials;
            int id = env.lectures[i].id;

            for(int j = 0; j < tuts.length; j++)
            {
                env.tutorials[tuts[j]].lec_id = id;    
            }
        }

        // Print the Lectures and Tutorials
        System.out.println("\nLecture and Tutorial Data:\n");
        // set parent lecture number in each tutorial for backwards lookup
        for(int i = 0; i < env.num_lectures; i++)
        {
            // get the ids of the tutorials associated with this lecture
            Integer[] tuts = env.lectures[i].tutorials;
            env.lectures[i].PrintData();

            for(int j = 0; j < tuts.length; j++)
            {
                env.tutorials[tuts[j]].PrintData();    
            }
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
     * Parse the input file for the tutorial data
     * @param bufferedReader: the file reader to get the lines from
     * @param lec_tut_data: the hashmap to store the tutorial data in
     * @return: true if parse was successfull, false if errors occured
     */
    private static boolean ParseTutorialData(BufferedReader bufferedReader, HashMap<String, HashMap<Integer, LectureData>> lec_tut_data)
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

        // load the lecture data
        while((nextLine != null) && (!nextLine.contains(HEADINGS[5])) )
        {
            // object for holding retreved lecture data
            TutorialData temp_tut = new TutorialData();
            // try to get the lecture data from the string
            if(TryGetTutorialFromLine(nextLine, temp_tut))
            {
                // does this course descriptor already exist or not?
                if(lec_tut_data.containsKey(temp_tut.course_descriptor))
                {
                    // already exists so get the current map of lecturData elements to add to
                    HashMap<Integer, LectureData> temp = lec_tut_data.get(temp_tut.course_descriptor);
                    if(temp.containsKey(temp_tut.lec_num))
                    {
                        // add the tutorial to the lecture data 
                        LectureData lecture = temp.get(temp_tut.lec_num);
                        lecture.tutorials.add(temp_tut);

                        // add this lecture data back to the map
                        temp.put(temp_tut.lec_num, lecture);
                        // add this map back to the main map
                        lec_tut_data.put(temp_tut.course_descriptor, temp);
                    }
                    else
                    {
                        System.out.println("you are trying to add tutorial without lecture: " + temp_tut.course_descriptor + " " + temp_tut.lec_num);
                    }
                }
                else
                {
                    System.out.println("you are trying to add tutorial without lecture: " + temp_tut.course_descriptor);
                }
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
        if(!nextLine.contains(HEADINGS[5]))
        {
            return false;
        }

        return true;
    } 

    /**
     * takes a string and tries to convert it to tutorial data
     * @param line: the line to parse for the tutorial data
     * @param lec: the tutorial data structure to return the information in
     * @return: true if the line could be parsed for tutorial data, false otherwise
     */
    private static boolean TryGetTutorialFromLine(String line, TutorialData tut)
    {
        // split the string by ',' deliminator
        String[] elements;

        // try spliting on either "TUT" or "LAB"
        if(line.contains("TUT"))
        {
            elements = line.split("TUT");
        }
        else if(line.contains("LAB"))
        {
            elements = line.split("LAB");
        }
        else
        {
            return false;
        }

        elements[0] = elements[0].strip();
        elements[1] = elements[1].strip();

        // check to see if the name contains "LEC"
        if(elements[0].contains("LEC"))
        {
            // split on "LEC"
            String[] lec_info = elements[0].split("LEC");
            // ensure that there is course descriptor and lecture number
            if(lec_info.length != 2)
            {
                return false;
            }

            // get the course descriptor and lecture number
            lec_info[0] = lec_info[0].strip();
            lec_info[1] = lec_info[1].strip();

            // record the course descriptor
            tut.course_descriptor = lec_info[0];
            
            // convert the strings to a number
            int[] buffer = new int[1];
            if(!GetSafeIntFromString(lec_info[1], buffer))
            {
                return false;
            }
            
            // record the lecture number
            tut.lec_num = buffer[0];

            // is this an evening lecture
            if(tut.lec_num == 9)
            {
                tut.is_evng = true;
            }
            else
            {
                tut.is_evng = false;
            }

        }
        else
        {
            // if it does not contain "LEC" then lecture number is 1
            tut.lec_num = 1;
            tut.course_descriptor = elements[0];
        }

        // get the course number and AL
        elements = elements[1].split(",");

        // there should be 2 elements
        if(elements.length != 2)
        {
            return false;
        }

        // convert the strings to a number
        int[] buffer = new int[1];
        if(!GetSafeIntFromString(elements[0], buffer))
        {
            return false;
        }
        
        // record the lecture number
        tut.tut_num = buffer[0];

        if(elements[1].contains("true"))
        {
            tut.is_al = true;
        }
        else
        {
            tut.is_al = false;
        }

        return true;
    }

    /**
     * Parse the input file for the lecture data
     * @param bufferedReader: the file reader to get the lines from
     * @param lec_tut_data: the hashmap of lecture data to put the results in
     * @return: true if parse was successfull, false if errors occured
     */
    private static boolean ParseLectureData(BufferedReader bufferedReader, HashMap<String, HashMap<Integer, LectureData>> lec_tut_data)
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

        // load the lecture data
        while((nextLine != null) && (!nextLine.contains(HEADINGS[4])) )
        {
            // object for holding retreved lecture data
            LectureData temp_lec = new LectureData();
            // try to get the lecture data from the string
            if(TryGetLectureFromLine(nextLine, temp_lec))
            {
                // does this course descriptor already exist or not?
                if(lec_tut_data.containsKey(temp_lec.course_descriptor))
                {
                    // already exists so get the current map of lecturData elements to add to
                    HashMap<Integer, LectureData> temp = lec_tut_data.get(temp_lec.course_descriptor);
                    if(!temp.containsKey(temp_lec.lec_num))
                    {
                        temp.put(temp_lec.lec_num, temp_lec);
                        lec_tut_data.put(temp_lec.course_descriptor, temp);
                    }
                }
                else
                {
                    // does not exist yet, so create a new map for this course descriptor
                    HashMap<Integer, LectureData> temp = new HashMap<Integer, LectureData>();
                    temp.put(temp_lec.lec_num, temp_lec);
                    lec_tut_data.put(temp_lec.course_descriptor, temp);

                }
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
        if(!nextLine.contains(HEADINGS[4]))
        {
            return false;
        }

        return true;
    } 

    /**
     * takes a string and tries to convert it to lecture data
     * @param line: the line to parse for the lecture information
     * @param lec: the lecture data structure to retrun the information in
     * @return: true if the line could be parsed for lecture data, false otherwise
     */
    private static boolean TryGetLectureFromLine(String line, LectureData lec)
    {
        // split the string by ',' deliminator
        String[] elements;

        // split the line on the key word LEC
        if(line.contains("LEC"))
        {
            elements = line.split("LEC");
        }
        else
        {
            return false;
        }


        // there must be 2 elements (course_descriptor, lecture number + AL)
        if(elements.length != 2)
        {
            return false;
        }

        elements[0] = elements[0].strip();
        elements[1] = elements[1].strip();

        // record the course descriptor
        lec.course_descriptor = elements[0];

        // determine if this is a 500 level course
        String[] desc = elements[0].split(" ");
        if(desc.length != 2)
        {
            return false;
        }
        desc[1] = desc[1].strip();

        if(desc[1].charAt(0) == '5')
        {
            lec.is_5xx = true;
        }
        else
        {
            lec.is_5xx = false;
        }

        // get the course number and AL
        elements = elements[1].split(",");
        
        // there should be 2 elements
        if(elements.length != 2)
        {
            return false;
        }

        // convert the strings to a number
        int[] buffer = new int[1];
        if(!GetSafeIntFromString(elements[0], buffer))
        {
            return false;
        }
        
        // record the lecture number
        lec.lec_num = buffer[0];

        // is this an evening lecture
        if(lec.lec_num == 9)
        {
            lec.is_evng = true;
        }
        else
        {
            lec.is_evng = false;
        }

        if(elements[1].contains("true"))
        {
            lec.is_al = true;
        }
        else
        {
            lec.is_al = false;
        }

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
     * Parse the input file for the tutorial slots
     * @param bufferedReader: the file reader to get the lines from
     * @param tutorial_slots: the hashmap of tutorial slots to put the results in
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

        // load the tutorial slots
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
    int id; // the unique id of the lecture 
    String course_descriptor; // e.g. CPSC 433
    boolean is_evng; // is this an evening lecture
    boolean is_5xx; // is this a 500 level course
    int lec_num; // e.g LEC 01 -> lec_num = 1
    boolean is_al; // is this an active learning lecture or not
    ArrayList<TutorialData> tutorials = new ArrayList<TutorialData>(); // the tutorials associated with this lecture

    /**
     * print important information about this lecture 
     */
    public void PrintData()
    {
        System.out.print(String.format("Lecture %s, %d ", course_descriptor, lec_num));
        System.out.print(is_al);
        System.out.print(", ");
        System.out.print(is_evng);
        System.out.print(", ");
        System.out.println(is_5xx);
    }

    /**
     * Convert the Lecture Data to a Lecture
     * NOTE: tutorials must be assigned unique ids first
     * @param id: the unique id to give this lecture
     * @param section: the section that this lecture bellongs to 
     * @return: a lecture data structure
     */
    public Lecture ConvertToLecture(int _id, int section)
    {
        Lecture temp = new Lecture();
        id = _id;
        temp.id = _id;
        temp.is_al = is_al;
        temp.is_5xx = is_5xx;
        temp.is_evng = is_evng;
        temp.section = section;

        temp.tutorials = new Integer[tutorials.size()];
        
        for(int i = 0; i < tutorials.size(); i++)
        {
            temp.tutorials[i] = tutorials.get(i).id;
        }

        temp.course_descriptor = course_descriptor;
        temp.lec_num = lec_num;
        return temp;
    }
}


class TutorialData
{
    int id; // the unique id of the tutorial
    String course_descriptor; // e.g. CPSC 433
    boolean is_evng; // is this an evening lecture
    int lec_num; // e.g. LEC 01 -> lec_num = 1 (defualt to 1 if not included)
    int tut_num; // e.g. TUT/LAB 04 -> tut_num = 4
    boolean is_al; // is this an active learning tutorial or not

    /**
     * print important information about this tutorial
     */
    public void PrintData()
    {
        System.out.print(String.format("\tTutorial %s, %d, %d ", course_descriptor, lec_num, tut_num));
        System.out.print(is_al);
        System.out.print(", ");
        System.out.println(is_evng);
    }

    /**
     * Convert the Tutorial Data to a Tutorial
     * NOTE: tutorials must be assigned unique ids first
     * @param id: the unique id to give this tutorial
     * @return: a lecture data structure
     */
    public Tutorial ConvertToTutorial(int _id)
    {
        Tutorial temp = new Tutorial();
        id = _id;
        temp.id = _id;
        temp.is_al = is_al;
        temp.is_evng = is_evng;

        temp.lec_num = lec_num;
        temp.tut_num = tut_num;
        temp.course_descriptor = course_descriptor;
        return temp;
    }
}
