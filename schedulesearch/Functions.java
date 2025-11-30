package schedulesearch;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * the Functions class contains all the static functions needed for the And Tree search
 * This class was created so that these functions are available to the inputParser and Main classes as well
 */
public final class Functions
{
    /**
     * MinBoundScore calculates the minimum score of a problem that cannot do better than
     * @param pr the problem to evaluate
     * @param env the environment with the penalties and weights
     * @return the score of the problem
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
     * @param pr the problem to check
     * @param env the environment
     * @return true if pr is solvable, false otherwise
     */ 
    public static boolean Solvable(Problem pr, Environment env)
    {
        // sudo code
        // if all lectures and tutorials have a non null assignment in pr, then the problem is solvable so return true
        // if there are lectures or tutorials with null assignments in pr, and there are no valid slots to assign the null
        // lectures or tutorials then the problem is solvable

        // iterate through all lecs in pr
        for (int i = 0; i < pr.lectures.length; i++) {
            // if a lec assignment is null, check if it has any valid slot assignments
            if (pr.lectures[i] == -1) {
                int[] validLecs = ValidLectureSlots(env, i, pr);
                // if there are valid slot assignments, solution can still be expanded
                if (validLecs.length > 0) {
                    return false;
                }
            }
        }

        // iterate through all tutorials in pr
        for (int i = 0; i < pr.tutorials.length; i++) {
            // if a tut assignment is null, check if it has any valid assignments
            if (pr.tutorials[i] == -1) {
                int[] validTuts = ValidTutSlots(env, i, pr);
                // if there are valid assignments, solution can be expanded still
                if (validTuts.length > 0) {
                    return false;
                }
            }
        }

        //solution is not expandable
        return true;
    }

    /**
     * Fbound determines if the leaf represented by this problem can be pruned from the search tree
     * It does this by determining if there is no possible way for this problem to achive a better score than the one that has already been found
     * @param pr the problem to check
     * @param env the environment that stores the best score so far
     * @return true if this problem cannot give a better solution, false otherwise
     */ 
    public static boolean FBound(Problem pr, Environment env)
    {
        // sudo code
        // evaluate the minboundscore of pr, if this score is greater than the best score found so far return true
        // return false otherwise

        int mbs = MinBoundScore(pr, env);
        if (mbs > env.best_score) return true;
        return false;
    }

    /**
     * Depth gets the depth of a problem in the tree
     * @param pr the problem to evaluate
     * @return the depth of the problem (0 is the root of the tree)
     */ 
    public static int Depth(Problem pr)
    {
        // sudo code 
        // during each expansion, only one lecture or tutorial is assigned, so depth is the number of tutorials and lectures assigned
        // maybe add a feild to Problem that records the depth in the tree and just return that

        // variable for depth value
        int depth = 0;
        // iterate through all lectures
        for (int i = 0; i < pr.lectures.length; i++) {
            // if lecture is null then continue looping
            if (pr.lectures[i] == -1) {
                continue;
            }
            // otherwise this lecture has been assigned so increase depth count by 1
            depth++;
        }
        // iterate through all tutorials
        for (int i = 0; i < pr.tutorials.length; i++) {
            // if tutorial is null then continue looping
            if (pr.tutorials[i] == -1) {
                continue;
            }
            // otherwise this tutorial has been assigned so increase depth count by 1
            depth++;
        }
        // return the depth count, return depth if we want to use function instead of pr.depth
        return pr.depth;
    }

