package schedulesearch;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;

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
        // Record start time (using nanoTime to match Environment's scale)
        long startTime = System.nanoTime();
        int iterations = 0;
        
        // Reset environment best score for this run
        // env.best_score = Integer.MAX_VALUE; 

        while(!tree.isEmpty())
        {
            // Check Iteration Limit
            if (iterations > env.max_iterations) {
                System.out.println("Search halted: Max iterations reached (" + env.max_iterations + ")");
                break;
            }

            // Check Time Limit
            if ((System.nanoTime() - startTime) > env.time_limit) {
                System.out.println("Search halted: Time limit reached.");
                break;
            }

            Problem top_problem = tree.poll();
            if (top_problem == null) break;

            // 1. Check Solvability (Consistency)
            if (!Functions.Solvable(top_problem, env)) {
                // Prune: The partial assignment is already invalid/inconsistent
                continue;
            }

            // 2. Check Bounds (Optimization)
            if (Functions.FBound(top_problem, env)) {
                // Prune: The minimum bound score is worse than the best solution found so far
                continue;
            }

            // 3. Check for Complete Solution
            if (!top_problem.UnassignedLectures() && !top_problem.UnassignedTutorials()) {
                // Problem is Solvable (Valid), Within Bounds, and Complete.
                // Check if it's the best so far.
                if (top_problem.score < env.best_score) {
                    env.best_score = top_problem.score;
                    env.best_sol = top_problem;
                    // Optional: Print progress
                    // System.out.println("New best solution found: " + env.best_score);
                }
                // Do not expand a complete solution
                continue;
            }

            // 4. Expand (Branching)
            // Heuristic: Minimum Remaining Values (MRV) with ConstraintRank tie-breaker
            
            Lecture best_lecture = null;
            int min_lec_mrv = Integer.MAX_VALUE;
            int max_lec_rank = -1;

            for(Lecture lec : env.lectures)
            {
                // Check if unassigned
                if (top_problem.lectures[lec.id] == -1)
                {
                    // Calculate MRV (number of valid slots)
                    int[] valid_slots = Functions.ValidLectureSlots(env, lec.id, top_problem);
                    int mrv = (valid_slots != null) ? valid_slots.length : 0;
                    
                    // Optimization: If 0 valid slots, this branch is dead. Prune immediately (conceptually).
                    // In practice, we can just pick it, and the expansion loop below will add 0 children, killing the branch.
                    
                    int rank = Functions.ConstraintRank(lec, env);

                    if (mrv < min_lec_mrv) {
                        min_lec_mrv = mrv;
                        max_lec_rank = rank;
                        best_lecture = lec;
                    } else if (mrv == min_lec_mrv) {
                        if (rank > max_lec_rank) {
                            max_lec_rank = rank;
                            best_lecture = lec;
                        }
                    }
                }
            }

            if(best_lecture != null)
            {
                // Expand on this lecture
                int[] valid_slots = Functions.ValidLectureSlots(env, best_lecture.id, top_problem);
                if (valid_slots != null) {
                    for(int slot : valid_slots)
                    {
                        Problem new_problem = new Problem(top_problem);
                        new_problem.AssignLecture(best_lecture.id, slot);
                        new_problem.depth++;
                        new_problem.score = Functions.Eval(new_problem, env);
                        tree.add(new_problem);
                    }
                }
            }
            else
            {
                // No lectures left, try tutorials
                Tutorial best_tutorial = null;
                int min_tut_mrv = Integer.MAX_VALUE;
                int max_tut_rank = -1;

                for(Tutorial tut : env.tutorials)
                {
                    // Check if unassigned
                    if (top_problem.tutorials[tut.id] == -1)
                    {
                        int[] valid_slots = Functions.ValidTutSlots(env, tut.id, top_problem);
                        int mrv = (valid_slots != null) ? valid_slots.length : 0;
                        
                        int rank = Functions.ConstraintRank(tut, env);

                        if (mrv < min_tut_mrv) {
                            min_tut_mrv = mrv;
                            max_tut_rank = rank;
                            best_tutorial = tut;
                        } else if (mrv == min_tut_mrv) {
                            if (rank > max_tut_rank) {
                                max_tut_rank = rank;
                                best_tutorial = tut;
                            }
                        }
                    }
                }

                if(best_tutorial != null)
                {
                    // Expand on this tutorial
                    int[] valid_slots = Functions.ValidTutSlots(env, best_tutorial.id, top_problem);
                    if (valid_slots != null) {
                        for(int slot : valid_slots)
                        {
                            Problem new_problem = new Problem(top_problem);
                            new_problem.AssignTutorial(best_tutorial.id, slot);
                            new_problem.depth++;
                            new_problem.score = Functions.Eval(new_problem, env);
                            tree.add(new_problem);
                        }
                    }
                }
            }

            iterations++;
        }

        // Search finished (or stopped)
        if (env.best_sol != null) {
            System.out.println("Search Finished.");
            Functions.PrintProblem(env.best_sol, env);
            return true;
        }
        
        System.out.println("No valid solution found.");
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
        // sort on the following priority
        // 1: solvable nodes go first
        // 2: deepest nodes go first
        // 3: lowest score according to MinBoundScore go first
        // 4: tie break on problem unique id 
        
        // Sort by solvable
        if (p1.depth > p2.depth)
        {
            return -1;
        }
        if (p1.depth < p2.depth)
        {
            return 1;
        }

        // Sort by MinBoundScore (Lower is better)
        // We can use current score as proxy or calculate MinBoundScore
        // MinBoundScore is also expensive.
        // Usually you store 'f_score' in Problem.
        
        if (p1.score < p2.score) {
            return -1;
        }
        if (p1.score > p2.score) {
            return 1;
        }

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