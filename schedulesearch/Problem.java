package schedulesearch;

/**
 * Problem class holds the information needed to describe a problem instance
 */
public class Problem
{
    // the lecture map is 
    int[] lectures; // the assignments of lectures to slots, index is lecture id, value is slot id
    int[] tutorials; // the assignments of tutorials to slots, index is tutorial id, value is slot id
    int depth;
    int score;

    public Problem()
    {
    }

    public Problem(Problem prob)
    {
        this.lectures = new int[prob.lectures.length];
        this.tutorials = new int[prob.tutorials.length];
        
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

        this.depth = prob.depth;
        this.score = prob.score;
    }

    /**
     * Assign a lecture to a slot
     * @param lec_id the id of the lecture to assign
     * @param slot_id the id of the slot to assign the lecture to
     */
    public void AssignLecture(int lec_id, int slot_id)
    {
        if(!(lec_id < lectures.length))
        {
            System.out.println("lecture id passed to assign lecture is larger than the number of lectures, something is very wrong");
        }

        lectures[lec_id] = slot_id;
    }

    /**
     * Assign a tutorial to a slot
     * @param tut_id the id of the tutorial to assign
     * @param slot_id the id of the slot to assign the tutorial to
     */
    public void AssignTutorial(int tut_id, int slot_id)
    {

        if(!(tut_id < tutorials.length))
        {
            System.out.println("tutorial id passed to assign tutorial is larger than the number of tutorials, something is very wrong");
        }
        tutorials[tut_id] = slot_id;
    }

    /**
     * initialize the problem with a given number of lectures and tutorials to keep track of
     * @param num_lectures the number of lectures to track
     * @param num_tutorials the number of tutorials to track
     */
    public void SetupProblem(int num_lectures, int num_tutorials)
    {
        lectures = new int[num_lectures];
        tutorials = new int[num_tutorials];
        depth = 0;
        score = 0;

        for(int i = 0; i < num_lectures; i++)
        {
            lectures[i] = -1;
        }

        for(int i = 0; i < num_tutorials; i++)
        {
            tutorials[i] = -1;
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