    /**
     * Eval calculates the score of a problem assignment
     * @param pr the problem to score
     * @param env the environment with the scoring parameters
     * @return the score of the problem
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
        // Variables for lecture penalty and tutorial penalty
        int lec_penalty = 0;
        int tut_penalty = 0;

        // Array to store lecture counts for each lecture slot
        int[] lec_counts = new int[env.lec_slots_array.length];
        // iterate through every lectures that have been assigned
        for (int lec_id = 0; lec_id < pr.lectures.length; lec_id++) {
            // get the slot_id that the lecture has been assigned
            int slot_id = pr.lectures[lec_id];
            // if >= 0 then this lecture has been assigned and increment count for the slot_id by 1
            if (slot_id >= 0) {
                lec_counts[slot_id]++;
            }
        }
        // Repeat for tutorials
        // Array to store tutorial counts for each tutorial slot
        int [] tut_counts = new int[env.tut_slots_array.length];
        // iterate through every tutorial that's been assigned
        for (int tut_id = 0; tut_id < pr.tutorials.length; tut_id++) {
            // get the slot_id that the tutorial has been assigned
            int slot_id = pr.tutorials[tut_id];
            // if >= 0 then this tutorial has been assigned and increment count for the slot_id by 1
            if (slot_id >= 0) {
                tut_counts[slot_id]++;
            }
        }
        // get lecture penalty
        for (int i = 0; i < env.lec_slots_array.length; i++) {
            int count = lec_counts[i];
            int p = Math.max(env.lec_slots_array[i].min - count, 0);
            lec_penalty += p * env.pen_lecturemin;
        }
        // get tutorial penalty
        for (int i = 0; i < env.tut_slots_array.length; i++) {
            int count = tut_counts[i];
            int p = Math.max(env.tut_slots_array[i].min - count, 0);
            tut_penalty += p * env.pen_tutorialmin;
        }
        // return the summation of lecture penalty and tutorial penalty
        return lec_penalty + tut_penalty;
    }

    private static int EvalPref(Problem pr, Environment env)
    {
        int lecPenalty = 0;
        int tutPenalty = 0;

        // Sum of lecture penalties
        for (int i = 0; i < pr.lectures.length; i++) {
            int slotId = pr.lectures[i];

            // skip unassigned
            if (slotId >= 0) { 
                Lecture lec = env.lectures[i];
                lecPenalty += lec.preferences.getOrDefault(slotId, 0);
            }
        }

        // Sum of tutorial penalites
        for (int i = 0; i < pr.tutorials.length; i++) {
            int slotId = pr.tutorials[i];

            //skip unassigned
            if (slotId >= 0) {
                Tutorial tut = env.tutorials[i];
                tutPenalty += tut.preferences.getOrDefault(slotId, 0);
            }
        }

        return lecPenalty + tutPenalty;
    }

    
    private static int EvalPair(Problem pr, Environment env)
    {
        // variables for accumulating
        int penalty = 0;
        int a;
        int b;
        // go through each pair
        for (Pair pair : env.pairs) {
            // if first item is an assigned lecture, then set a accordingly
            if (pair.is_lec1) {
                a = pr.lectures[pair.id1];
            } // otherwise its an assigned tutorial and we set it accordingly as well
            else {
                a = pr.tutorials[pair.id1];
            } // repeat same process for 2nd item
            if (pair.is_lec2) {
                b = pr.lectures[pair.id2];
            }
            else {
                b = pr.tutorials[pair.id2];
            }
            // check to see if a or b is actually assigned by checking if its value is >= 0, then also check if a is not equal to b
            // assign(a) != assign(b) -> apply penalty
            if (a >=0 && b >= 0 && a != b) {
                penalty += env.pen_notpaired;
            }
            // if both in same slot or one unassigned then no penalty is added to the accumulator, assign(a) == assign(b) -> penalty = 0
        }

        return penalty;
    }

    private static int EvalSecDiff(Problem pr, Environment env)
    {
        return 0;
    }

    /**
     * ValidLectureSlots finds all valid slots for a given lecture
     * @param env the environment
     * @param lec_id the unique id of the lecture 
     * @param pr the problem that has the existing lecture and tutorial assignments
     * @return an array of unique lecture slot id's
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

        // get the information about the lecture
        Lecture lecture = env.lectures[lec_id];
        // this hashset will store the indices of all the slots that are not valid
        HashSet<Integer> slot_mask = new HashSet<Integer>();

        // Find over capacity slots ####################################################################################################################
        // initialize an array for holding the slots fill values
        int[] slot_fill = new int[env.lec_slots_array.length];
        // initialize an array for holding the al slots fill values
        int[] slot_al_fill = new int[env.lec_slots_array.length];

        // ### TODO ###: replace this fill counting by recording the fill values in the Problem class
        for(int i = 0; i < slot_fill.length; i++)
        {
            slot_fill[i] = 0;
            slot_al_fill[i] = 0;
        }

        // count the number of elements in each slot
        for(int i = 0; i < pr.lectures.length; i++)
        {
            if(pr.lectures[i] != -1)
            {
                slot_fill[pr.lectures[i]]++;
                if(env.lectures[i].is_al)
                {
                    slot_al_fill[pr.lectures[i]]++;
                }
            }
        }

        // if the slot is at capacity then add it to the mask
        for(int i = 0; i < slot_fill.length; i++)
        {
            if(slot_fill[i] >= env.lec_slots_array[i].max)
            {
                // this lecture slot does not have enough spaces for this lecture
                slot_mask.add(i);
                continue;
            }
            else if(lecture.is_al && (slot_al_fill[i] >= env.lec_slots_array[i].almax))
            {
                // this lecture slot does not have enough active learning spaces for this active learning lecture
                slot_mask.add(i);
                continue;
            }

            if(lecture.is_evng && !env.lec_slots_array[i].is_evng)
            {
                // this is not an evening lecture slot
                slot_mask.add(i);
            }
        }
    
        // find slots of the corresponding tutorials ##########################################################################################################
        for(int i = 0; i < lecture.tutorials.length; i++)
        {
            // the id of the tutorial
            int id = pr.tutorials[lecture.tutorials[i]];
            if(id != -1)
            {
                // add the ids of the lectures that overlap this tutorial
                for(int j = 0; j < env.tutslot_lecslot[id].length; j++)
                    {
                        slot_mask.add(env.tutslot_lecslot[id][j]);
                    }
            }            
        }

        // find not compatible slot assignments ######################################################################################################################
        // loop through the not compatible lectures/tutorials and get their slot assignments
        for(Integer lec: lecture.not_compatible_lec)
        {
            // lec is the id of the not compatible lecture, use this to get the slot assigned to lec
            int slot_id = pr.lectures[lec];
            if(slot_id != -1)
            {
                // add the id of this slot
                slot_mask.add(slot_id);
            }
        }

        for(Integer tut: lecture.not_compatible_tut)
        {
            // the id of the tutorial
            int id = pr.tutorials[tut];
            if(id != -1)
            {
                // add the ids of the lectures that overlap this tutorial
                for(int j = 0; j < env.tutslot_lecslot[id].length; j++)
                {
                    slot_mask.add(env.tutslot_lecslot[id][j]);
                }
            }            
        }

        // find the Unwanted slots #################################################################################################################################
        for(Integer slot_id: lecture.unwanted)
        {
            slot_mask.add(slot_id);
        }
        
        // find other 5xx level lectures if this is a 5xx level lecture ############################################################################################################################
        if(lecture.is_5xx)
        {
            for(int i = 0; i < env.lectures_5xx.length; i++)
            {
                int id = pr.lectures[env.lectures_5xx[i]];
                if(id != -1)
                {
                    slot_mask.add(id);
                }
            }
        
        }
        
        // filter the array list
        int num_slots = env.lec_slots_array.length - slot_mask.size();
        if(num_slots <= 0)
        {
            return null;
        }

        int[] valid_slots = new int[num_slots];
        int j = 0;
        for(int i = 0; i < env.lec_slots_array.length; i++)
        {
            if(!slot_mask.contains(i))
            {
                valid_slots[j] = i;
                j++;
            }
        }

        return valid_slots;
    }

    /**
     * print the list of lecture slots
     * @param ids the ids to print
     * @param env the environment variables
     */
    public static void PrintLectureSlots(int[] ids, Environment env)
    {
        if(ids == null)
        {
            return;
        }
        System.out.println("Lecture slots:");
        for(int i = 0; i < ids.length; i++)
        {
            System.out.print("\t");
            env.lec_slots_array[ids[i]].PrintSlot();
        }
    }

