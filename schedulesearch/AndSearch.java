package schedulesearch;
import java.util.PriorityQueue;
import java.util.Comparator;

/**
 * AndSearch class runs the and search to solve the constraint satisfaction problem
 */
public class AndSearch
{
    // the environment
    Environment env;
    // the tree in the form of a priority queue (next leaf to expand on top)
    PriorityQueue<Problem> tree = new PriorityQueue<Problem>(10, new FLeafComparator());

    /**
     * initialization funciton for the AndSearch
     * @param _env the environment
     * @param _s0 the starting state
     */
    public AndSearch(Environment _env, Problem _s0)
    {
        env = _env;
        tree.add(_s0);
    }

    /**
     * run the and tree search
     * @param sf the final state to return
     * @return true if a solution could be found, otherwise false
     */
    public boolean RunSearch(Problem sf)
    {
        // sudo code:
        // get the problem from the top of the priority queue "tree"
        // use ftrans to select the transition 
        // use div to get the new problems
        // push the new problems onto the priority queue "tree"

        Problem top_problem = tree.poll();
        if(Functions.Solvable(top_problem))
        {
            // If the current problem is has sol = yes, no unassigned lectures or tutorials,
            // and is a better solution than the previous, replace the previous solution with
            // the current one
            if(!top_problem.UnassignedLectures() && !top_problem.UnassignedTutorials() && top_problem.score < env.best_score)
            {
                env.best_score = top_problem.score;
                env.best_sol = top_problem;
            }
        }
        
        // If this problem can be pruned, then it is removed from the tree
        else if(Functions.FBound(top_problem, env))
        {
            
        }

        // Otherwise, create new problem derivations by adding the next lecture/tutorial to all possible valid slots
        else
        {
            // Get the first lecture in env.lectures that has not been assigned
            Lecture unassigned_lecture = null;
            for(Lecture lec : env.lectures)
            {
                if (top_problem.lectures[lec.id] == -1)
                {
                    unassigned_lecture = lec;
                    break;
                }
            }

            // If an unassigned lecture was found, add all problem derivations that can stem from adding it to top_problem
            if(unassigned_lecture != null)
            {
                int[] valid_slots = Functions.ValidLectureSlots(env, unassigned_lecture.id, top_problem);
                for(int slot : valid_slots)
                {
                    Problem new_problem = new Problem(top_problem);
                    new_problem.AssignLecture(unassigned_lecture.id, slot);
                    new_problem.depth++;
                    new_problem.score = Functions.Eval(new_problem, env);
                    tree.add(new_problem);
                }
            }

            // Get the first tutorial in env.lectures that has not been assigned
            Tutorial unassigned_tutorial = null;
            for(Tutorial tut : env.tutorials)
            {
                if (top_problem.lectures[tut.id] == -1)
                {
                    unassigned_tutorial = tut;
                    break;
                }
            }

            // If an unassigned tutorial was found, add all problem derivations that can stem from adding it to top_problem
            if(unassigned_tutorial != null)
            {
                int[] valid_slots = Functions.ValidTutSlots(env, unassigned_tutorial.id, top_problem);
                for(int slot : valid_slots)
                {
                    Problem new_problem = new Problem(top_problem);
                    new_problem.AssignTutorial(unassigned_tutorial.id, slot);
                    new_problem.depth++;
                    new_problem.score = Functions.Eval(new_problem, env);
                    tree.add(new_problem);
                }
            }
        }
        return false;
    }
}

/**
 * FLeafComparator is used to sort the Problems in the priority queue
 */
class FLeafComparator implements Comparator<Problem>
{
    /**
     * This methode implements the f_leaf function to sort problems p1 and p2
     * @param p1 the first problem to sort
     * @param p2 the second problem to sort
     * @return negative if the first object should go first in the list, positive otherwise
     */ 
    public int compare(Problem p1, Problem p2)
    {
        // sudo code:
        // sort on the following priority
        // 1: solvable nodes go first
        // 2: deepest nodes go first
        // 3: lowest score according to MinBoundScore go first
        // 4: tie break on problem unique id 

        // Sort by solvable
        boolean p1_solvable = Functions.Solvable(p1);
        boolean p2_solvable = Functions.Solvable(p2);

        if (p1_solvable && !p2_solvable)
        {
            return -1;
        }
        if (!p1_solvable && p2_solvable)
        {
            return 1;
        }

        // Sort by depth
        if (p1.depth > p2.depth)
        {
            return -1;
        }
        if (p1.depth < p2.depth)
        {
            return 1;
        }

        // Sort by MinBoundScore
        // ### TODO ###: Replace minboundscore with Kevlam's MinBoundScore() for Problem.java
        // if (p1.minboundscore < p2.minboundscore)
        // {
        //     return -1;
        // }
        // if (p1.minboundscore > p2.minboundscore)
        // {
        //     return 1;
        // }

        // Sort by id
        if (p1.hashCode() > p2.hashCode())
        {
            return 1;
        }
        else
        {
            return -1;
        }
    }
}