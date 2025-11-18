package schedulesearch;

/**
 * the Functions class contains all the static functions needed for the And Tree search
 * This class was created so that these functions are available to the inputParser and Main classes as well
 */
public final class Functions
{
    /**
     * MinBoundScore calculates the minimum score of a problem that cannot do better than
     * @param pr: the problem to evaluate
     * @param env: the environment with the penalties and weights
     * @return: the score of the problem
     */ 
    public static int MinBoundScore(Problem pr, Environment env)
    {
        // EvalPref and EvalSecDiff are permenant scores that cannot be reduced as more assignments are made
        int sum = EvalPref(pr, env);
        sum += EvalSecDiff(pr, env);
        return sum;
    }

    /**
     * Solvable determines if problem pr is solvable
     * @param pr: the problem to check
     * @return: true if pr is solvable, false otherwise
     */ 
    public static boolean Solvable(Problem pr)
    {
        // sudo code
        // if all lectures and tutorials have a non null assignment in pr, then the problem is solvable so return true
        // if there are lectures or tutorials with null assignments in pr, and there are no valid slots to assign the null
        // lectures or tutorials then the problem is solvable
        return false;
    }

    /**
     * Fbound determines if the leaf represented by this problem can be pruned from the search tree
     * It does this by determining if there is no possible way for this problem to achive a better score than the one that has already been found
     * @param pr: the problem to check
     * @param env: the environment that stores the best score so far
     * @return: true if this problem cannot give a better solution, false otherwise
     */ 
    public static boolean FBound(Problem pr, Environment env)
    {
        // sudo code
        // evaluate the minboundscore of pr, if this score is greater than the best score found so far return true
        // return false otherwise
        return false;
    }

    /**
     * Depth gets the depth of a problem in the tree
     * @param pr: the problem to evaluate
     * @return: the depth of the problem (0 is the root of the tree)
     */ 
    public static int Depth(Problem pr)
    {
        // sudo code 
        // during each expansion, only one lecture or tutorial is assigned, so depth is the number of tutorials and lectures assigned
        // maybe add a feild to Problem that records the depth in the tree and just return that
        return 0;
    }

    /**
     * Eval calculates the score of a problem assignment
     * @param pr: the problem to score
     * @param env: the environment with the scoring parameters
     * @return: the score of the problem
     */
    public static int Eval(Problem pr, Environment env)
    {
        int sum = EvalMinFilled(pr, env);
        sum += EvalPref(pr, env);
        sum += EvalPair(pr, env);
        sum += EvalSecDiff(pr, env);
        return sum;
    }

    private static int EvalMinFilled(Problem pr, Environment env)
    {
        return 0;
    }

    private static int EvalPref(Problem pr, Environment env)
    {
        return 0;
    }
    
    private static int EvalPair(Problem pr, Environment env)
    {
        return 0;
    }

    private static int EvalSecDiff(Problem pr, Environment env)
    {
        return 0;
    }

    /**
     * ValidLectureSlots finds all valid slots for a given lecture
     * @param env: the environment
     * @param lec_id: the unique id of the lecture 
     * @param pr: the problem that has the existing lecture and tutorial assignments
     * @return: an array of unique lecture slot id's
     */ 
    public static int[] ValidLectureSlots(Environment env, int lec_id, Problem pr)
    {
        // sudo code 
        // start with all lecture slots and use the following filters
        // if slot s is full then remove s from consideration
        // if lec_id is an active learning lecture and slot s has no free active learning slots then remove s from consideration
        // if any of the tutorials for lecture l have been assigned slot s then remove s from consideration
        // if any of the lectures or tutorials that are not compatible with l have been assigned slot s then remove s from consideration
        // if unwanted(l,s) is true then remove s from consideration
        // if lecture l is an evening lecture and s is not an evening time slot then remove s from consideration
        // if lecture l is a 5XX level course and there is another 5XX level lecture assigned to slot s then remove s from consideration
        // return the id's of all remaining slots

        return new int[0];
    }

    /**
     * ValidTutSlots finds all valid slots for a given tutorial
     * @param env: the environment
     * @param tut_id: the unique tutorial id of the tutorial
     * @param pr: the problem that has the existing lecture and tutorial assignments
     * @return: an array of unique tutorial slot id's
     */
    public static int[] ValidTutSlots(Environment env, int tut_id, Problem pr)
    {
        // sudo code 
        // start with all tutorial slots and use the following filters
        // if slot s is full then remove s from consideration
        // if tut_id is an active learning tutorial and slot s has no free active learning slots then remove s from consideration
        // if the lecture associated with tutorial t has been assigned slot s then remove slot s from consideration
        // if any of the lectures or tutorials that are not compatible with t have been assigned slot s then remove s from consideration
        // if unwanted(t,s) is true then remove s from consideration
        // return the id's of all remaining slots

        return new int[0];
    }

}