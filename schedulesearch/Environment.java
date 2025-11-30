package schedulesearch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Collections;

/**
 * The Environment class holds all the un-changing information for the search
 */ 
public class Environment
{

    // INPUT SCALARS ################################################################################
    // the weights used in the score calculations
    int w_minfilled;
    int w_pref;
    int w_pair;
    int w_secdiff;

    // the penalties used in the score calculations
    int pen_lecturemin;
    int pen_tutorialmin;
    int pen_notpaired;
    int pen_section;

    // max iterations
    int max_iterations;
    // time limit in seconds
    double time_limit; 

    // RECORD ############################################################################################
    // the best solution found so far
    Problem best_sol;
    // the score of the best solution found so far
    int best_score = 2000000000;
    // indicate if a solution has been found at all
    boolean solution_found = false;

    // Environment DATA #####################################################################################
    String dataset_name;
    
    // the number of lectures that need assigning
    int num_lectures;
    // the number of tutorials that need assigning
    int num_tutorials;
    
    // main data structures
    // the arraylist of lectures 
    Lecture[] lectures;
    // the arraylist of tutorials
    Tutorial[] tutorials;
    // the array of lecture slots
    Slot[] lec_slots_array;
    // the array of tutorial slots
    Slot[] tut_slots_array;


    // maps for cross lookup operations
    // the array of lectures at 5xx level
    int[] lectures_5xx;
    // the map from sections to lecture ids
    HashMap<Integer, int[]> sections = new HashMap<Integer, int[]>();
    // the array of pairs used to implement Pair
    Pair[] pairs;
    // the map of slot lecture hash to slot
    HashMap<Integer, Slot> lecture_slots;
    // the map of slot tutorial hash to slot
    HashMap<Integer, Slot> tutorial_slots;
    // the map from lecture tutorial slot ids to overlapping lecture ids
    int[][] tutslot_lecslot;
    // index is the lecture slot id, value is the corresponding tutorial slot
    int[][] lecslot_tutslot;
    // array in sorted order of the lecturs and totorials by their constraints (lookup by depth if done right)
    LecOrTutId[] constraint_ordering;

    // lookup lecture ids of lectures that a scheduled at the exact same time as a given tutorial
    int[] tutid_to_lecid;

    // this is the total preference value, the preference score of a selected slot is simply removed from this sum
    int total_pref_sum = 0;

    public Environment()
    {
    }

    /**
     * set all the weights and penalties used for calculating the scores of problems 
     * @param _w_minfilled 
     * @param _w_pref
     * @param _w_secdiff
     * @param _pen_lecturemin
     * @param _pen_tutorialmin
     * @param _pen_notpaired
     * @param _pen_section
     * @param _max_iterations
     * @param _time_limit
     */ 
    public void SetWeights(int _w_minfilled, int _w_pref, int _w_pair, int _w_secdiff, int _pen_lecturemin, int _pen_tutorialmin, int _pen_notpaired, int _pen_section, int _max_iterations, int _time_limit)
    {
        w_minfilled = _w_minfilled;
        w_pref = _w_pref;
        w_pair = _w_pair;
        w_secdiff = _w_secdiff;
        pen_lecturemin = _pen_lecturemin;
        pen_tutorialmin = _pen_tutorialmin;
        pen_notpaired = _pen_notpaired;
        pen_section = _pen_section;
        max_iterations = _max_iterations;
        time_limit = (double)_time_limit;
    }

    /**
     * Should be called after all environment data is given to the environment and before the search is run
     * This function sorts the lectures and tutorials based on priority and creates the array for sort order
     */
    public void SetupEnvironment()
    {
        CreateConstraintRankList();
        SumPreferences();
        CreateTutIDtoLecIDmap();
    }

    private void CreateConstraintRankList()
    {
        // array for storing the ids of all lectures and tutorials
        ArrayList<LecOrTutId> lectures_tutorials = new ArrayList<LecOrTutId>();

        // put all lectures and tutorials into this array
        for(int i = 0; i < lectures.length; i++)
        {
            LecOrTutId temp = new LecOrTutId();
            temp.is_lec = true;
            temp.id = lectures[i].id; // id and i should be the same
            lectures_tutorials.add(temp);
        }

        // put all the tutorials input this array
        for(int i = 0; i < tutorials.length; i++)
        {
            LecOrTutId temp = new LecOrTutId();
            temp.is_lec = false;
            temp.id = tutorials[i].id;
            lectures_tutorials.add(temp);
        }

        // quick!!! sort the list
        Collections.sort(lectures_tutorials, new ConstraintComparator(this));

        // now put the sorted list into the environments array
        constraint_ordering = new LecOrTutId[num_lectures + num_tutorials];
        
        // copy the list
        for(int i = 0; i < constraint_ordering.length; i++)
        {
            constraint_ordering[i] = lectures_tutorials.get(i);
        }
    }