    /**
     * print the list of tutorial slots
     * @param ids the ids to print
     * @param env the environment variables
     */
    public static void PrintTutorialSlots(int[] ids, Environment env)
    {
        if(ids == null)
        {
            return;
        }
        System.out.println("Tutorial slots:");
        for(int i = 0; i < ids.length; i++)
        {
            System.out.print("\t");
            env.tut_slots_array[ids[i]].PrintSlot();
        }
    }

    /**
     * ValidTutSlots finds all valid slots for a given tutorial
     * @param env the environment
     * @param tut_id the unique tutorial id of the tutorial
     * @param pr the problem that has the existing lecture and tutorial assignments
     * @return an array of unique tutorial slot id's
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
        // get the information about the lecture

        Tutorial tutorial = env.tutorials[tut_id];
        // this hashset will store the indices of all the slots that are not valid
        HashSet<Integer> slot_mask = new HashSet<Integer>();

        // Find over capacity slots ####################################################################################################################
        // initialize an array for holding the slots fill values
        int[] slot_fill = new int[env.tut_slots_array.length];
        // initialize an array for holding the al slots fill values
        int[] slot_al_fill = new int[env.tut_slots_array.length];

        // ### TODO ###: replace this fill counting by recording the fill values in the Problem class
        for(int i = 0; i < slot_fill.length; i++)
        {
            slot_fill[i] = 0;
            slot_al_fill[i] = 0;
        }

        // count the number of elements in each slot
        for(int i = 0; i < pr.tutorials.length; i++)
        {
            if(pr.tutorials[i] != -1)
            {
                slot_fill[pr.tutorials[i]]++;
                if(env.tutorials[i].is_al)
                {
                    slot_al_fill[pr.tutorials[i]]++;
                }
            }
        }

        // if the slot is at capacity then add it to the mask
        for(int i = 0; i < slot_fill.length; i++)
        {
            if(slot_fill[i] >= env.tut_slots_array[i].max)
            {
                // this lecture slot does not have enough spaces for this lecture
                slot_mask.add(i);
                continue;
            }
            else if(tutorial.is_al && (slot_al_fill[i] >= env.tut_slots_array[i].almax))
            {
                // this lecture slot does not have enough active learning spaces for this active learning lecture
                slot_mask.add(i);
                continue;
            }

            if(tutorial.is_evng && !env.tut_slots_array[i].is_evng)
            {
                // this is not an evening lecture slot
                slot_mask.add(i);
            }
        }
    
        // find slots of the corresponding lecture ##########################################################################################################

        for(int i = 0; i < tutorial.parent_lectures.length; i++)
        {
            // the id of the lecture slot
            int id = pr.lectures[tutorial.parent_lectures[i]];
            if(id != -1)
            {
                // add the ids of the tutorial slots that overlap this lecture slot
                for(int j = 0; j < env.lecslot_tutslot[id].length; j++)
                    {
                        slot_mask.add(env.lecslot_tutslot[id][j]);
                    }
            }            
        }

        // find not compatible slot assignments
        // loop through the not compatible lectures/tutorials and get their slot assignments
        for(Integer lec: tutorial.not_compatible_lec)
        {
            // lec is the id of the not compatible lecture, use this to get the slot assigned to lec
            int slot_id = pr.lectures[lec];
            if(slot_id != -1)
            {
                // get the overlapping tutorail slots for this lecture slot
                for(int i = 0; i < env.lecslot_tutslot[slot_id].length; i++)
                {
                    slot_mask.add(env.lecslot_tutslot[slot_id][i]);
                }
            }
        }

        for(Integer tut: tutorial.not_compatible_tut)
        {
            // the id of the tutorial
            int id = pr.tutorials[tut];
            if(id != -1)
            {
                // add the id of the overlapping tutorial
                slot_mask.add(id);
            }            
        }

        // find the Unwanted slots #################################################################################################################################
        for(Integer slot_id: tutorial.unwanted)
        {
            slot_mask.add(slot_id);
        }

        // filter the array list
        int num_slots = env.tut_slots_array.length - slot_mask.size();
        if(num_slots <= 0)
        {
            return null;
        }

        int[] valid_slots = new int[num_slots];
        int j = 0;
        for(int i = 0; i < env.tut_slots_array.length; i++)
        {
            if(!slot_mask.contains(i))
            {
                valid_slots[j] = i;
                j++;
            }
        }

        return valid_slots;
    }

    /**
     * prints the assignments of lectures and tutorials to slots in a nice format
     * @param pr the problem to print
     * @param env the environment variables
     */
    public static void PrintProblem(Problem pr, Environment env)
    {
        // create an array for holding the data
        ArrayList<LectureFormat> output = new ArrayList<LectureFormat>();

        // parse through the problem to get the assignments
        for(int i = 0; i < env.num_lectures; i++)
        {
            LectureFormat temp_lec = new LectureFormat();
            // get the slot string
            if(pr.lectures[i] == -1)
            {
                continue;
            }
            temp_lec.slot = env.lec_slots_array[pr.lectures[i]].name;
            // get the lecture string
            temp_lec.lecture = env.lectures[i].name;
            // get all the tutorials associated with this lecture
            for(int j = 0; j < env.lectures[i].tutorials.length; j++)
            {
                TutorialFormat temp_tut = new TutorialFormat();
                int tut_id = env.lectures[i].tutorials[j];
                // get the tutorial name
                temp_tut.tutorial = env.tutorials[tut_id].name;
                if(pr.tutorials[tut_id] != -1)
                {
                    // get the slot name
                    temp_tut.slot = env.tut_slots_array[pr.tutorials[tut_id]].name;
                }

                temp_lec.tutorials.add(temp_tut);
            }

            Collections.sort(temp_lec.tutorials, new TutorialSorter());

            output.add(temp_lec);
        }

        Collections.sort(output, new LectureSorter());

        for(int i = 0; i < output.size(); i++)
        {
            String temp = String.format("%-23s: %s", output.get(i).lecture,output.get(i).slot);
            System.out.println(temp);

            for(int j =0; j < output.get(i).tutorials.size(); j++)
            {
                temp = String.format("%-23s: %s", output.get(i).tutorials.get(j).tutorial,output.get(i).tutorials.get(j).slot);
                System.out.println(temp);
            }
        }
    } 
}

