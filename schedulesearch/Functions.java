package schedulesearch;
import java.util.HashSet;
import java.util.HashMap;
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
     * Solvable determines if problem pr is solvable in the 
     * @param pr the problem to check
     * @param env the environment
     * @return true if pr is solvable, false otherwise
     */ 
    public static boolean NotSatisfiable(Problem pr, Environment env)
    {
        // sudo code
        // if even a single lecture or tutorial has no valid slot assignments then return ture

        // iterate through all lecs in pr
        for (int i = 0; i < pr.lectures.length; i++) {
            // if a lec assignment is null, check if it has any valid slot assignments
            if (pr.lectures[i] == -1) 
            {
                int[] validLecs = ValidLectureSlots(env, i, pr);
                if(validLecs == null)
                {
                    return true;
                }
            }
        }

        // iterate through all tutorials in pr
        for (int i = 0; i < pr.tutorials.length; i++) {
            // if a tut assignment is null, check if it has any valid assignments
            if (pr.tutorials[i] == -1) 
            {
                int[] validTuts = ValidTutSlots(env, i, pr);
                // if there are valid assignments, solution can be expanded still
                if (validTuts == null) 
                {
                    return true;
                }
            }
        }

        //solution expandable
        return false;
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
        // return the depth count, return depth if we want to use function instead of pr.depth
        return pr.depth;
    }

    /**
     * MinBoundScore calculates the minimum score of a problem that cannot do better than
     * @param pr the problem to evaluate
     * @param env the environment with the penalties and weights
     * @return the score of the problem
     */ 
    public static int MinBoundScore(Problem pr, Environment env)
    {
        // EvalPref and EvalSecDiff are permenant scores that cannot be reduced as more assignments are made
        int sum = EvalPref(pr, env) * env.w_pref;
        sum += EvalSecDiff(pr, env) * env.w_secdiff;
        return sum;
    }

    /**
     * Eval calculates the score of a problem assignment
     * @param pr the problem to score
     * @param env the environment with the scoring parameters
     * @return the score of the problem
     */
    public static int Eval(Problem pr, Environment env)
    {
        int sum = EvalMinFilled(pr, env) * env.w_minfilled;
        sum += EvalPref(pr, env) * env.w_pref;
        sum += EvalPair(pr, env) * env.w_pair;
        sum += EvalSecDiff(pr, env) * env.w_secdiff;
        return sum;
    }

    /**
     * the penalty from the minimum assignment requirements not being filled
     * @param pr the problem to rank
     * @param env the environment data
     */
    private static int EvalMinFilled(Problem pr, Environment env)
    {
        // Variables for lecture penalty and tutorial penalty
        int lec_penalty = 0;
        int tut_penalty = 0;

        // go through each slot and find the differenc between min filled and actual fill
        for(int i = 0; i < pr.lec_slot_fill.length; i++)
        {
            int diff = env.lec_slots_array[i].min - pr.lec_slot_fill[i];

            // this only should be counted if the number filled is less than the minimum required
            if(diff > 0)
            {
                lec_penalty += diff;
            }
        }

        for(int i = 0; i < pr.tut_slot_fill.length; i++)
        {
            int diff = env.tut_slots_array[i].min - pr.tut_slot_fill[i];
            
            // this only should be counted if the number filled is less than the minimum required
            if(diff > 0)
            {
                tut_penalty += diff;
            }
        }

        return (lec_penalty*env.pen_lecturemin + tut_penalty*env.pen_tutorialmin);
    }

    private static int EvalPref(Problem pr, Environment env)
    {
        int lecPenalty = 0;
        int tutPenalty = 0;

        // Sum of lecture penalties
        for (int i = 0; i < pr.lectures.length; i++) {
            int slotId = pr.lectures[i];
            // skip unassigned
            if (env.lectures[i].preferences.containsKey(slotId)) 
            {
                lecPenalty += env.lectures[i].preferences.get(slotId);
            }
        }

        // Sum of tutorial penalites
        for (int i = 0; i < pr.tutorials.length; i++) {
            int slotId = pr.tutorials[i];

            //skip unassigned
            if (env.tutorials[i].preferences.containsKey(slotId))
            {
                tutPenalty += env.tutorials[i].preferences.get(slotId);
            }
        }

        return (env.total_pref_sum - (lecPenalty + tutPenalty));
    }

    
    private static int EvalPair(Problem pr, Environment env)
    {
        // variables for accumulating
        int penalty = 0;
        int a;
        int b;

        // go through each pair
        for (Pair pair : env.pairs) 
        {
            // case1: both are lectures
            if(pair.is_lec1 && pair.is_lec2)
            {
                // because they are both lectures we simply verify that the id is the same
                a = pr.lectures[pair.id1];
                b = pr.lectures[pair.id2];
                if(a >= 0 && (a != b))
                {
                    penalty += env.pen_notpaired;
                }
            }
            // case2: lecture and tutorial
            else if(pair.is_lec1 && !pair.is_lec2)
            {
                // the starting time of a tutorial slot can always be mapped to a single lecture slot 
                a = pr.lectures[pair.id1];
                b = env.tutid_to_lecid[pr.tutorials[pair.id2]];
                if(a >= 0 && (a != b))
                {
                    penalty += env.pen_notpaired;
                }
            }
            // case3; tutorial and lecture
            else if(!pair.is_lec1 && pair.is_lec2)
            {
                // the starting time of a tutorial slot can always be mapped to a single lecture slot 
                a = pr.lectures[pair.id2];
                b = env.tutid_to_lecid[pr.tutorials[pair.id1]];
                if(a >= 0 && (a != b))
                {
                    penalty += env.pen_notpaired;
                }
            }
            // case4; tutorial and tutorial
            else
            {
                // because they are both tutorials we simply verify that the id is the same
                a = pr.tutorials[pair.id1];
                b = pr.tutorials[pair.id2];
                if(a >= 0 && (a != b))
                {
                    penalty += env.pen_notpaired;
                }
            }
        }

        return penalty;
    }

    private static int EvalSecDiff(Problem pr, Environment env)
    {
        int section_penalty = 0;

        // loop over every section and find number of overlapping lectures
        for(int[] lecs : env.sections.values())
        {
            // record which elements we have seen
            HashMap<Integer, Integer> found_slots = new HashMap<Integer, Integer>();

            // loop through the lectures in this section
            for(int i = 0; i < lecs.length; i++)
            {
                // get the slot assigned to this lecture
                int slot_id = pr.lectures[lecs[i]];
                if(slot_id >= 0)
                {
                    // if this slot has been seen before then add to the penalty
                    // otherwise add this slot to the set of found slots
                    if(found_slots.containsKey(slot_id))
                    {
                        // found another occurance of this, so increment by 1
                        int value = found_slots.get(slot_id)+1;
                        found_slots.put(slot_id, value);
                    }
                    else
                    {
                        // new slot
                        found_slots.put(slot_id, 1);
                    }
                }
            }

            // count the number of pairs that violate this
            for(Integer val : found_slots.values())
            {
                // number of unique pairs from n items is n(n-1)/2
                section_penalty += (val*(val-1)) >> 1;
            }
        }

        return (section_penalty * env.pen_section);
    }

    /**
     * Ftrans selects the next lecture or tutorial based on the constraint rank 
     * 
     */
    public static LecOrTutId Ftrans(Problem pr, Environment env)
    {
        // iterate through the sorted array of lectures and tutorials until we find one that has not yet been assigned
        for(int i = (pr.last_selection+1); i < env.constraint_ordering.length; i++)
        {
            // the lecture or tutorial
            LecOrTutId temp = env.constraint_ordering[i];
            // check to see if this lecture/tutorial has been assigned
            if(temp.is_lec)
            {  
                // lecture
                int slot_id = pr.lectures[temp.id];
                if(slot_id == -1)
                {
                    // increment the last selection number to the current index
                    pr.last_selection = i;
                    return temp;

                }

            }
            else
            {
                // tutorial
                int slot_id = pr.tutorials[temp.id];
                if(slot_id == -1)
                {
                    // increment the last selection number to the current index
                    pr.last_selection = i;
                    return temp;
                }
            }
        }

        // something is not right if we get here
        return null;
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
        // if the slot is at capacity then add it to the mask
        for(int i = 0; i < pr.lec_slot_fill.length; i++)
        {
            if(pr.lec_slot_fill[i] >= env.lec_slots_array[i].max)
            {
                // this lecture slot does not have enough spaces for this lecture
                slot_mask.add(i);
                continue;
            }
            else if(lecture.is_al && (pr.lec_al_slot_fill[i] >= env.lec_slots_array[i].almax))
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
        // if the slot is at capacity then add it to the mask
        for(int i = 0; i < pr.tut_slot_fill.length; i++)
        {
            if(pr.tut_slot_fill[i] >= env.tut_slots_array[i].max)
            {
                // this lecture slot does not have enough spaces for this lecture
                slot_mask.add(i);
                continue;
            }
            else if(tutorial.is_al && (pr.tut_al_slot_fill[i] >= env.tut_slots_array[i].almax))
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