    private void SumPreferences()
    {
       // sum the preference values
       total_pref_sum = 0;
       // preference values for lectures 
       for(int i = 0; i < lectures.length; i++)
       {
           
           for(Integer value : lectures[i].preferences.values())
           {
               total_pref_sum += value;
           }
       }
       // preference values for tutorials
       for(int i = 0; i < tutorials.length; i++)
       {
           for(Integer value : tutorials[i].preferences.values())
           {
               total_pref_sum += value;
           }
       }    
    }

    private void CreateTutIDtoLecIDmap()
    {
        // initialize the array
        tutid_to_lecid = new int[tut_slots_array.length];

        // loop through all tutorials and see if there is a corresponding lecture slot at the same time
        for(int i = 0; i < tut_slots_array.length; i++)
        {
            if(lecture_slots.containsKey(tut_slots_array[i].lec_hash))
            {
                tutid_to_lecid[i] = lecture_slots.get(tut_slots_array[i].lec_hash).id;
            }
            else
            {
                tutid_to_lecid[i] = -1;
            }
        }
    }
}

/**
 * implements the comparison based on the number of constraints on a lecture or tutorial
 */
class ConstraintComparator implements Comparator<LecOrTutId>
{
    // need environment information to perform the sort
    Environment env;

    /**
     * constructor for the comparator
     * @param _env give the comparator the environment data 
     */
    public ConstraintComparator(Environment _env)
    {
        env = _env;
    }
    /**
     * implements the sorting function for comparator
     * @param a the first object to be compared.
     * @param b the second object to be compared.
     * @return 1 if a should appear above b, 0 if they are equal, -1 otherwise
     */
    public int compare(LecOrTutId b, LecOrTutId a)
    {
        // sort by the following priorities
        // 1. evening lectures/tutorials are ranked first, tie break on ordering
        // 2. active learning lectures/tutorials are ranked first, tie break on ordering
        // 3. 5xx level lectures are ranked first, tie break on ordering
        // 4. number of occurances in notCompatible, Unwanted, Sections, and parent lectures/ child tutorails
        
        if(a.is_lec && b.is_lec)
        {
            // lecture v lecture
            Lecture lec_a = env.lectures[a.id];
            Lecture lec_b = env.lectures[b.id];
            return LecVLec(lec_a, lec_b);
        }
        else if(a.is_lec && !b.is_lec)
        {
            // lecture v tutorial
            Lecture lec_a = env.lectures[a.id];
            Tutorial tut_b = env.tutorials[b.id];
            return LecVTut(lec_a, tut_b);
        }
        else if(!a.is_lec && b.is_lec)
        {
            // lecture v tutorial
            Lecture lec_b = env.lectures[b.id];
            Tutorial tut_a = env.tutorials[a.id];
            return (-1 * LecVTut(lec_b, tut_a));

        }
        else
        {
            // tutorial v tutorial
            Tutorial tut_a = env.tutorials[a.id];
            Tutorial tut_b = env.tutorials[b.id];
            return TutVTut(tut_a, tut_b);
        }
    }

    /**
     * implement the compare constraint rank based on two lectures
     * @param a first lecture
     * @param b second lecture
     */
    private int LecVLec(Lecture a, Lecture b)
    {
        // sort by the following priorities
        // 1. evening lectures/tutorials are ranked first, tie break on ordering
        // 2. active learning lectures/tutorials are ranked first, tie break on ordering
        // 3. 5xx level lectures are ranked first, tie break on ordering
        // 4. number of occurances in notCompatible, Unwanted, Sections, and parent lectures/ child tutorails
        
        // check for evening
        if(a.is_evng && !b.is_evng)
        {
            return 1;
        }
        else if(!a.is_evng && b.is_evng)
        {
            return -1;
        }

        // check for active learning
        if(a.is_al && !b.is_al)
        {
            return 1;
        }
        else if(!a.is_al && b.is_al)
        {
            return -1;
        }

        // check for 5xx level lectures (this will be different for tutorials
        if(a.is_5xx && !b.is_5xx)
        {
            return 1;
        }
        else if(!a.is_5xx && b.is_5xx)
        {
            return -1;
        }

        // count number of occurances in not compatible, unwanted, sections, and parent child tutorials
        int a_count = 0;
        int b_count = 0;

        // not compatible
        a_count = a.not_compatible_lec.size();
        a_count += a.not_compatible_tut.size();

        b_count = b.not_compatible_lec.size();
        b_count += b.not_compatible_tut.size();


        // unwanted 
        a_count += a.unwanted.size();
        b_count += b.unwanted.size();

        // sections
        a_count += env.sections.get(a.section).length;
        b_count += env.sections.get(b.section).length;

        // number of child tutorials
        a_count += a.tutorials.length;
        b_count += b.tutorials.length;

        // compare
        if(a_count > b_count)
        {
            return 1;
        }
        else if(b_count < a_count)
        {
            return -1;
        }

        // tie breaker
        if(a.hashCode() > b.hashCode())
        {
            return 1;
        }
        else
        {
            return -1;
        }

    }

