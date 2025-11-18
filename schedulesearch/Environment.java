package schedulesearch;
import java.util.ArrayList;

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
    // the arraylist of lectures 
    ArrayList<Lecture> lectures = new ArrayList<Lecture>();
    // the arraylist of tutorials
    ArrayList<Tutorial> tutorials = new ArrayList<Tutorial>();
    // the array of pairs used to implement Pair
    ArrayList<Pair> pairs = new ArrayList<Pair>();
    // the lecture slots
    ArrayList<Slot> lectureSlots = new ArrayList<Slot>();
    // the tutorial slots
    ArrayList<Slot> tutorialSlots = new ArrayList<Slot>();

    public Environment()
    {
    }

    /**
     * set all the weights and penalties used for calculating the scores of problems 
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