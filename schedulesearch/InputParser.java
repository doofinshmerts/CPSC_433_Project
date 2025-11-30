package schedulesearch;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
     * @param input_file the input file to parse
     * @param env the environment to return
     * @param s0 the start state to return
     * @return false if there was an error, otherwise true
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
        env.dataset_name = name;

        // Get the Lecture Slots ###########################################################################################
        HashMap<Integer, Slot> lecture_slots = new HashMap<Integer, Slot>();
        if(!ParseLectureSlots(bufferedReader, lecture_slots))
        {
            System.out.println("Could not get lecture slots from file: " + input_file);
            return false;
        }
        env.lecture_slots = lecture_slots;

        // fill the array of lecture slots (used for quick iterating)
        env.lec_slots_array = new Slot[env.lecture_slots.size()];
        
        int k = 0;
        for(Slot slot: env.lecture_slots.values())
        {
            env.lec_slots_array[k] = slot;
            slot.id = k; // who designed this 
            k++;
        }

        // Get the Tutorial Slots #############################################################################################
        HashMap<Integer, Slot> tutorial_slots = new HashMap<Integer, Slot>();
        if(!ParseTutorialSlots(bufferedReader, tutorial_slots))
        {
            System.out.println("Could not get tutorial slots from file: " + input_file);
            return false;
        }
        env.tutorial_slots = tutorial_slots;

        // fill the array of lecture slots (used for quick iterating)
        env.tut_slots_array = new Slot[env.tutorial_slots.size()];
        env.tutslot_lecslot = new int[env.tut_slots_array.length][];

        k = 0;
        for(Slot slot: env.tutorial_slots.values())
        {
            // record the slot in the slot array
            env.tut_slots_array[k] = slot;

            // find the ids of any overlapping lectures if they exist
            int[] temp = slot.TutSlotToOverLappingLecSlots();
            ArrayList<Integer> lec_slots = new ArrayList<Integer>();

            for(int j = 0; j < temp.length; j++)
            {
                // check that this lecture slot exists
                if(env.lecture_slots.containsKey(temp[j]))
                {
                    // add the lecture id to the lecture slots
                    lec_slots.add(env.lecture_slots.get(temp[j]).id);
                }
            }

            // add the overlapping lecture slots to the array
            temp = new int[lec_slots.size()];
            for(int i = 0; i < temp.length; i++)
            {
                temp[i] = lec_slots.get(i);
            }
            env.tutslot_lecslot[k] = temp;

            // assign the slot id and increment the counter
            slot.id = k;
            k++;
        }

        // fill the map from lecture slots to tutorial slots
        ArrayList<int[]> lec_to_tut_slot = new ArrayList<int[]>();

        for(int i = 0; i < env.lec_slots_array.length; i++)
        {
            // get the tutorial hashes of tutorials that would potentially overlap this lecture slot
            int[] temp = env.lec_slots_array[i].LecSlotToOverLappingTutSlots();
            ArrayList<Integer> tut_slots = new ArrayList<Integer>();
            for(int j = 0; j < temp.length; j++)
            {
                // if the slot exists then get its id and add it
                if(env.tutorial_slots.containsKey(temp[j]))
                {
                    tut_slots.add(env.tutorial_slots.get(temp[j]).id);
                }
            }

            // add the overlapping lecture slots to the array
            temp = new int[tut_slots.size()];
            for(int j = 0; j < temp.length; j++)
            {
                temp[j] = tut_slots.get(j);
            }

            // add this array to the array of arrays
            lec_to_tut_slot.add(temp);
        }

        // record this map in the environment 
        env.lecslot_tutslot = new int[lec_to_tut_slot.size()][];
        for(int i = 0; i < env.lecslot_tutslot.length; i++)
        {
            env.lecslot_tutslot[i] = lec_to_tut_slot.get(i);
        }

        // Get the Lectures ######################################################################################################
        // key is the course identifier (name and number), The key for the inner map is the lecture number 
        HashMap<String, HashMap<Integer, LectureData>> lec_tut_data = new HashMap<String, HashMap<Integer, LectureData>>();
        if(!ParseLectureData(bufferedReader, lec_tut_data))
        {
            System.out.println("Could not get the lecture data from file: " + input_file);
            return false;
        }

        int count = 0;
        for(HashMap<Integer, LectureData> elm : lec_tut_data.values())
        {
            count += elm.size();
        }
        env.num_lectures = count;
        
        // Get the tutorials ######################################################################################################
        if(!ParseTutorialData(bufferedReader, lec_tut_data))
        {
            System.out.println("Could not get the tutorial data from file: " + input_file);
            return false;
        }

        // count the total number of tutorials
        count = 0; // tutorial count
        int s_count = 0; // section number
        for(HashMap<Integer, LectureData> elm : lec_tut_data.values())
        {
            for(LectureData elem : elm.values())
            {
                ArrayList<TutorialData> temp = elem.tutorials;
                count += temp.size();   
            }

            // allocate the arrays for the section map to the number of lectures in each section
            int[] sec = new int[elm.size()];
            env.sections.put(s_count, sec);

            s_count++;
        }
        env.num_tutorials = count;

        // Convert TutorialData and lectureData to Tutorials and Lectures #########################################################################################
        
        int t_count = 0; // tutorial id
        int l_count = 0; // lecture id
        s_count = 0; // section id
        
        env.tutorials = new Tutorial[env.num_tutorials];
        env.lectures = new Lecture[env.num_lectures];

        // use for recording the 5xx level lectures
        ArrayList<Integer> lec_5xx = new ArrayList<Integer>();
        
        // create the tutorials and lectures
        for(HashMap<Integer, LectureData> elm : lec_tut_data.values())
        {
            int i = 0; // the counter for the internal lectures within a section
            for(LectureData lec : elm.values())
            {
                // if this is a 5xx level lecture, then record it
                if(lec.is_5xx)
                {
                    lec_5xx.add(l_count);
                }

                ArrayList<TutorialData> temp = lec.tutorials;
                for(TutorialData tut: temp)
                {
                    tut.id = t_count;
                    // convert this Tutorial data to a Tutorial
                    env.tutorials[t_count] = tut.ConvertToTutorial(t_count, s_count, l_count);            
                    // increment the tutorial count
                    t_count++;

                } 
                lec.id = l_count;
                env.sections.get(s_count)[i] = l_count;
                env.lectures[l_count] = lec.ConvertToLecture(l_count, s_count);
                // increment the lecture count
                l_count++;
                i++;
            }

            // increment the section count
            s_count++;
        }

        // this is for creating the lists of tutorials assigned to lectures
        ArrayList<ArrayList<Integer>> tut_map = new ArrayList<ArrayList<Integer>>();

        // initialize the array
        for(int i = 0; i < env.num_lectures; i++)
        {
            // initialize the array at this index
            ArrayList<Integer> new_array = new ArrayList<Integer>();
            tut_map.add(new_array);
        }

        // assign the tutorial parent lectures
        for(int i = 0; i < env.tutorials.length; i++)
        {
            
            // if the section is not -1 then add all lectures in the section as parent lectures
            if(env.tutorials[i].section != -1)
            {
                // this tutorial bellongs to a section so add all tutorials in its section
                int[] temp = env.sections.get(env.tutorials[i].section);
                env.tutorials[i].parent_lectures = temp;

                // add this tutorial to the array for its parent lecture
                for(int j = 0; j < temp.length; j++)
                {
                    // get the lecture at index j and add the tutorial id 'i' to its list of children
                    tut_map.get(temp[j]).add(i);
                }
            }
            else
            {
                // if not then add this tutorial to the list of its only parent
                tut_map.get(env.tutorials[i].parent_lectures[0]).add(i);
            }
        }

        // add the tutorials to the lectures
        for(int i = 0; i < env.num_lectures; i++)
        {
            // the array for holding the list of tutorials that bellong to this lecture
            int[] temp = new int[tut_map.get(i).size()];
            for(int j = 0; j < temp.length; j++)
            {
                temp[j] = tut_map.get(i).get(j);
            }
            // assign this map to the lecture 
            env.lectures[i].tutorials = temp;
        }

        // put the array of 5xx level lectures in the environment
        int[] lectures_5xx = new int[lec_5xx.size()];
        for(int i = 0; i < lectures_5xx.length; i++)
        {
            lectures_5xx[i] = lec_5xx.get(i);
        }
        env.lectures_5xx = lectures_5xx;

        // Parse Not Compatible ##################################################################################################################
        if(!ParseNotCompatible(bufferedReader, env.lectures, env.tutorials, lec_tut_data))
        {
            System.out.println("Could not get not compatible data from file: " + input_file);
            return false;
        }

        // Parse Unwanted ################################################################################################################################
        if(!ParseUnwanted(bufferedReader, env.lectures, env.tutorials, lec_tut_data, env.tutorial_slots, env.lecture_slots))
        {
            System.out.println("Could not get unwanted data from file: " + input_file);
            return false;
        }

        // Parse Preferences #################################################################################################################################
        if(!ParsePreferences(bufferedReader, env.lectures, env.tutorials, lec_tut_data, env.lecture_slots, env.tutorial_slots))
        {
            System.out.println("Could not get preferences data from file: " + input_file);
            return false;
        }

        // Parse Pair #################################################################################################################################################
        ArrayList<Pair> pairs = new ArrayList<Pair>();
        if(!ParsePairs(bufferedReader, lec_tut_data, pairs))
        {
            System.out.println("Could not get preferences data from file: " + input_file);
            return false;
        }
        
        // convert the array list to an array
        env.pairs = new Pair[pairs.size()];
        for(int i = 0; i < pairs.size(); i++)
        {
            env.pairs[i] = pairs.get(i);
        }

        // Parse Partial Assignments ######################################################################################################################################
        ArrayList<UnwantedPair> part_assign_lec = new ArrayList<UnwantedPair>();
        ArrayList<UnwantedPair> part_assign_tut = new ArrayList<UnwantedPair>();
        if(!ParsePartialAssignments(bufferedReader, env.lecture_slots, env.tutorial_slots, lec_tut_data, part_assign_tut, part_assign_lec))
        {
            System.out.println("Could not get unwanted data from file: " + input_file);
            return false;
        }

        // environment must be setup inorder to work
        env.SetupEnvironment();
        // Print the results of the parse 
        PrintParseResults(env, part_assign_lec, part_assign_tut);

        // Add the special constraints #########################################################################################################################################
        // remove any lecture slots that overlap tuesdays at 11:00 to 12:30
        // if exists add partial constraint CPSC 851 to TU 18:00
        // if exists add partial constraint CPSC 913 to TU 18:00
        // if exists add unwanted for any CPSC 351 to time overlapping 18:00 to 19:00
        // if exists add unwanted for any CPSC 413 to time overlapping 18:00 to 19:00
        
        // apply the partial assignments to the starting state #################################################################################################################
        
        // create an initial problem
        s0.SetupProblem(env.num_lectures, env.num_tutorials, env.lec_slots_array.length, env.tut_slots_array.length);

        // assign the partial assignments for the lectures
        for(UnwantedPair pair : part_assign_lec)
        {
            System.out.println("\nAssigning lecture: " + pair.id + ", to slot: " + pair.slot_id);
            // get the valid slots for this lecture assignments
            int[] valid_slots = Functions.ValidLectureSlots(env, pair.id, s0);
            if(valid_slots == null)
            {
                System.out.println("Invalid partial assignment: assigning lecture: " + pair.id + ", to slot: " + pair.slot_id);
                return false; 
            }
            Functions.PrintLectureSlots(valid_slots, env); 

            // ensure that the slot exists in the array of valid slots
            boolean found_slot = false;
            for(int i = 0; i < valid_slots.length; i++)
            {
                if(valid_slots[i] == pair.slot_id)
                {
                    s0.AssignLecture(pair.id, pair.slot_id, env.lectures[pair.id].is_al);
                    found_slot = true;
                }
            }

            if(found_slot == false)
            {
                System.out.println("Invalid partial assignment: assigning lecture: " + pair.id + ", to slot: " + pair.slot_id);
                return false; 
            }
        }

        // assign the partial assignments for the tutorials
        for(UnwantedPair pair : part_assign_tut)
        {
            System.out.println("\nAssigning tutorial: " + pair.id + ", to slot: " + pair.slot_id);
            // get the valid slots for this lecture assignments
            int[] valid_slots = Functions.ValidTutSlots(env, pair.id, s0);
            // check to see if any slots were returned
            if(valid_slots == null)
            {
                System.out.println("Invalid partial assignment: assigning tutorial: " + pair.id + ", to slot: " + pair.slot_id);
                return false; 
            }
            Functions.PrintTutorialSlots(valid_slots, env); 

            // ensure that the slot exists in the array of valid slots
            boolean found_slot = false;
            for(int i = 0; i < valid_slots.length; i++)
            {
                if(valid_slots[i] == pair.slot_id)
                {
                    s0.AssignTutorial(pair.id, pair.slot_id, env.tutorials[pair.id].is_al);
                    found_slot = true;
                }
            }

            if(found_slot == false)
            {
                System.out.println("Invalid partial assignment: assigning tutorial: " + pair.id + ", to slot: " + pair.slot_id);
                return false;     
            }
        }

        // print the current form of the problem
        System.out.println("Initial problem after partial assignments");
        Functions.PrintProblem(s0, env);
        
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
     * prints all the information obtained from parsing the file
     * @param env the environment information
     * @param part_assign_lec the partial assignments of lectures
     * @param part_assign_tut the partial assignments of tutorials
     */
    private static void PrintParseResults(Environment env, ArrayList<UnwantedPair> part_assign_lec, ArrayList<UnwantedPair> part_assign_tut)
    {
        // print the name of the data set
        System.out.println("dataset name: " + env.dataset_name);

        System.out.println(String.format("w_minfilled: %d\nw_pref: %d\nw_pair: %d\nw_secdiff: %d\npen_lecturemin: %d\npen_tutorialmin: %d\npen_notpaired: %d\npen_section: %d\nmax_iterations: %d\ntime_limit: %d\n",
            env.w_minfilled,
            env.w_pref,
            env.w_pair,
            env.w_secdiff,
            env.pen_lecturemin,
            env.pen_tutorialmin,
            env.pen_notpaired,
            env.pen_section,
            env.max_iterations,
            env.time_limit
        ));
        
        // print the lecture slots
        System.out.println("\nLecture slots ############################################################\nsize: " + env.lecture_slots.size());
        for(int i = 0; i < env.lec_slots_array.length; i++)
        {
            env.lec_slots_array[i].PrintSlot();
        }

        // print the tutorial slots
        System.out.println("\nTutorial slots ###########################################################\nsize: " + env.tutorial_slots.size());
        for(int i = 0; i < env.tut_slots_array.length; i++)
        {
            env.tut_slots_array[i].PrintSlot();
        }

        // print map of tutorial slots to lecture slots
        System.out.println("\nMap of Tutorial slots to lecture slots ###################################\n");
        for(int i = 0; i < env.tutslot_lecslot.length; i++)
        {
            System.out.println("tutorial id: " + i);
            for(int j = 0; j < env.tutslot_lecslot[i].length; j++)
            {
                System.out.println("\tlec id: " + env.tutslot_lecslot[i][j]);
            }
        }

        //print map of lecture slots to tutorial slots
        System.out.println("\nMap of Lecture Slots to Tutorial Slots ####################################\n");
        for(int i = 0; i < env.lecslot_tutslot.length; i++)
        {
            System.out.println("lecture id: " + i);
            for(int j = 0; j < env.lecslot_tutslot[i].length; j++)
            {
                System.out.println("\ttut id: " + env.lecslot_tutslot[i][j]);
            }
        }

        // Print the Lectures and Tutorials
        System.out.println("\nLecture and Tutorial Data ##################################################\n");
        // set parent lecture number in each tutorial for backwards lookup
        for(int i = 0; i < env.num_lectures; i++)
        {
            // get the ids of the tutorials associated with this lecture
            int[] tuts = env.lectures[i].tutorials;
            env.lectures[i].PrintData();

            for(int j = 0; j < tuts.length; j++)
            {
                System.out.print("\t");
                env.tutorials[tuts[j]].PrintData();    
            }
            System.out.println("");
        }

        // print the sections map
        System.out.println("\nSections ###################################################################\n");
        int j = 0;
        for(int[] elm: env.sections.values())
        {
            System.out.println("Section: " + j);
            j++;
            for(int i = 0; i < elm.length; i++)
            {
                System.out.println("\tId: " + elm[i]);
            }
        }

        // print the 5xx lectures
        System.out.println("\nLectures 5XX ###############################################################\n");
        for(int i = 0; i < env.lectures_5xx.length; i++)
        {
            env.lectures[env.lectures_5xx[i]].PrintData();
        }

        // print the not compatible assignments
        System.out.println("\nNot compatible data from Lectures ###########################################\n");
        // set parent lecture number in each tutorial for backwards lookup
        for(int i = 0; i < env.num_lectures; i++)
        {
            // get the ids of the tutorials associated with this lecture
            env.lectures[i].PrintData();

            for(Integer id : env.lectures[i].not_compatible_lec)
            {
                System.out.print("\t");
                env.lectures[id].PrintData();
            }

            for(Integer id : env.lectures[i].not_compatible_tut)
            {
                System.out.print("\t");
                env.tutorials[id].PrintData();
            }

            System.out.println("");
            
        }
        // print the not compatible assignments
        System.out.println("\nNot compatible data from Tutorials ############################################\n");
        // set parent lecture number in each tutorial for backwards lookup
        for(int i = 0; i < env.num_tutorials; i++)
        {
            // get the ids of the tutorials associated with this lecture
            env.tutorials[i].PrintData();

            for(Integer id : env.tutorials[i].not_compatible_lec)
            {
                System.out.print("\t");
                env.lectures[id].PrintData();
            }

            for(Integer id : env.tutorials[i].not_compatible_tut)
            {
                System.out.print("\t");
                env.tutorials[id].PrintData();
            }

            System.out.println("");
            
        }

        // print Unwanted
        System.out.println("\nUnwanted ########################################################################\n");
        for(int i = 0; i < env.num_lectures; i++)
        {
            env.lectures[i].PrintData();

            for(Integer id : env.lectures[i].unwanted)
            {
                System.out.print("\t");
                env.lec_slots_array[id].PrintSlot();
            }
            System.out.println("");
        }

        for(int i = 0; i < env.num_tutorials; i++)
        {
            env.tutorials[i].PrintData();

            for(Integer id : env.tutorials[i].unwanted)
            {
                System.out.print("\t");
                env.tut_slots_array[id].PrintSlot();
            }
            System.out.println("");
        }

        // print preferences for lectures
        System.out.println("\nPreferences #######################################################################\n");
        for(int i = 0; i < env.num_lectures; i++)
        {
            env.lectures[i].PrintData();

            for(Map.Entry<Integer, Integer> entry : env.lectures[i].preferences.entrySet())
            {
                System.out.print("\tvalue: " + entry.getValue() + ", ");
                env.lec_slots_array[entry.getKey()].PrintSlot();
            }
            System.out.println("");
        }

        // print preferences for tutorials
        for(int i = 0; i < env.num_tutorials; i++)
        {
            env.tutorials[i].PrintData();

            for(Map.Entry<Integer, Integer> entry : env.tutorials[i].preferences.entrySet())
            {
                System.out.print("\tvalue: " + entry.getValue() + ", ");
                env.tut_slots_array[entry.getKey()].PrintSlot();
            }
            System.out.println("");
        }

        // print the pairs
        System.out.println("\nPairs ###############################################################################");
        for(int i = 0; i < env.pairs.length; i++)
        {
            Pair p = env.pairs[i];

            System.out.println("");
            if(p.is_lec1)
            {
                env.lectures[p.id1].PrintData();
            }
            else
            {
                env.tutorials[p.id1].PrintData();
            }

            if(p.is_lec2)
            {
                env.lectures[p.id2].PrintData();
            }
            else
            {
                env.tutorials[p.id2].PrintData();
            }
        }

        
        // print the some basic indexing checks
        System.out.println("\nChecks ###############################################################################");

        System.out.println("indicies check");
        for(int i = 0; i < env.lectures.length; i++)
        {
            // this is important for checking that this system is working
            System.out.println(String.format("index: %d id: %d",i, env.lectures[i].id));
        }

        for(int i = 0; i < env.tutorials.length; i++)
        {
            System.out.println(String.format("index: %d id: %d", i, env.tutorials[i].id));
        }

        System.out.println(String.format("\nPreference sum: %d", env.total_pref_sum));

        System.out.println("\n equivalent lectrue slots\n");
        for(int i = 0; i < env.tutid_to_lecid.length; i++)
        {
            System.out.println(String.format("tut id: %d, lec id: %d", i, env.tutid_to_lecid[i]));
        }


        // print the constraint sorting order
        System.out.println("\nConstraint sorting order##################################################################\n");

        for(int i = 0; i< env.constraint_ordering.length; i++)
        {
            if(env.constraint_ordering[i].is_lec)
            {
                env.lectures[env.constraint_ordering[i].id].PrintData();
            }
            else
            {
                env.tutorials[env.constraint_ordering[i].id].PrintData();
            }
        }

        // print the partial assignments
        System.out.println("\nPartial assignments ##################################################################");

        for(int i = 0; i < part_assign_lec.size(); i++)
        {
            System.out.println("");
            UnwantedPair p = part_assign_lec.get(i);
            env.lectures[p.id].PrintData();
            env.lec_slots_array[p.slot_id].PrintSlot();
        }


        for(int i = 0; i < part_assign_tut.size(); i++)
        {
            System.out.println("");
            UnwantedPair p = part_assign_tut.get(i);
            env.tutorials[p.id].PrintData();
            env.tut_slots_array[p.slot_id].PrintSlot();
        }
    }


    
    /**
     * Parse for the unwanted slots
     * @param bufferedReader the file to read the data from
     * @param lecture_slots the map of ids to lecture slots
     * @param tutorial_slots the map of ids to tutorial slots
     * @param lec_tut_data the map of lectures and tutorials to ids
     * @param part_assign_tut the array of partial tutorial assignemnts
     * @param part_assign_lec the array of partial lecture assignments
     * @return: true if no errors occured while parsing the data, false otherwise
     */
    private static boolean ParsePartialAssignments(BufferedReader bufferedReader,
                                        HashMap<Integer, Slot> lecture_slots,
                                        HashMap<Integer, Slot> tutorial_slots,  
                                        HashMap<String, HashMap<Integer, LectureData>> lec_tut_data, 
                                        ArrayList<UnwantedPair> part_assign_tut,
                                        ArrayList<UnwantedPair> part_assign_lec)
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
        while(nextLine != null)
        {
            // object for holding retreved lecture data
            UnwantedPair pair = new UnwantedPair();
            // try to get the lecture data from the string
            if(TryGetUnwantedPairFromLine(nextLine,lec_tut_data, pair))
            {
                if(pair.is_lec)
                {
                    // check that the slot exists
                    if(lecture_slots.containsKey(pair.slot_id))
                    {
                        // set the slot id to the actual id
                        pair.slot_id = lecture_slots.get(pair.slot_id).id;
                        part_assign_lec.add(pair);
                    }
                    else
                    {
                        System.out.println("INPUT WARNING: (Partial Assignments) invalid slot in line: " + nextLine);
                    }
                }
                else
                {
                    // check that the slot exits
                    if(tutorial_slots.containsKey(pair.slot_id))
                    {
                        // set the slot id to the actuall id
                        pair.slot_id = tutorial_slots.get(pair.slot_id).id;
                        part_assign_tut.add(pair);
                    }
                    else
                    {
                        System.out.println("INPUT WARNING: (Partial Assignments) invalid slot in line: " + nextLine);
                    }
                }
            }
            else
            {
                System.out.println("INPUT WARNING: (Partial Assignments) invalid information in line: " + nextLine);
            }

            // read the next line from the file
            try{
                nextLine = bufferedReader.readLine();
            }catch (IOException e)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Parse the input data for pairs 
     * @param bufferedReader the file to read the data from
     * @param lectures the array of lectures to use
     * @param tutorials the array of tutorials to use
     * @param pairs the set of pairs of lecture/tutorials to return
     * @return true if no errors occured while parsing the data, false otherwise
     */ 
    private static boolean ParsePairs(BufferedReader bufferedReader, HashMap<String, HashMap<Integer, LectureData>> lec_tut_data, ArrayList<Pair> pairs)
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
        while((nextLine != null) && (!nextLine.contains(HEADINGS[9])) )
        {
            // object for holding retreved lecture data
            Pair pair = new Pair();
            // try to get the lecture data from the string
            if(TryGetPairFromLine(nextLine, lec_tut_data, pair))
            {
                pairs.add(pair);
            }
            else
            {
                System.out.println("INPUT WARNING: (Pairs) invalid information in line: " + nextLine);
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
        if(!nextLine.contains(HEADINGS[9]))
        {
            return false;
        }

        return true;
    }

    /**
     * Parse for the preferences
     * @param bufferedReader the file to read the data from
     * @param lectures the array of lectures to use
     * @param tutorials the array of tutorials to use
     * @param lec_tut_data the map of lectures and tutorials to ids
     * @param lec_slots the map of lecture hashes to lecture slots
     * @param tut_slots the map of tutorial hashes to tutorial slots
     * @return true if no errors occured while parsing the data, false otherwise 
     */
    private static boolean ParsePreferences(BufferedReader bufferedReader, 
                                        Lecture[] lectures, 
                                        Tutorial[] tutorials, 
                                        HashMap<String, HashMap<Integer, LectureData>> lec_tut_data, 
                                        HashMap<Integer, Slot> lec_slots,
                                        HashMap<Integer, Slot> tut_slots)
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
        while((nextLine != null) && (!nextLine.contains(HEADINGS[8])) )
        {
            // object for holding retreved lecture data
            Preference pref = new Preference();
            // try to get the lecture data from the string
            if(TryGetPreferenceFromLine(nextLine,lec_tut_data, pref))
            {
                if(pref.is_lec)
                {
                    // ensure that the slot exists
                    if(lec_slots.containsKey(pref.slot_id))
                    {
                        // get the true id
                        pref.slot_id = lec_slots.get(pref.slot_id).id;
                        // add the slot and score to the preferences
                        lectures[pref.id].preferences.put(pref.slot_id, pref.value);
                    }
                    else
                    {
                        System.out.println("INPUT WARNING: (Preferences) invalid slot in line: " + nextLine);
                    }
                }
                else
                {
                    // ensure that the slot exists
                    if(tut_slots.containsKey(pref.slot_id))
                    {
                        // get the true id
                        pref.slot_id = tut_slots.get(pref.slot_id).id;
                        // add the slot to the unwanted set
                        tutorials[pref.id].preferences.put(pref.slot_id, pref.value);
                    }
                    else
                    {
                        System.out.println("INPUT WARNING: (Preferences) invalid slot in line: " + nextLine);
                    }
                }
            }
            else
            {
                System.out.println("INPUT WARNING: (Preferences) invalid information in line: " + nextLine);
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
        if(!nextLine.contains(HEADINGS[8]))
        {
            return false;
        }

        return true;
    }

    /**
     * Parse the line for a lecture/tutorial, time slot, and preference value
     * @param nectline the line to parse for the data
     * @param lec_tut_data the map of lectures and tutorials to ids
     * @param pref the data structure to return the information in
     * @retrun true if a data was found, false otherwise
     */ 
    private static boolean TryGetPreferenceFromLine(String nextLine, HashMap<String, HashMap<Integer, LectureData>> lec_tut_data, Preference pref)
    {
        // split the string by ',' deliminator
        String[] elements = nextLine.split(",");
        
        // there must be 4 elements (day,time,lecture/tutorial,value)
        if(elements.length != 4)
        {
            return false;
        }
        
        elements[0] = elements[0].strip();
        elements[1] = elements[1].strip();
        elements[2] = elements[2].strip();
        elements[3] = elements[3].strip();

        // for storing the slot data
        Slot slot = new Slot();
        
        // get the day
        switch(elements[0])
        {
            case "MO":
                slot.day = 0;
                break;
            case "TU":
                slot.day = 1;
                break;
            case "WE":
                slot.day = 2;
                break;
            case "TR":
                slot.day = 3;
                break;
            case "FR":
                slot.day = 4;
                break;
            default:
                // if the day is not found then return false
                return false;
        }
        
        // get the time
        String[] hr_min = elements[1].split(":");
        // there should be 2 elements
        if(hr_min.length != 2)
        {
            return false;
        }
        hr_min[0] = hr_min[0].strip();
        hr_min[1] = hr_min[1].strip();
        
        // convert the strings to numbers
        int[] buffer = new int[1];
        if(!GetSafeIntFromString(hr_min[0], buffer))
        {
            return false;
        }
                
        // record the hour
        slot.hour = buffer[0];
        if(!GetSafeIntFromString(hr_min[1], buffer))
        {
            return false;
        }
        // record the minute
        slot.minute = buffer[0];

        // setup the slot
        slot.SetupSlot();

        // get the value
        if(!GetSafeIntFromString(elements[3], buffer))
        {
            return false;
        }
        pref.value = buffer[0];

        // check to see if this is a lecture or tutorial
        pref.is_lec = !((elements[2].contains("TUT")) || (elements[2].contains("LAB")) );

        if(pref.is_lec)
        {
            LectureData temp1 = new LectureData();
            
            // parse the strings for the data 
            if(!TryGetLectureBasicFromLine(elements[2], temp1))
            {
                return false;
            }

            // get the slot id (lecture hash for lecture slot)
            pref.slot_id = slot.lec_hash;

            // get the id of the first element
            if(lec_tut_data.containsKey(temp1.course_descriptor))
            {
                // already exists so get the current map of lecturData elements to get the id
                HashMap<Integer, LectureData> temp = lec_tut_data.get(temp1.course_descriptor);
                if(temp.containsKey(temp1.lec_num))
                {
                    // get the id 
                    pref.id = temp.get(temp1.lec_num).id;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
        else
        {
            TutorialData temp1 = new TutorialData();

            // parse the strings for the data 
            if(!TryGetTutorialBasicFromLine(elements[2], temp1))
            {
                return false;
            }

            // get the slot id (tutorial hash for tutorial slot)
            pref.slot_id = slot.tut_hash;

            // get the id of the second element
            if(lec_tut_data.containsKey(temp1.course_descriptor))
            {
        
                // already exists so get the current map of lecturData elements to get the id
                HashMap<Integer, LectureData> temp = lec_tut_data.get(temp1.course_descriptor);
                if(temp.containsKey(temp1.lec_num))
                {
            
                    // get the array of tutorials
                    ArrayList<TutorialData> tuts = temp.get(temp1.lec_num).tutorials;

                    // find the tutorial with the same tutorial number and record its number
                    for(TutorialData tut: tuts)
                    {
                        pref.id = -1;
                        if(tut.tut_num == temp1.tut_num)
                        {
                            pref.id = tut.id;
                            break;
                        }
                    }
                    // make sure that the id was found
                    if(pref.id == -1)
                    {
                        return false;
                    }
                }
                else
                {
                    return false;
                }
            }
            else
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Parse for the unwanted slots
     * @param bufferedReader the file to read the data from
     * @param lectures the array of lectures to use
     * @param tutorials the array of tutorials to use
     * @param lec_tut_data the map of lectures and tutorials to ids
     * @param tutorial_slots the map of ids to tutorial slots
     * @param lecture_slots the map of ids to lecture slots
     * @return true if no errors occured while parsing the data, false otherwise
     */
    private static boolean ParseUnwanted(BufferedReader bufferedReader, 
                                        Lecture[] lectures, 
                                        Tutorial[] tutorials, 
                                        HashMap<String, HashMap<Integer, LectureData>> lec_tut_data, 
                                        HashMap<Integer, Slot> tutorial_slots,
                                        HashMap<Integer, Slot> lecture_slots)
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
        while((nextLine != null) && (!nextLine.contains(HEADINGS[7])) )
        {
            // object for holding retreved lecture data
            UnwantedPair pair = new UnwantedPair();
            // try to get the lecture data from the string
            if(TryGetUnwantedPairFromLine(nextLine,lec_tut_data, pair))
            {
                if(pair.is_lec)
                {
                    // ensure that the slot exists
                    if(lecture_slots.containsKey(pair.slot_id))
                    {
                        // add the slot to the unwanted set
                        lectures[pair.id].unwanted.add(lecture_slots.get(pair.slot_id).id);
                    }
                    else
                    {
                        System.out.println("INPUT WARNING: (Unwanted) invalid slot in line: " + nextLine);
                    }
                }
                else
                {
                    // ensure that the slot exists
                    if(tutorial_slots.containsKey(pair.slot_id))
                    {
                        // add the slot to the unwanted set
                        tutorials[pair.id].unwanted.add(tutorial_slots.get(pair.slot_id).id);
                    }
                    else
                    {
                        System.out.println("INPUT WARNING: (Unwanted) invalid slot in line: " + nextLine);
                    }
                }
            }
            else
            {
                System.out.println("INPUT WARNING: (Unwanted) invalid information in line: " + nextLine);
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
        if(!nextLine.contains(HEADINGS[7]))
        {
            return false;
        }

        return true;
    }

    /**
     * Parse the line for a lecture/tutorial and a time slot
     * @param nectline the line to parse for the data
     * @param lec_tut_data the map of lectures and tutorials to ids
     * @param pair the pair data structure to return the information in
     * @retrun true if a pair was found, false otherwise
     */ 
    private static boolean TryGetUnwantedPairFromLine(String nextLine, HashMap<String, HashMap<Integer, LectureData>> lec_tut_data, UnwantedPair pair)
    {
        // try spliting on a ","
        String[] elements = nextLine.split(",", 2);

        // ensure that there are two elements
        if(elements.length != 2)
        {
            return false;
        }

        // check to see if this is a lecture or tutorial
        pair.is_lec = !((elements[0].contains("TUT")) || (elements[0].contains("LAB")) );

        // get the information for the slot
        Slot temp2 = new Slot();
        if(!TryGetBasicSlotFromLine(elements[1], temp2))
        {
            return false;
        }


        if(pair.is_lec)
        {
            LectureData temp1 = new LectureData();
            
            // parse the strings for the data 
            if(!TryGetLectureBasicFromLine(elements[0], temp1))
            {
                return false;
            }

            // get the slot id (lecture hash for lecture slot)
            pair.slot_id = temp2.lec_hash;

            // get the id of the first element
            if(lec_tut_data.containsKey(temp1.course_descriptor))
            {
                // already exists so get the current map of lecturData elements to get the id
                HashMap<Integer, LectureData> temp = lec_tut_data.get(temp1.course_descriptor);
                if(temp.containsKey(temp1.lec_num))
                {
                    // get the id 
                    pair.id = temp.get(temp1.lec_num).id;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
        else
        {
            TutorialData temp1 = new TutorialData();

            // parse the strings for the data 
            if(!TryGetTutorialBasicFromLine(elements[0], temp1))
            {
                return false;
            }

            // get the slot id (tutorial hash for tutorial slot)
            pair.slot_id = temp2.tut_hash;

            // get the id of the second element
            if(lec_tut_data.containsKey(temp1.course_descriptor))
            {
                // already exists so get the current map of lecturData elements to get the id
                HashMap<Integer, LectureData> temp = lec_tut_data.get(temp1.course_descriptor);
                if(temp.containsKey(temp1.lec_num))
                {
                    // get the array of tutorials
                    ArrayList<TutorialData> tuts = temp.get(temp1.lec_num).tutorials;

                    // find the tutorial with the same tutorial number and record its number
                    for(TutorialData tut: tuts)
                    {
                        pair.id = -1;
                        if(tut.tut_num == temp1.tut_num)
                        {
                            pair.id = tut.id;
                            break;
                        }
                    }
                    // make sure that the id was found
                    if(pair.id == -1)
                    {
                        return false;
                    }
                }
                else
                {
                    return false;
                }
            }
            else
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Parse the input file for the not compatible data
     * @param bufferedReader the file to read the data from
     * @param lectures the array of lectures to use
     * @param tutorials the array of tutorials to use
     * @param lec_tut_data the map between lectures and tutorials needed for the not compatible assignments
     * @return true if no errors occured while parsing the data, false otherwise
     */ 
    private static boolean ParseNotCompatible(BufferedReader bufferedReader, Lecture[] lectures,Tutorial[] tutorials, HashMap<String, HashMap<Integer, LectureData>> lec_tut_data)
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
        while((nextLine != null) && (!nextLine.contains(HEADINGS[6])) )
        {
            // object for holding retreved lecture data
            Pair pair = new Pair();
            // try to get the lecture data from the string
            if(TryGetPairFromLine(nextLine, lec_tut_data, pair))
            {
                if(pair.is_lec1)
                {
                    if(pair.is_lec2)
                    {
                        // 1:lecture, 2:lecture
                        lectures[pair.id1].not_compatible_lec.add(pair.id2);
                        lectures[pair.id2].not_compatible_lec.add(pair.id1);
                    }
                    else
                    {   
                        // 1:lecture, 2:tutorial
                        lectures[pair.id1].not_compatible_tut.add(pair.id2);
                        tutorials[pair.id2].not_compatible_lec.add(pair.id1);
                    }
                }
                else
                {
                    if(pair.is_lec2)
                    {
                        // 1:tutorial, 2:lecture
                        tutorials[pair.id1].not_compatible_lec.add(pair.id2);
                        lectures[pair.id2].not_compatible_tut.add(pair.id1);
                    }
                    else
                    {
                        // 1:tutorial, 2:tutorial
                        tutorials[pair.id1].not_compatible_tut.add(pair.id2);
                        tutorials[pair.id2].not_compatible_tut.add(pair.id1);
                    }
                }
            }
            else
            {
                System.out.println("INPUT WARNING: (Not Compatible) invalid information in line: " + nextLine);
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
        if(!nextLine.contains(HEADINGS[6]))
        {
            return false;
        }

        return true;
    }

    /**
     * Try to get the ids of the two lectures/tutorials in the given line
     * @param nextLine the string to parse for the pair of ids
     * @param lec_tut_data the map from tutorials and lectures to ids
     * @param pair the structure for returning the data in
     * @retrun true if a pair was found, false if not
     */
    private static boolean TryGetPairFromLine(String nextLine, HashMap<String, HashMap<Integer, LectureData>> lec_tut_data, Pair pair)
    {
        // try spliting the string on a comma
        String[] elements = nextLine.split(",");

        if(elements.length != 2)
        {
            return false;
        }
        // determine whether the elements are lectures or tutorials
        pair.is_lec1 = !(elements[0].contains("TUT") || elements[0].contains("LAB"));
        pair.is_lec2 = !(elements[1].contains("TUT") || elements[1].contains("LAB"));

        // check the type of each 
        if(pair.is_lec1)
        {
            if(pair.is_lec2)
            {
                if(!ParseForLecLecPair(elements[0], elements[1], pair, lec_tut_data))
                {
                    return false;
                }
            }
            else
            {   
                if(!ParseForLecTutPair(elements[0], elements[1], pair, lec_tut_data))
                {
                    return false;
                }
            }
        }
        else
        {
            if(pair.is_lec2)
            {
                if(!ParseForTutLecPair(elements[0], elements[1], pair, lec_tut_data))
                {
                    return false;
                }
            }
            else
            {
                if(!ParseForTutTutPair(elements[0], elements[1], pair, lec_tut_data))
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Parse line1 and line2 for a pair of lecture ids
     * @param line1 the first line to parse
     * @param line2 the second line to parse
     * @param lec_tut_data the map from lectures and tutorials to ids
     * @retrun true if a pair could be found, false if not
     */
    private static boolean ParseForLecLecPair(String line1, String line2, Pair pair, HashMap<String, HashMap<Integer, LectureData>> lec_tut_data)
    {
        // 1:lecture, 2:lecture
        LectureData temp1 = new LectureData();
        LectureData temp2 = new LectureData();

        // parse the strings for the data 
        if(!TryGetLectureBasicFromLine(line1, temp1))
        {
            return false;
        }

        if(!TryGetLectureBasicFromLine(line2, temp2))
        {
            return false;
        }

        // get the id of the first element
        if(lec_tut_data.containsKey(temp1.course_descriptor))
        {
            // already exists so get the current map of lecturData elements to get the id
            HashMap<Integer, LectureData> temp = lec_tut_data.get(temp1.course_descriptor);
            if(temp.containsKey(temp1.lec_num))
            {
                // get the id 
                pair.id1 = temp.get(temp1.lec_num).id;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }

        // get the id of the second element
        if(lec_tut_data.containsKey(temp2.course_descriptor))
        {
            // already exists so get the current map of lecturData elements to get the id
            HashMap<Integer, LectureData> temp = lec_tut_data.get(temp2.course_descriptor);
            if(temp.containsKey(temp2.lec_num))
            {
                // get the id 
                pair.id2 = temp.get(temp2.lec_num).id;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }

        return true;
    }

    /**
     * Parse line1 and line2 for a lec tut pair ids
     * @param line1 the first line to parse
     * @param line2 the second line to parse
     * @param lec_tut_data the map from lectures and tutorials to ids
     * @retrun true if a pair could be found, false if not
     */
    private static boolean ParseForLecTutPair(String line1, String line2, Pair pair, HashMap<String, HashMap<Integer, LectureData>> lec_tut_data)
    {
        // 1:lecture, 2:lecture
        LectureData temp1 = new LectureData();
        TutorialData temp2 = new TutorialData();

        // parse the strings for the data 
        if(!TryGetLectureBasicFromLine(line1, temp1))
        {
            return false;
        }
        
        if(!TryGetTutorialBasicFromLine(line2, temp2))
        {
            return false;
        }

        // get the id of the first element
        if(lec_tut_data.containsKey(temp1.course_descriptor))
        {
            // already exists so get the current map of lecturData elements to get the id
            HashMap<Integer, LectureData> temp = lec_tut_data.get(temp1.course_descriptor);
            if(temp.containsKey(temp1.lec_num))
            {
               // get the id 
               pair.id1 = temp.get(temp1.lec_num).id;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }

        // get the id of the second element
        if(lec_tut_data.containsKey(temp2.course_descriptor))
        {
            // already exists so get the current map of lecturData elements to get the id
            HashMap<Integer, LectureData> temp = lec_tut_data.get(temp2.course_descriptor);
            if(temp.containsKey(temp2.lec_num))
            {
                // get the array of tutorials
                ArrayList<TutorialData> tuts = temp.get(temp2.lec_num).tutorials;

                // find the tutorial with the same tutorial number and record its number
                for(TutorialData tut: tuts)
                {
                    pair.id2 = -1;
                    if(tut.tut_num == temp2.tut_num)
                    {
                        pair.id2 = tut.id;
                        break;
                    }
                }
                // make sure that the id was found
                if(pair.id2 == -1)
                {
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }

        return true;
    }

    /**
     * Parse line1 and line2 for a tut lec pair ids
     * @param line1 the first line to parse
     * @param line2 the second line to parse
     * @param lec_tut_data the map from lectures and tutorials to ids
     * @retrun true if a pair could be found, false if not
     */
    private static boolean ParseForTutLecPair(String line1, String line2, Pair pair, HashMap<String, HashMap<Integer, LectureData>> lec_tut_data)
    {
        // 1:lecture, 2:lecture
        LectureData temp2 = new LectureData();
        TutorialData temp1 = new TutorialData();

        // parse the strings for the data 
        if(!TryGetLectureBasicFromLine(line2, temp2))
        {
            return false;
        }

        if(!TryGetTutorialBasicFromLine(line1, temp1))
        {
            return false;
        }

        // get the id of the first element
        if(lec_tut_data.containsKey(temp2.course_descriptor))
        {
            // already exists so get the current map of lecturData elements to get the id
            HashMap<Integer, LectureData> temp = lec_tut_data.get(temp2.course_descriptor);
            if(temp.containsKey(temp2.lec_num))
            {
               // get the id 
               pair.id2 = temp.get(temp2.lec_num).id;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }

        // get the id of the second element
        if(lec_tut_data.containsKey(temp1.course_descriptor))
        {
            // already exists so get the current map of lecturData elements to get the id
            HashMap<Integer, LectureData> temp = lec_tut_data.get(temp1.course_descriptor);
            if(temp.containsKey(temp1.lec_num))
            {
                // get the array of tutorials
                ArrayList<TutorialData> tuts = temp.get(temp1.lec_num).tutorials;


                // find the tutorial with the same tutorial number and record its number
                for(TutorialData tut: tuts)
                {
                    pair.id1 = -1;
                    if(tut.tut_num == temp1.tut_num)
                    {
                        pair.id1 = tut.id;
                        break;
                    }
                }
                // make sure that the id was found
                if(pair.id1 == -1)
                {
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }

        return true;
    }

    /**
     * Parse line1 and line2 for a tut tut pair ids
     * @param line1 the first line to parse
     * @param line2 the second line to parse
     * @param lec_tut_data the map from lectures and tutorials to ids
     * @retrun true if a pair could be found, false if not
     */
    private static boolean ParseForTutTutPair(String line1, String line2, Pair pair, HashMap<String, HashMap<Integer, LectureData>> lec_tut_data)
    {
        // 1:lecture, 2:lecture
        TutorialData temp1 = new TutorialData();
        TutorialData temp2 = new TutorialData();

        // parse the strings for the data 
        if(!TryGetTutorialBasicFromLine(line1, temp1))
        {
            return false;
        }

        if(!TryGetTutorialBasicFromLine(line2, temp2))
        {
            return false;
        }

        // get the id of the second element
        if(lec_tut_data.containsKey(temp1.course_descriptor))
        {
            // already exists so get the current map of lecturData elements to get the id
            HashMap<Integer, LectureData> temp = lec_tut_data.get(temp1.course_descriptor);
            if(temp.containsKey(temp1.lec_num))
            {
                // get the array of tutorials
                ArrayList<TutorialData> tuts = temp.get(temp1.lec_num).tutorials;


                // find the tutorial with the same tutorial number and record its number
                for(TutorialData tut: tuts)
                {
                    pair.id1 = -1;
                    if(tut.tut_num == temp1.tut_num)
                    {
                        pair.id1 = tut.id;
                        break;
                    }
                }
                // make sure that the id was found
                if(pair.id1 == -1)
                {
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }

        // get the id of the second element
        if(lec_tut_data.containsKey(temp2.course_descriptor))
        {
            // already exists so get the current map of lecturData elements to get the id
            HashMap<Integer, LectureData> temp = lec_tut_data.get(temp2.course_descriptor);
            if(temp.containsKey(temp2.lec_num))
            {
                // get the array of tutorials
                ArrayList<TutorialData> tuts = temp.get(temp2.lec_num).tutorials;


                // find the tutorial with the same tutorial number and record its number
                for(TutorialData tut: tuts)
                {
                    pair.id2 = -1;
                    if(tut.tut_num == temp2.tut_num)
                    {
                        pair.id2 = tut.id;
                        break;
                    }
                }
                // make sure that the id was found
                if(pair.id2 == -1)
                {
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }

        return true;
    }


    /**
     * Parse the input file for the tutorial data
     * @param bufferedReader the file reader to get the lines from
     * @param lec_tut_data the hashmap to store the tutorial data in
     * @return true if parse was successfull, false if errors occured
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
                        System.out.println("INPUT WARNING: (Tutorials) attempt to add tutorial twice: " + temp_tut.course_descriptor + " " + temp_tut.lec_num);
                    }
                }
                else
                {
                    System.out.println("INPUT WARNING: (Tutorials) attempt to add tutorial without corresponding lecture: " + temp_tut.course_descriptor);
                }
            }
            else
            {
                System.out.println("INPUT WARNING: (Tutorials) invalid information in line: " + nextLine);
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
     * takes a string and tries to convert it to basic tutorial data (course_descriptor, lecture number, tutorial number)
     * @param line the line to parse for the tutorial data
     * @param lec the tutorial data structure to return the information in
     * @return true if the line could be parsed for tutorial data, false otherwise
     */
    private static boolean TryGetTutorialBasicFromLine(String line, TutorialData tut)
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
        if(elements.length > 1)
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

        return true;
    }

    /**
     * takes a string and tries to convert it to tutorial data
     * @param line the line to parse for the tutorial data
     * @param lec the tutorial data structure to return the information in
     * @return true if the line could be parsed for tutorial data, false otherwise
     */
    private static boolean TryGetTutorialFromLine(String line, TutorialData tut)
    {
        // split the string by ',' deliminator
        String[] elements;

        // record the full name of the tutorial
        tut.name = line.split(",")[0];

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
            if(lec_info[1].charAt(0) == '9')
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
            // if it does not contain "LEC" then this tutorial bellongs to all lectures in the section
            // so set the lecture number to 1 (not important) and use section to true
            tut.lec_num = 1;
            tut.use_section = true;
            tut.is_evng = true;
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
        
        // record the tutorial number
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
     * @param bufferedReader the file reader to get the lines from
     * @param lec_tut_data the hashmap of lecture data to put the results in
     * @return true if parse was successfull, false if errors occured
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
                    else
                    {
                        System.out.println("INPUT WARNING: (lectures) attempt to add same lecture twice: " + nextLine);
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
            else
            {
                System.out.println("INPUT WARNING: (lectures) invalid information in line: " + nextLine);
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
     * takes a string and tries to convert it to only the basic lecture data (course_descriptor and lecture number
     * @param line the line to parse for the lecture information
     * @param lec the lecture data structure to retrun the information in
     * @return true if the line could be parsed for lecture data, false otherwise
     */
    private static boolean TryGetLectureBasicFromLine(String line, LectureData lec)
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

        // get the course number and AL
        elements = elements[1].split(",");
        
        // there should be 2 elements
        if(elements.length > 1)
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

        return true;
    }

    /**
     * takes a string and tries to convert it to lecture data
     * @param line the line to parse for the lecture information
     * @param lec the lecture data structure to retrun the information in
     * @return true if the line could be parsed for lecture data, false otherwise
     */
    private static boolean TryGetLectureFromLine(String line, LectureData lec)
    {
        // split the string by ',' deliminator
        String[] elements;

        // record the full name of the lecture
        lec.name = line.split(",")[0];;

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
        elements[0] = elements[0].stripLeading();
        elements[1] = elements[1].stripLeading();
        
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
        if(elements[0].charAt(0) == '9')
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
     * @param bufferedReader the file reader to get the lines from
     * @return the name of the dataset, null or empty if not found
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
     * @param bufferedReader the file reader to get the lines from
     * @param lecture_slots the hashmap of lecture slots to put the results in
     * @return true if parse was successfull, false if errors occured
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
            else
            {
                System.out.println("INPUT WARNING: (lecture slots) invalid information in line: " + nextLine);
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
     * @param bufferedReader the file reader to get the lines from
     * @param tutorial_slots the hashmap of tutorial slots to put the results in
     * @return true if parse was successfull, false if errors occured
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
                tutorial_slots.put(temp_slot.tut_hash,temp_slot);
            }
            else
            {
                System.out.println("INPUT WARNING: (tutorial slots) invalid information in line: " + nextLine);
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
     * takes a string and tries to convert it to basic slot information (Day and time)
     * @param line the line to parse for the slot information
     * @param slot the slot to put the return data into
     * @return true if the line could be parsed for slot information, false otherwise
     */
    private static boolean TryGetBasicSlotFromLine(String line, Slot return_slot)
    {
        // split the string by ',' deliminator
        String[] elements = line.split(",");
                
        // there must be 5 elements (day,time,lecturemax,lecturemin,allecturemax)
        if(elements.length < 2)
        {
            return false;
        }
       
        elements[0] = elements[0].strip();
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
        String[] hr_min = elements[1].split(":");
        // there should be 2 elements
        if(hr_min.length != 2)
        {
            return false;
        }
        hr_min[0] = hr_min[0].strip();
        hr_min[1] = hr_min[1].strip();

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
        return_slot.minute = buffer[0];

        // setup the slot
        return_slot.SetupSlot();

        return true;
    }

    /**
     * takes a string and tries to convert it to a lecture slot
     * @param line the line to parse for the slot information
     * @param slot the slot to put the return data into
     * @return true if the line could be parsed for slot information, false otherwise
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
        return_slot.name = elements[0] + "," + elements[1].stripTrailing();

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
        hr_min[0] = hr_min[0].strip();
        hr_min[1] = hr_min[1].strip();

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
        return_slot.minute = buffer[0];

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

    /**
     * Get an integer from a string 
     * @param input_string the input string to parse
     * @param int_return the array to put the integer in (will be put into index 0)
     * @return true if a number was found, false if not
     */
    private static boolean GetSafeIntFromString(String input_string, int[] int_return)
    {
        // check that the given array is not null and has at least one index for assigning values
        if(int_return == null || int_return.length == 0)
        {
            return false;
        }

        // try to get an integer from the string
        try{
            int_return[0] = Integer.parseInt(input_string);
            // if the string was a float then return the float by reference and return true
            return true;
        }catch(NumberFormatException e)
        {
            // if the string was not a float then return false and an error message to the user
            int_return[0] = -1;
            return false;
        }
    }
}

class Preference
{
    boolean is_lec; // is this a lecture
    int id; // the id of the lecture/tutorial
    int slot_id; // the id of the slot
    int value; // the preference value
}

class UnwantedPair
{
    boolean is_lec; // is this a lecture
    int id; // the id of the lecture/tutorial
    int slot_id; // the id of the slot
}


class LectureData
{
    int id; // the unique id of the lecture 
    String course_descriptor; // e.g. CPSC 433
    String name; // the full name of the lecture
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
     * NOTE: tutorail array must be assigned seperately
     * @param id the unique id to give this lecture
     * @param section the section that this lecture bellongs to 
     * @return a lecture data structure
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
        temp.name = name;
        temp.course_descriptor = course_descriptor;
        temp.lec_num = lec_num;
        return temp;
    }
}


class TutorialData
{
    int id; // the unique id of the tutorial
    String course_descriptor; // e.g. CPSC 433
    String name; // the full name of the lecture
    boolean is_evng; // is this an evening lecture
    int lec_num; // e.g. LEC 01 -> lec_num = 1 (defualt to 1 if not included)
    boolean use_section = false; // if this tutorial bellongs to all lectures in a section, then set this to true
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
     * @param id the unique id to give this tutorial
     * @return a lecture data structure
     */
    public Tutorial ConvertToTutorial(int _id, int section, int lecture)
    {
        Tutorial temp = new Tutorial();
        id = _id;
        temp.id = _id;
        temp.is_al = is_al;
        temp.is_evng = is_evng;
        if(use_section)
        {
            temp.lec_num = 0;
            temp.section = section;
        }
        else
        {
            temp.lec_num = lec_num;
            temp.section = -1;
            // set the lecture as the only entry in parent lectures
            temp.parent_lectures = new int[1];
            temp.parent_lectures[0] = lecture;
        }
        temp.name = name;
        temp.tut_num = tut_num;
        temp.course_descriptor = course_descriptor;
        return temp;
    }
}