    /**
     * implement the compare constraint rank based on a lecture and a tutorial
     */
    private int LecVTut(Lecture a, Tutorial b)
    {
        // sort by the following priorities
        // 1. evening lectures/tutorials are ranked first, tie break on ordering
        // 2. active learning lectures/tutorials are ranked first, tie break on ordering
        // 3. 5xx level lectures are ranked first, tie break on ordering
        // 4. number of occurances in notCompatible, Unwanted, Sections, and parent lectures/ child tutorails
        
        // check for evening
        if(a.is_evng && !b.is_evng)
        {
            return 1;
        }
        else if(!a.is_evng && b.is_evng)
        {
            return -1;
        }

        // check for active learning
        if(a.is_al && !b.is_al)
        {
            return 1;
        }
        else if(!a.is_al && b.is_al)
        {
            return -1;
        }

        // check for 5xx level lecture
        if(a.is_5xx)
        {
            return 1;
        }

        // count number of occurances in not compatible, unwanted, sections, and parent child tutorials
        int a_count = 0;
        int b_count = 0;

        // not compatible
        a_count = a.not_compatible_lec.size();
        a_count += a.not_compatible_tut.size();

        b_count = b.not_compatible_lec.size();
        b_count += b.not_compatible_tut.size();


        // unwanted 
        a_count += a.unwanted.size();
        b_count += b.unwanted.size();

        // sections
        a_count += env.sections.get(a.section).length;
        // get the first parent lectrue, lookup its section 
        b_count += env.sections.get(env.lectures[b.parent_lectures[0]].section).length;

        // number of child tutorials
        a_count += a.tutorials.length;
        b_count += b.parent_lectures.length;

        // compare
        if(a_count > b_count)
        {
            return 1;
        }
        else if(b_count < a_count)
        {
            return -1;
        }

        // tie breaker
        if(a.hashCode() > b.hashCode())
        {
            return 1;
        }
        else
        {
            return -1;
        }
    }

    /**
     * implement the compare constraint rank based on two tutorials
     */
    private int TutVTut(Tutorial a, Tutorial b)
    {
        // sort by the following priorities
        // 1. evening lectures/tutorials are ranked first, tie break on ordering
        // 2. active learning lectures/tutorials are ranked first, tie break on ordering
        // 3. 5xx level lectures are ranked first, tie break on ordering
        // 4. number of occurances in notCompatible, Unwanted, Sections, and parent lectures/ child tutorails
        
        // check for evening
        if(a.is_evng && !b.is_evng)
        {
            return 1;
        }
        else if(!a.is_evng && b.is_evng)
        {
            return -1;
        }

        // check for active learning
        if(a.is_al && !b.is_al)
        {
            return 1;
        }
        else if(!a.is_al && b.is_al)
        {
            return -1;
        }

        // count number of occurances in not compatible, unwanted, sections, and parent child tutorials
        int a_count = 0;
        int b_count = 0;

        // not compatible
        a_count = a.not_compatible_lec.size();
        a_count += a.not_compatible_tut.size();

        b_count = b.not_compatible_lec.size();
        b_count += b.not_compatible_tut.size();


        // unwanted 
        a_count += a.unwanted.size();
        b_count += b.unwanted.size();

        // sections
        a_count += env.sections.get(env.lectures[a.parent_lectures[0]].section).length;
        b_count += env.sections.get(env.lectures[b.parent_lectures[0]].section).length;

        // number of child tutorials
        a_count += a.parent_lectures.length;
        b_count += b.parent_lectures.length;

        // compare
        if(a_count > b_count)
        {
            return 1;
        }
        else if(b_count < a_count)
        {
            return -1;
        }

        // tie breaker
        if(a.hashCode() > b.hashCode())
        {
            return 1;
        }
        else
        {
            return -1;
        }
    }
}

/**
 * holds a lecture or tutorial id
 */
class LecOrTutId
{
    // is this a lecture id
    boolean is_lec = false;
    // the id of the lecture or tutorial
    int id = -1;
}

