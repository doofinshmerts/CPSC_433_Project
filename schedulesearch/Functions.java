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
     * Pick the next lecture or tutorial to expand on based on which has the least number of slots
     * 
     * @param pr The problem find a transition for
     * @param env The environment the problem is in
     * @param slots The ids of the slots to assign to the selected lecture/tutorial
     * @param selected The id of the lecture or tutorial to assign the slots to
     * @return True if this solution can be satisfied, false if this problem cannot be satisfied (at least one lecture/tutorial has no available slots)
     */
    public static boolean Ftrans(Problem pr, Environment env, int[] slots, LecOrTutId selected)
    {
        int min_so_far = 2000000; // big enough

        // iterate through all lecs in pr
        for (int i = 0; i < pr.lectures.length; i++) {
            // if a lec assignment is null, check if it has any valid slot assignments
            if (pr.lectures[i] == -1) 
            {
                int[] validLecs = ValidLectureSlots(env, i, pr);
                if(validLecs == null)
                {
                    // solution cannot be satisfied
                    return false;
                }
                else if(validLecs.length < min_so_far)
                {
                    // if this is the least number so far then record its slots and id
                    min_so_far = validLecs.length;
                    slots = validLecs;
                    selected.is_lec = true;
                    selected.id = i;
                }
            }
        }

        // iterate through all tutorials in pr
        for (int i = 0; i < pr.tutorials.length; i++) {
            // if a tut assignment is null, check if it has any valid assignments
            if (pr.tutorials[i] == -1) 
            {
                int[] validTuts = ValidTutSlots(env, i, pr);
                if (validTuts == null) 
                {
                    // solution cannot be satisfied
                    return true;
                }
                else if(validTuts.length < min_so_far)
                {
                    // if this is the least number so far then record its slots and id
                    min_so_far = validTuts.length;
                    slots = validTuts;
                    selected.is_lec = false;
                    selected.id = i;
                }
            }
        }
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
        int sum = EvalSecDiff(pr, env) * env.w_secdiff;
        sum += EvalPref(pr, env) * env.w_pref;
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
     * Print the components of the score
     */
    public static void PrintEvaluations(Problem pr, Environment env)
    {
        System.out.println("Min Filled: " + EvalMinFilled(pr,env));
        System.out.println("Pref: " + EvalPref(pr,env));
        System.out.println("Pair: " + EvalPair(pr,env));
        System.out.println("Sec: " + EvalSecDiff(pr,env));
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

        // calculation starts by assuming that all problems have been given their ideal pick,
        // 

        // Sum of lecture penalties
        for (int i = 0; i < pr.lectures.length; i++) {
            int slotId = pr.lectures[i];
            // if unassigned assume top preference
            if(slotId == -1)
            {
                // if slot is not assigned, or this lecture has no preferences, then
                // assume it got it's first choice, first choice is zero if it has no preferences
                lecPenalty += env.lectures[i].first_choice;
            }
            else if (env.lectures[i].preferences.containsKey(slotId)) 
            {
                // if slot is assigned and is a preference then add its value
                lecPenalty += env.lectures[i].preferences.get(slotId);
            }
        }

        // Sum of tutorial penalites
        for (int i = 0; i < pr.tutorials.length; i++) {
            int slotId = pr.tutorials[i];

            // if unassigned assume top preference
            if(slotId == -1)
            {
                // if slot is not assigned, or this tutorial has no preferences, 
                // then assume it got it's first choice, first choice is zero if it has no preferences
                tutPenalty += env.tutorials[i].first_choice;
            }
            else if (env.tutorials[i].preferences.containsKey(slotId))
            {
                // if slot is assigned and is a preference then add its value
                tutPenalty += env.tutorials[i].preferences.get(slotId);
            }
        }

        return (env.total_pref_sum - (lecPenalty + tutPenalty));
        //return lecPenalty + tutPenalty;
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
                //section_penalty += val-1;
            }
        }

        return (section_penalty * env.pen_section);
    }

    /**
     * Ftrans selects the next lecture or tutorial based on the constraint rank 
     * 
     */
    public static LecOrTutId SelectLecTut(Problem pr, Environment env)
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

        // if we need to remove the tuesday at 11:00 lecture slot then add it to the filter of every lecture if it exists
        if(env.remove_tue_11_slot && env.tue_11_slot_id != -1)
        {
            slot_mask.add(env.tue_11_slot_id);
        }

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
        ArrayList<OutputFormat> output = new ArrayList<OutputFormat>();

        // parse through the lectures of the problem and get the lecture names and slot assignments
        for(int i = 0; i < env.num_lectures; i++)
        {
            OutputFormat lec_out = new OutputFormat();

            // get the slot string
            if(pr.lectures[i] == -1)
            {
                continue;
            }
            
            // get the name of the slot
            lec_out.slot_name = env.lec_slots_array[pr.lectures[i]].name;
            // get the name of the lecture
            lec_out.name = env.lectures[i].name;
            // add this to the output array
            output.add(lec_out);
        }

        // parse through the tutorials of the problem and get the tutorial names and slot assignments
        for(int i = 0; i < env.num_tutorials; i++)
        {
            OutputFormat tut_out = new OutputFormat();

            // get the slot string
            if(pr.tutorials[i] == -1)
            {
                continue;
            }
            
            // get the name of the slot
            tut_out.slot_name = env.tut_slots_array[pr.tutorials[i]].name;
            // get the name of the lecture
            tut_out.name = env.tutorials[i].name;
            // add this to the output array
            output.add(tut_out);
        }
    
        // sort the output
        Collections.sort(output, new LecTutSorter());

        for(int i = 0; i < output.size(); i++)
        {
            String temp = String.format("%-23s: %s", output.get(i).name,output.get(i).slot_name);
            System.out.println(temp);
        }
    } 


    // hard constraint compliance

    /**
     * do the slots meet the hard constraints on capacity and active learning capacity
     * @param pr the problem to analyze
     * @param env the environment to analyze
     */
    public static boolean SlotCapacityCompliance(Problem pr, Environment env)
    {
        // determine the number of assignments to each slot
        int[] fill_lec_slots = new int[env.lec_slots_array.length];
        int[] fill_tut_slots = new int[env.tut_slots_array.length];
        int[] fill_lec_slots_al = new int[env.lec_slots_array.length];
        int[] fill_tut_slots_al = new int[env.tut_slots_array.length];

        // initialize the arrays
        for(int i= 0; i < fill_lec_slots.length; i++)
        {
            fill_lec_slots[i] = 0;
            fill_lec_slots_al[i] = 0;
        }

        for(int i = 0; i < fill_tut_slots.length; i++)
        {
            fill_tut_slots[i] = 0;
            fill_tut_slots_al[i] = 0;
        }

        for(int i = 0; i < pr.lectures.length; i++)
        {
            // slot id
            int slot_id = pr.lectures[i];

            // add to the count
            fill_lec_slots[slot_id] += 1;
            // is this an active learning lecture
            if(env.lectures[i].is_al)
            {
                fill_lec_slots_al[slot_id] += 1;
            }
        }


        for(int i = 0; i < pr.tutorials.length; i++)
        {
            // slot id
            int slot_id = pr.tutorials[i];

            // add to the count
            fill_tut_slots[slot_id] += 1;
            // is this an active learning lecture
            if(env.tutorials[i].is_al)
            {
                fill_tut_slots_al[slot_id] += 1;
            }
        }

        // verify that they are not over capacity
        for(int i = 0; i < fill_lec_slots.length; i++)
        {
            Slot s = env.lec_slots_array[i];
            

            if(s.max < fill_lec_slots[i])
            {
                System.out.println(String.format("lec slot: %2d, capacity: %2d, al capacity: %2d, fill %2d, al fill %2d",i, s.max, s.almax, fill_lec_slots[i], fill_lec_slots_al[i]));
                // over flow
                return false;
            }

            if(s.almax < fill_lec_slots_al[i])
            {
                System.out.println(String.format("lec slot: %2d, capacity: %2d, al capacity: %2d, fill %2d, al fill %2d",i, s.max, s.almax, fill_lec_slots[i], fill_lec_slots_al[i]));
                // active learning over flow
                return false;
            }
        }


        for(int i = 0; i < fill_tut_slots.length; i++)
        {
            Slot s = env.tut_slots_array[i];
            

            if(s.max < fill_tut_slots[i])
            {
                System.out.println(String.format("tut slot: %2d, capacity: %2d, al capacity: %2d, fill %2d, al fill %2d",i, s.max, s.almax, fill_tut_slots[i], fill_tut_slots_al[i]));
                // over flow
                return false;
            }

            if(s.almax < fill_tut_slots_al[i])
            {
                System.out.println(String.format("tut slot: %2d, capacity: %2d, al capacity: %2d, fill %2d, al fill %2d",i, s.max, s.almax, fill_tut_slots[i], fill_tut_slots_al[i]));
                // active learning over flow
                return false;
            }
        }

        return true;
    }

    /**
     * check if the lectures do not overlap any of their tutorials
     * @param pr the problem to analyze
     * @param env the environment to analyze
     */
    public static boolean LectureNoOverlapTutorials(Problem pr, Environment env)
    {
        // go through every lecture and see if any of its tutorials have overlapping times
        for(int i = 0; i < pr.lectures.length; i++)
        {
            // get the current lecture
            Lecture lec = env.lectures[i];

            // the slot that the lecture is assigned
            Slot lec_slot = env.lec_slots_array[pr.lectures[i]];

            // for each child tutorial check its assignment
            for(int j = 0; j < lec.tutorials.length; j++)
            {
                // get the assigned slot of this tutorial (look up the tutorial id using j, then look up the slot id using the tutorial id)
                Slot tut_slot = env.tut_slots_array[pr.tutorials[lec.tutorials[j]]];

                if(AreLecTutSlotsOverlapping(lec_slot, tut_slot))
                {
                    lec.PrintData();
                    lec_slot.PrintSlot();
                    System.out.println("above lecture overlaps with tutorial: " + env.tutorials[lec.tutorials[j]].name);
                    tut_slot.PrintSlot();
                    // overlap found, return false
                    return false;
                }
            }
        }

        // no overlaps found return true
        return true;
    }


    /**
     * are a lecture and tutorial slot overlapping
     * @param lec_slot the lecture slot
     * @param tut_slot the tutorial slot
     */
    private static boolean AreLecTutSlotsOverlapping(Slot lec_slot, Slot tut_slot)
    {
        // cases are the days Monday, Tuesday, Friday
        if((tut_slot.day == 0) || (tut_slot.day == 2))
        {
            // monday
            // moday case lecture and tutorial slots a one hour long
            if(lec_slot.lec_hash == tut_slot.lec_hash)
            {
                return true;
            }
        }
        else if((tut_slot.day == 1) || (tut_slot.day == 3))
        {
            // tuesday lecture slots are 90 min, tutorial slots are 60 min
            if(((lec_slot.lec_hash + 90) > tut_slot.lec_hash) && ( (lec_slot.lec_hash + 90) <= (tut_slot.lec_hash + 50)) )
            {
                // lecture ends in the middle of a tutorial, thus overlapping
                return true;
            }

            if(((tut_slot.lec_hash + 60) >(lec_slot.lec_hash)) && ((tut_slot.lec_hash + 60) <= (lec_slot.lec_hash + 90)))
            {
                // tutorial end in the middle of a lecture, thus overlapping
                return true;
            }
        }
        else
        {
            // TGIF
            // tuesday friday lecture slots are 60 min, tutorial slots are 120 min
            if(((lec_slot.lec_hash + 60) > tut_slot.lec_hash) && ( (lec_slot.lec_hash + 60) <= (tut_slot.lec_hash + 120)) )
            {
                // lecture ends in the middle of a tutorial, thus overlapping
                return true;
            }

            if(((tut_slot.lec_hash + 120) >(lec_slot.lec_hash)) && ((tut_slot.lec_hash + 120) <= (lec_slot.lec_hash + 60)))
            {
                // tutorial end in the middle of a lecture, thus overlapping
                return true;
            }

        }

        return false;
    }

    /**
     * check that there are no incompatible lectures/tutorials assigned to the same slots
     * @param pr the problem to check
     * @param env the environment to use
     */
    public static boolean NotCompatibleCheck(Problem pr, Environment env)
    {
        // go through every lecture and check that it does not overlap a not compatible lecture/tutorial
        for(int i = 0; i < pr.lectures.length; i++)
        {
            // get the current lecture
            Lecture lec = env.lectures[i];

            // the slot that the lecture is assigned
            Slot lec_slot = env.lec_slots_array[pr.lectures[i]];

            // for each not compatible lecture
            for(Integer l: lec.not_compatible_lec)
            {
                // get the assigned slot of this lecture
                int l_slot = pr.lectures[l];

                if(lec_slot.id == l_slot)
                {
                    lec.PrintData();
                    lec_slot.PrintSlot();
                    System.out.println("above lecture overlaps with lecture: " + env.lectures[l].name);
                    env.lectures[l].PrintData();
                    env.lec_slots_array[l_slot].PrintSlot();
                    // overlap found, return false
                    return false;
                }
            }

            // for each not compatible tutorial
            for(Integer t: lec.not_compatible_tut)
            {
                // get the assigned slot
                Slot s = env.tut_slots_array[pr.tutorials[t]];

                if(AreLecTutSlotsOverlapping(lec_slot, s))
                {
                    lec.PrintData();
                    lec_slot.PrintSlot();
                    System.out.println("above lecture overlaps with tutorial: ");
                    env.tutorials[t].PrintData();
                    s.PrintSlot();
                    // overlap found, return false
                    return false;
                }
            }
        }

        // go through every tutorial and check that it does not overlap a not compatible tutorial/lecture
        for(int i = 0; i < pr.tutorials.length; i++)
        {
            // get the current tutorial
            Tutorial tut = env.tutorials[i];

            // the slot that the tutorial is assigned
            Slot tut_slot = env.tut_slots_array[pr.tutorials[i]];

            // for each not compatible lecture
            for(Integer l: tut.not_compatible_lec)
            {
                // get the assigned slot of this lecture
                Slot l_slot = env.lec_slots_array[pr.lectures[l]];

                if(AreLecTutSlotsOverlapping(l_slot, tut_slot))
                {
                    tut.PrintData();
                    tut_slot.PrintSlot();
                    System.out.println("above tutorial overlaps with lecture: ");
                    env.lectures[l].PrintData();
                    l_slot.PrintSlot();
                    // overlap found, return false
                    return false;
                }
            }

            // for each not compatible tutorial
            for(Integer t: tut.not_compatible_tut)
            {
                // get the assigned slot
                int s = pr.tutorials[t];

                if(tut_slot.id == s)
                {
                    tut.PrintData();
                    tut_slot.PrintSlot();
                    System.out.println("above tutorial overlaps with tutorial: ");
                    env.tutorials[t].PrintData();
                    env.tut_slots_array[s].PrintSlot();
                    // overlap found, return false
                    return false;
                }
            }
        }


        // not conflicts found return true
        return true;
    }

    /**
     * check that there are no lectures/tutorials assigned to unwanted slots
     * @param pr the problem to check
     * @param env the environment to use
     */
    public static boolean UnwantedCheck(Problem pr, Environment env)
    {
        // go through every lecture and check that it is not assigned an unwanted slot
        for(int i = 0; i < pr.lectures.length; i++)
        {
            // get the current lecture
            Lecture lec = env.lectures[i];

            // the slot that the lecture is assigned
            int lec_slot = pr.lectures[i];

            // for each unwanted, ensure it is not the assigned slot
            for(Integer unwanted: lec.unwanted)
            {
                if(unwanted == lec_slot)
                {
                    // overlap found, return false
                    return false;
                }
            }
        }


        // go through every lecture and check that it is not assigned an unwanted slot
        for(int i = 0; i < pr.tutorials.length; i++)
        {
            // get the current lecture
            Tutorial tut = env.tutorials[i];

            // the slot that the lecture is assigned
            int tut_slot = pr.tutorials[i];

            // for each unwanted, ensure it is not the assigned slot
            for(Integer unwanted: tut.unwanted)
            {
                if(unwanted == tut_slot)
                {
                    // overlap found, return false
                    return false;
                }
            }
        }

        return true;
    }



}

class LecTutSorter implements Comparator<OutputFormat>
{
    /**
     * implements the sorting function for comparator
     * @param a the first object to be compared.
     * @param b the second object to be compared.
     * @return : 1 if a should appear above b, 0 if they are equal, -1 otherwise
     */
    public int compare(OutputFormat a, OutputFormat b)
    {
        // sort on the name 
        return a.name.compareTo(b.name);
    }
}

/**
 * for holding the information needed to print out the assignments
 */
class OutputFormat
{
    // the name of this lecture or Tutorial
    String name = "";
    // the name of this slot
    String slot_name = "";
}
