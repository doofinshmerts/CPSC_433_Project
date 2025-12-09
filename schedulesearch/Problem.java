package schedulesearch;

/**
 * Problem class holds the information needed to describe a problem instance
 */
public class Problem
{
    // the lecture map is 
    int[] lectures; // the assignments of lectures to slots, index is lecture id, value is slot id
    int[] tutorials; // the assignments of tutorials to slots, index is tutorial id, value is slot id
    int[] tut_slot_fill; // the number of assignments to each slot
    int[] tut_al_slot_fill; // the number of active learning assignments to each slot
    int[] lec_slot_fill; // the number of assignments to each slot
    int[] lec_al_slot_fill; // the number of active learning assignments to each slot

    int depth;
    int score;
    int min_score; // keep track this it does not have to be re-calculated over and over
    
    int last_selection; // the index in the environment list of sorted lectures and tutorials that was selected for the last assignment

    public Problem()
    {
    }

    /**
     * copy constructor
     */
    public Problem(Problem prob)
    {
        this.lectures = new int[prob.lectures.length];
        this.tutorials = new int[prob.tutorials.length];
        this.lec_slot_fill = new int[prob.lec_slot_fill.length];
        this.tut_slot_fill = new int[prob.tut_slot_fill.length];
        this.lec_al_slot_fill = new int[prob.lec_slot_fill.length];
        this.tut_al_slot_fill = new int[prob.tut_slot_fill.length];
        
        int k = 0;
        for(int i : prob.lectures)
        {
            this.lectures[k] = i;
            k++;
        }

        k = 0;
        for(int i : prob.tutorials)
        {
            this.tutorials[k] = i;
            k++;
        }

        for(int i = 0; i < lec_slot_fill.length; i++)
        {
            lec_slot_fill[i] = prob.lec_slot_fill[i];
        }

        for(int i = 0; i < tut_slot_fill.length; i++)
        {
            tut_slot_fill[i] = prob.tut_slot_fill[i];
        }

        for(int i = 0; i < lec_slot_fill.length; i++)
        {
            lec_al_slot_fill[i] = prob.lec_al_slot_fill[i];
        }

        for(int i = 0; i < tut_slot_fill.length; i++)
        {
            tut_al_slot_fill[i] = prob.tut_al_slot_fill[i];
        }

        this.depth = prob.depth;
        this.score = prob.score;
        this.last_selection = prob.last_selection;
    }

    /**
     * Assign a lecture to a slot
     * @param lec_id the id of the lecture to assign
     * @param slot_id the id of the slot to assign the lecture to
     */
    public void AssignLecture(int lec_id, int slot_id, boolean _al)
    {
        if(!(lec_id < lectures.length))
        {
            System.out.println("lecture id passed to assign lecture is larger than the number of lectures, something is very wrong");
        }

        lectures[lec_id] = slot_id;
        lec_slot_fill[slot_id]++; // increment the number of assignments in this slot
        depth += 1; // add 1 to the depth on every assignment

        if(_al)
        {
            // increment the number of active learning slots if this is an active learning tutorial
            lec_al_slot_fill[slot_id]++;
        }
    }

    /**
     * Assign a tutorial to a slot
     * @param tut_id the id of the tutorial to assign
     * @param slot_id the id of the slot to assign the tutorial to
     */
    public void AssignTutorial(int tut_id, int slot_id, boolean _al)
    {

        if(!(tut_id < tutorials.length))
        {
            System.out.println("tutorial id passed to assign tutorial is larger than the number of tutorials, something is very wrong");
        }

        tutorials[tut_id] = slot_id;
        tut_slot_fill[slot_id]++; // increment the number of assignments in this slot
        depth += 1; // add 1 to the depth on every assignment

        if(_al)
        {
            // increment the number of active learn'in slots if this is an active learning tutorial
            tut_al_slot_fill[slot_id]++;
        }
    }

    /**
     * retruns true if all lectrues and tutorials have been assigned a slot
     */
    public boolean AllAssigned()
    {
        int count = lectures.length + tutorials.length;
        if(count <= depth)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * initialize the problem with a given number of lectures and tutorials to keep track of
     * @param num_lectures the number of lectures to track
     * @param num_tutorials the number of tutorials to track
     * @param num_lec_slots the number of lectures slots
     * @param num_tut_slots the number of tutorial slots
     */
    public void SetupProblem(int num_lectures, int num_tutorials, int num_lec_slots, int num_tut_slots)
    {
        lectures = new int[num_lectures];
        tutorials = new int[num_tutorials];
        lec_slot_fill = new int[num_lec_slots];
        tut_slot_fill = new int[num_tut_slots];
        lec_al_slot_fill = new int[num_lec_slots];
        tut_al_slot_fill = new int[num_tut_slots];

        depth = 0;
        score = 200000000;
        min_score = 200000000;
        last_selection = -1;

        for(int i = 0; i < num_lectures; i++)
        {
            lectures[i] = -1;
        }

        for(int i = 0; i < num_tutorials; i++)
        {
            tutorials[i] = -1;
        }

        for(int i = 0; i < num_lec_slots; i++)
        {
            lec_slot_fill[i] = 0;
        }

        for(int i = 0; i < num_tut_slots; i++)
        {
            tut_slot_fill[i] = 0;
        }


        for(int i = 0; i < num_lec_slots; i++)
        {
            lec_al_slot_fill[i] = 0;
        }

        for(int i = 0; i < num_tut_slots; i++)
        {
            tut_al_slot_fill[i] = 0;
        }
    }

    /**
     * check to see if the problem has any lectures that have not been assigned
     * @return true if all lectures in lectures[] have a slot value assigned to them, false otherwise
     */
    public boolean UnassignedLectures()
    {
        for(int i = 0; i < lectures.length; i++)
        {
            int assignment = lectures[i];
            if(assignment == -1)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * check to see if the problem has any tutorials that have not been assigned
     * @return true if all tutorials in tutorials[] have a slot value assigned to them, false otherwise
     */
    public boolean UnassignedTutorials()
    {
        for(int i = 0; i < tutorials.length; i++)
        {
            int assignment = tutorials[i];
            if(assignment == -1)
            {
                return true;
            }
        }

        return false;
    }
}