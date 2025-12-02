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
    // For the constraint ranking
    int w_al = 1000000;
    int w_evng = 100000;
    int w_5xx = 10000;

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

    /**
     * calculate the best score that can possibly be achived
     */
    public int BestPossibleScore()
    {
        int max = total_pref_sum;
        // total preference sum - best preferences
        for(int i = 0; i < lectures.length; i++)
        {
            max -= lectures[i].first_choice;
        }

        for(int i = 0; i < tutorials.length; i++)
        {
            max -= tutorials[i].first_choice;
        }

        return max;
    }

    /**
     * create a list of lecture/tutorial ids ordered by their constraint rank
     */
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
            // calculate the rank of this lecture
            temp.rank_value = CalculateLectureRank(lectures[i]); 
            lectures_tutorials.add(temp);
        }

        // put all the tutorials input this array
        for(int i = 0; i < tutorials.length; i++)
        {
            LecOrTutId temp = new LecOrTutId();
            temp.is_lec = false;
            temp.id = tutorials[i].id;

            // calculate the rank of this tutorial
            temp.rank_value = CalculateTutorialRank(tutorials[i]);
            lectures_tutorials.add(temp);
        }

        // quick!!! sort the list
        Collections.sort(lectures_tutorials, new ConstraintComparator());

        // now put the sorted list into the environments array
        constraint_ordering = new LecOrTutId[num_lectures + num_tutorials];
        
        // copy the list
        for(int i = 0; i < constraint_ordering.length; i++)
        {
            constraint_ordering[i] = lectures_tutorials.get(i);
        }
    }

    /**
     * calculate the rank value of a lecture
     */
    private int CalculateLectureRank(Lecture lec)
    {
        // sum the values of things that contribute to the constraint rank of the lecture
        int value = 0;
        // check for evening
        if(lec.is_evng)
        {
            value += w_evng;
        }

        // check for active learning
        if(lec.is_al)
        {
            value += w_al;
        }

        // check for 5xx level course
        if(lec.is_5xx)
        {
            value += w_5xx;
        }
        
        // count number of occurances in not compatible, unwanted, sections, and parent child tutorials
        // not compatible
        value += lec.not_compatible_tut.size();
        // unwanted
        value += lec.unwanted.size();
        // sections
        value += sections.get(lec.section).length;
        // number of child tutorials
        value += lec.tutorials.length;

        return value;
    }

    /**
     * calculate the rank value of a tutorial
     */
    private int CalculateTutorialRank(Tutorial tut)
    {
        // sum the values of things that contribute to the constraint rank of the lecture
        int value = 0;
        // check for evening
        if(tut.is_evng)
        {
            value += w_evng;
        }

        // check for active learning
        if(tut.is_al)
        {
            value += w_al;
        }
        
        // count number of occurances in not compatible, unwanted, and parent lectures
        // not compatible
        value += tut.not_compatible_tut.size();
        // unwanted
        value += tut.unwanted.size();
        // number of child tutorials
        value += tut.parent_lectures.length;

        return value;
    }

    private void SumPreferences()
    {
        // sum the preference values
        total_pref_sum = 0;
        // preference values for lectures 
        for(int i = 0; i < lectures.length; i++)
        {
            // also record the first choice of this lecture
            int first = 0;

            for(Integer value : lectures[i].preferences.values())
            {
                if(value > first)
                {
                    first = value;
                }
                total_pref_sum += value;
            }

            lectures[i].first_choice = first;
        }
        // preference values for tutorials
        for(int i = 0; i < tutorials.length; i++)
        {
            // also record the first choice of this tutorial
            int first = 0;
            for(Integer value : tutorials[i].preferences.values())
            {
                if(value > first)
                {
                    first = value;
                }
                total_pref_sum += value;
            }

            tutorials[i].first_choice = first;
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
        
        // sort based on rank
        if(b.rank_value > a.rank_value)
        {
            return -1;
        }
        else if(b.rank_value < a.rank_value)
        {
            return 1;
        }
        
        // if same rank, then lectures first
        if(b.is_lec && !a.is_lec)
        {
            return -1;
        }
        else if(!b.is_lec && a.is_lec)
        {
            return 1;
        }

        // if same rank and same type, then they will have different id
        if(b.id > a.id)
        {
            return -1;
        }
        else
        {
            return 1;
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
    // the value representing the constraint rank
    int rank_value = 0;
}

