package schedulesearch;
import java.util.ArrayList;
import java.util.HashMap;

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
    long time_limit; 

    // RECORD ############################################################################################
    // the current execution time since the start of the search
    long current_time = 0;
    // the start time (needed for measuring time since timer does not start at zero)
    long start_time = 0;
    // the number of iterations used so far
    int iterations = 0;
    // the best solution found so far
    Problem best_sol;
    // the score of the best solution found so far
    int best_score = 2000000000;

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
    HashMap<Integer, Integer[]> sections = new HashMap<Integer, Integer[]>();
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
        time_limit = ((int)_time_limit) * 1000000000;
    }
}