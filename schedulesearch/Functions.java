package schedulesearch;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

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
        // iterate through all lecs in pr
        for (int i = 0; i < pr.lectures.length; i++) {
            // if a lec assignment is null, check if it has any valid slot assignments
            if (pr.lectures[i] == -1) {
                int[] validLecs = ValidLectureSlots(env, i, pr);
                // if there are valid slot assignments, solution can still be expanded
                if (validLecs != null && validLecs.length > 0) {
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
                if (validTuts != null && validTuts.length > 0) {
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
        int sum = 0;
        for(Pair pair : env.pairs)
        {
            int slot1 = -1;
            int slot2 = -1;

            if(pair.is_lec1)
            {
                slot1 = pr.lectures[pair.id1];
            }
            else
            {
                slot1 = pr.tutorials[pair.id1];
            }

            if(pair.is_lec2)
            {
                slot2 = pr.lectures[pair.id2];
            }
            else
            {
                slot2 = pr.tutorials[pair.id2];
            }

            // If both are assigned and not to the same slot, add penalty
            if(slot1 != -1 && slot2 != -1 && slot1 != slot2)
            {
                sum += env.pen_notpaired;
            }
        }
        return sum;
    }

    private static int EvalSecDiff(Problem pr, Environment env)
    {
        int sum = 0;
        for(Integer[] section : env.sections.values())
        {
            for(int i = 0; i < section.length; i++)
            {
                for(int j = i + 1; j < section.length; j++)
                {
                    int slot1 = pr.lectures[section[i]];
                    int slot2 = pr.lectures[section[j]];

                    // If both are assigned to the same slot, add penalty
                    if(slot1 != -1 && slot2 != -1 && slot1 == slot2)
                    {
                        sum += env.pen_section;
                    }
                }
            }
        }
        return sum;
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
        // get the information about the lecture
        Lecture lecture = env.lectures[lec_id];
        // this hashset will store the indices of all the slots that are not valid
        HashSet<Integer> slot_mask = new HashSet<Integer>();

        // Find over capacity slots
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
            // Hard Constraint: No lectures on Tuesdays 11:00-12:30
            // Tuesday is day 1. 11:00 is 660 mins. 12:30 is 750 mins.
            int startTime = env.lec_slots_array[i].hour * 60 + env.lec_slots_array[i].minute;
            if (env.lec_slots_array[i].day == 1 && startTime >= 660 && startTime < 750)
            {
                slot_mask.add(i);
                continue;
            }

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
    
        // find slots of the corresponding tutorials
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

        // find not compatible slot assignments
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

        // find the Unwanted slots
        for(Integer slot_id: lecture.unwanted)
        {
            slot_mask.add(slot_id);
        }
        
        // find other 5xx level lectures if this is a 5xx level lecture
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
        Tutorial tutorial = env.tutorials[tut_id];
        // this hashset will store the indices of all the slots that are not valid
        HashSet<Integer> slot_mask = new HashSet<Integer>();

        // Find over capacity slots
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
    
        // find slots of the corresponding lecture
        // get the slot of the parent lecture
        int lec_slot = pr.lectures[tutorial.lec_id];
        if(lec_slot != -1)
        {
            // get the overlapping tutorial slots for this lecture slot
            for(int i = 0; i < env.lecslot_tutslot[lec_slot].length; i++)
            {
                slot_mask.add(env.lecslot_tutslot[lec_slot][i]);
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

        // find the Unwanted slots
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
    /**
     * Calculates the rank of a lecture based on its constraints.
     * Higher rank means it should be assigned earlier.
     */
    public static int ConstraintRank(Lecture l, Environment env) {
        int rank = 0;
        // 1. Evening lectures (highest priority)
        if (l.is_evng) rank += 100000;
        
        // 2. Active Learning
        if (l.is_al) rank += 10000;
        
        // 3. 5XX Level
        if (l.is_5xx) rank += 1000;
        
        // 4. Number of constraints
        int constraints = l.not_compatible_lec.size() + l.not_compatible_tut.size() + l.unwanted.size();
        rank += constraints;
        
        return rank;
    }

    /**
     * Calculates the rank of a tutorial based on its constraints.
     */
    public static int ConstraintRank(Tutorial t, Environment env) {
        int rank = 0;
        // 1. Evening
        if (t.is_evng) rank += 100000;
        
        // 2. Active Learning
        if (t.is_al) rank += 10000;
        
        // 4. Number of constraints
        int constraints = t.not_compatible_lec.size() + t.not_compatible_tut.size() + t.unwanted.size();
        rank += constraints;
        
        return rank;
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