class LectureSorter implements Comparator<LectureFormat>
{
    /**
     * implements the sorting function for comparator
     * @param a the first object to be compared.
     * @param b the second object to be compared.
     * @return : 1 if a should appear above b, 0 if they are equal, -1 otherwise
     */
    public int compare(LectureFormat a, LectureFormat b)
    {
        // sort on the name 
        return a.lecture.compareTo(b.lecture);
    }
}

class TutorialSorter implements Comparator<TutorialFormat>
{
    /**
     * implements the sorting function for comparator
     * @param a the first object to be compared.
     * @param b the second object to be compared.
     * @return : 1 if a should appear above b, 0 if they are equal, -1 otherwise
     */
    public int compare(TutorialFormat a, TutorialFormat b)
    {
        // sort on the name 
        return a.tutorial.compareTo(b.tutorial);
    }
}

/**
 * for holding the information needed to print out the assignments
 */
class LectureFormat
{
    // the array of tutorials associated with this lecture
    ArrayList<TutorialFormat> tutorials = new ArrayList<TutorialFormat>();
    // the name of the lecture
    String lecture = "";
    // the name of the slot
    String slot = "";
}

/**
 * for holding the information needed to print out the assignments
 */
class TutorialFormat
{
    // the name of this tutorial
    String tutorial = "";
    // the name of this slot
    String slot = "";
}