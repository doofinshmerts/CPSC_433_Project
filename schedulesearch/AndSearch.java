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

    // how often should status be printed
    int status_update_freq = 10000; // once every x iterations

    // the tree in the form of a priority queue (next leaf to expand on top)
    PriorityQueue<Problem> tree;

    /**
     * initialization funciton for the AndSearch
     * @param _env the environment
     * @param _s0 the starting state
     */
    public AndSearch(Environment _env, Problem _s0)
    {
        env = _env;
        tree = new PriorityQueue<Problem>(10, new FLeafComparator(env));
        tree.add(_s0);
        
    }

    private void PrintQueue()
    {
        while(!tree.isEmpty())
        {
            Problem pr = tree.poll();
            System.out.println(String.format("Problem score: " + pr.min_score + ", depth: " + pr.depth));
        }
    }

    /**
     * run the and tree search
     * best solution is returned in the environment
     * @return true if a solution could be found, otherwise false
     */
    public boolean RunSearch()
    {
        // start a timer
        long start_time = System.nanoTime();
        long current_time = start_time;
        double elapsed_time = 0;

        // start the loop
        for(int i = 0; i < env.max_iterations; i++)
        {  
            // timing
            current_time = System.nanoTime();
            elapsed_time = ((double)(current_time - start_time)) / 1000000000.0;

            if(i % status_update_freq == 0)
            {
                System.out.println(String.format("Elapsed Time: %10.2f, Iterations: %10d, tree size: %10d, Best so far: %10d", elapsed_time, i, tree.size(), env.best_score));
            }

            // time bound
            if(elapsed_time >= env.time_limit)
            {
                System.out.println("Out of time");
                break;
            }

            // iteration bound
            if(i > env.max_iterations)
            {
                System.out.println("max iterations");
                break;
            }

            // ensure that the queue is not empty
            if(tree.isEmpty())
            {
                System.out.println("tree fully explored");
                break;
            }

            // run a search step
            SearchStep();
        }

        //PrintQueue();

        // indicate if a solution has been found or not
        if(env.solution_found)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * run a single search step
     */
    private void SearchStep()
    {
        // sudo code:
        // get the problem from the top of the priority queue "tree"
        // check to see if it is boundable
        // check to see if the problem has all lectures and tutorials assigned (much easier that solvable) 
        // check to see if it is solvable 
        // use constrain ordering as ftrans to select to next lectrue or tutorial  
        // use div(valid lectrue slots or valid tutorail slots) to get the new problems
        // push the new problems onto the priority queue "tree"

        // get the best problem
        Problem top_problem = tree.poll();

        // check to see if the problem is pruneable
        if(top_problem.min_score >= env.best_score)
        {
            //System.out.println("Bound");
            // simply do not put the problem back into the queue and return
            return;   
        }

        // check to see if the problem has all lectures and tutorials assigned
        if(top_problem.AllAssigned())
        {
            //System.out.println("assigned");
            // try to get this problems score and record it if it is the best
            int score = Functions.Eval(top_problem, env);
            //System.out.println("score: " + score);
            if(score < env.best_score)
            {
                //Functions.PrintProblem(top_problem,env);
                env.best_score = score;
                env.best_sol = top_problem;
                env.solution_found = true;
            }

            return;
        }

        
        // check to see if it is solvable
        
        if(Functions.NotSatisfiable(top_problem, env))
        {
            //System.out.println("not satisfyable");
            // solvable in this case means that the constraints could not be met with the existing assignments
            // so do not put this back into the tree
            return;
        }

        // select the next lecture or tutorial based on the constraint rank 
        LecOrTutId selected = Functions.Ftrans(top_problem, env);

        // determine if lecture or tutorial
        if(selected.is_lec)
        {
            // determine if this is an active learning lecture
            boolean al = env.lectures[selected.id].is_al;
            
            // add all problem derivations that can stem from top_problem
            int[] valid_slots = Functions.ValidLectureSlots(env, selected.id, top_problem);
            if(valid_slots == null)
            {
                return;
            }
            // try all possible assignments
            for(int slot : valid_slots)
            {
                //System.out.println("\nAssigning lecture: " + selected.id + ", to slot: " + slot);
                Problem new_problem = new Problem(top_problem);
                new_problem.AssignLecture(selected.id, slot, al);
                new_problem.min_score = Functions.MinBoundScore(new_problem, env);

                // don't even bother adding it if its min score is worse than the best score
                if(new_problem.min_score < env.best_score)
                {
                    tree.add(new_problem);
                }
                
            }
        }
        else
        {
            // determine if this is an active learning tutorial
            boolean al = env.tutorials[selected.id].is_al;
            // add all problem derivations that can stem from top_problem
            int[] valid_slots = Functions.ValidTutSlots(env, selected.id, top_problem);
            if(valid_slots == null)
            {
                return;
            }
            // try all possible assignments
            for(int slot : valid_slots)
            {
                //System.out.println("\nAssigning tutorial: " + selected.id + ", to slot: " + slot);
                Problem new_problem = new Problem(top_problem);
                new_problem.AssignTutorial(selected.id, slot, al);
                new_problem.min_score = Functions.MinBoundScore(new_problem, env);
                // don't even bother adding it if its min score is worse than the best score
                if(new_problem.min_score < env.best_score)
                {
                    tree.add(new_problem);
                }
            }
        }
    }
}

/**
 * FLeafComparator is used to sort the Problems in the priority queue
 */
class FLeafComparator implements Comparator<Problem>
{
    // need a reference to the environment for comparison
    Environment env; 

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
        // 1: sort by prunable
        // 2: deepest nodes go first
        // 3: lowest score according to MinBoundScore go first
        // 4: tie break on problem unique id 

        
        if((p1.min_score >= env.best_score) && !(p2.min_score >= env.best_score))
        {
            return -1;
        }
        else if((p1.min_score >= env.best_score) && !(p2.min_score >= env.best_score))
        {
            return 1;
        }
 
        // Sort by depth
        if (p1.depth > p2.depth)
        {
            return -1;
        }
        else if (p1.depth < p2.depth)
        {
            return 1;
        }

        // sort by minboundscore
        if(p1.min_score < p2.min_score)
        {
            return -1;
        }
        else if(p1.min_score > p2.min_score)
        {
            return 1;
        }

        // Sort by hash
        if (p1.hashCode() > p2.hashCode())
        {
            return 1;
        }
        else
        {
            return -1;
        }
    }

    /**
     * setup the comparator
     * @param _env: the environment to use
     */
    public FLeafComparator(Environment _env)
    {
        env = _env;
    }
}