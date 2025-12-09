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
    PriorityQueue<Problem> tree;

    /**
     * initialization funciton for the AndSearch
     * @param _env the environment
     * @param _s0 the starting state
     */
    public AndSearch(Environment _env, Problem _s0)
    {
        env = _env;
        tree = new PriorityQueue<Problem>(10, new FLeafComparator(_env));
        tree.add(_s0);
    }

    /**
     * run the and tree search
     * @param sf the final state to return
     * @return true if a solution could be found, otherwise false
     */
    public boolean RunSearch(Problem sf)
    {
<<<<<<< Updated upstream
        while (!tree.isEmpty() && env.iterations < env.max_iterations) {
            Problem top_problem = tree.poll();
            env.iterations++;
            
            // Check if this is a complete solution
            if (!top_problem.UnassignedLectures() && !top_problem.UnassignedTutorials()) {
                if (top_problem.score < env.best_score) {
                    env.best_score = top_problem.score;
                    env.best_sol = new Problem(top_problem);
                    // Copy the solution to sf
                    sf.lectures = top_problem.lectures.clone();
                    sf.tutorials = top_problem.tutorials.clone();
                    sf.depth = top_problem.depth;
                    sf.score = top_problem.score;
                    return true;
                }
                continue;
            }
            
            // Prune if bound is worse than best solution
            if (Functions.FBound(top_problem, env)) {
                continue;
            }
            
            // Expand the node
            expandNode(top_problem);
        }
        return false;
=======
        // Initialize timing and iteration counter
        env.start_time = System.nanoTime();
        env.iterations = 0;
        
        while (!tree.isEmpty() && 
               env.iterations < env.max_iterations && 
               (System.nanoTime() - env.start_time) < env.time_limit) {
            
            // get the problem from the top of the priority queue "tree"
            Problem current = tree.poll();
            env.iterations++;
            env.current_time = System.nanoTime() - env.start_time;
            
            // Check if this is a complete solution
            if (Functions.Solvable(current)) {
                int score = Functions.Eval(current, env);
                if (score < env.best_score) {
                    env.best_score = score;
                    // Copy the solution to return parameter
                    copyProblem(current, sf);
                    System.out.println("Found new best solution with score: " + score);
                }
                continue; // Move to next node in queue
            }
            
            // Check if we should prune this branch
            if (Functions.FBound(current, env)) {
                continue; // Prune this branch
            }
            
            // use ftrans to select the transition (select which lecture/tutorial to expand next)
            int itemToExpand = selectItemToExpand(current);
            if (itemToExpand == -1) {
                continue; // No expandable items found
            }
            
            boolean isLecture = itemToExpand < current.lectures.length;
            int actualId = isLecture ? itemToExpand : itemToExpand - current.lectures.length;
            
            // use div to get the new problems
            int[] validSlots;
            if (isLecture) {
                validSlots = Functions.ValidLectureSlots(env, actualId, current);
            } else {
                validSlots = Functions.ValidTutSlots(env, actualId, current);
            }
            
            if (validSlots == null || validSlots.length == 0) {
                continue; // No valid slots for this item
            }
            
            // push the new problems onto the priority queue "tree"
            for (int slotId : validSlots) {
                Problem newProblem = createCopy(current);
                newProblem.depth = current.depth + 1;
                
                if (isLecture) {
                    newProblem.AssignLecture(actualId, slotId);
                } else {
                    newProblem.AssignTutorial(actualId, slotId);
                }
                
                tree.add(newProblem);
            }
        }
        
        // Return whether we found a solution
        boolean foundSolution = (env.best_score < 2000000000);
        System.out.println("Search completed. Iterations: " + env.iterations + 
                          ", Time: " + (env.current_time / 1000000000.0) + "s" +
                          ", Found solution: " + foundSolution +
                          ", Best score: " + (foundSolution ? env.best_score : "N/A"));
        return foundSolution;
    }
    
    /**
     * Select which unassigned item to expand next (ftrans implementation)
     * Priority: DIV9 lectures > active learning > everything else
     */
    private int selectItemToExpand(Problem current) {
        // First pass: look for DIV9 lectures (evening lectures)
        for (int i = 0; i < current.lectures.length; i++) {
            if (current.lectures[i] == -1 && env.lectures[i].is_evng) {
                return i; // Return lecture index
            }
        }
        
        // Second pass: look for active learning items
        for (int i = 0; i < current.lectures.length; i++) {
            if (current.lectures[i] == -1 && env.lectures[i].is_al) {
                return i;
            }
        }
        for (int i = 0; i < current.tutorials.length; i++) {
            if (current.tutorials[i] == -1 && env.tutorials[i].is_al) {
                return current.lectures.length + i; // Return offset index for tutorial
            }
        }
        
        // Third pass: any unassigned lecture
        for (int i = 0; i < current.lectures.length; i++) {
            if (current.lectures[i] == -1) {
                return i;
            }
        }
        
        // Fourth pass: any unassigned tutorial
        for (int i = 0; i < current.tutorials.length; i++) {
            if (current.tutorials[i] == -1) {
                return current.lectures.length + i;
            }
        }
        
        return -1; // No unassigned items found
    }
    
    /**
     * Create a deep copy of a problem
     */
    private Problem createCopy(Problem original) {
        Problem copy = new Problem();
        copy.SetupProblem(original.lectures.length, original.tutorials.length);
        System.arraycopy(original.lectures, 0, copy.lectures, 0, original.lectures.length);
        System.arraycopy(original.tutorials, 0, copy.tutorials, 0, original.tutorials.length);
        copy.depth = original.depth;
        copy.score = original.score;
        return copy;
    }
    
    /**
     * Copy problem data to another problem instance
     */
    private void copyProblem(Problem source, Problem target) {
        target.SetupProblem(source.lectures.length, source.tutorials.length);
        System.arraycopy(source.lectures, 0, target.lectures, 0, source.lectures.length);
        System.arraycopy(source.tutorials, 0, target.tutorials, 0, source.tutorials.length);
        target.depth = source.depth;
        target.score = source.score;
>>>>>>> Stashed changes
    }

    /**
     * Expand a node by selecting the next variable to assign based on f_trans rules
     * @param problem the problem to expand
     */
    private void expandNode(Problem problem) {
        // Implement f_trans rules from your proposal:
        // 1. "DIV 9" lectures with evening time slots first
        // 2. lectures/tutorials that require active learning
        // 3. all other lectures/tutorials
        
        Lecture nextLecture = findNextLectureToAssign(problem);
        if (nextLecture != null) {
            int[] validSlots = Functions.ValidLectureSlots(env, nextLecture.id, problem);
            if (validSlots != null) {
                for (int slot : validSlots) {
                    Problem newProblem = new Problem(problem);
                    newProblem.AssignLecture(nextLecture.id, slot);
                    newProblem.depth++;
                    newProblem.score = Functions.Eval(newProblem, env);
                    tree.add(newProblem);
                }
            }
            return;
        }
        
        Tutorial nextTutorial = findNextTutorialToAssign(problem);
        if (nextTutorial != null) {
            int[] validSlots = Functions.ValidTutSlots(env, nextTutorial.id, problem);
            if (validSlots != null) {
                for (int slot : validSlots) {
                    Problem newProblem = new Problem(problem);
                    newProblem.AssignTutorial(nextTutorial.id, slot);
                    newProblem.depth++;
                    newProblem.score = Functions.Eval(newProblem, env);
                    tree.add(newProblem);
                }
            }
        }
    }

    /**
     * Find the next lecture to assign based on f_trans rules
     * @param problem the current problem state
     * @return the next lecture to assign, or null if no lectures need assignment
     */
    private Lecture findNextLectureToAssign(Problem problem) {
        // Rule 1: "DIV 9" lectures with evening time slots first
        for (Lecture lec : env.lectures) {
            if (problem.lectures[lec.id] == -1 && isDiv9Lecture(lec)) {
                return lec;
            }
        }
        
        // Rule 2: Active learning lectures
        for (Lecture lec : env.lectures) {
            if (problem.lectures[lec.id] == -1 && lec.is_al) {
                return lec;
            }
        }
        
        // Rule 3: All other lectures
        for (Lecture lec : env.lectures) {
            if (problem.lectures[lec.id] == -1) {
                return lec;
            }
        }
        
        return null;
    }

    /**
     * Find the next tutorial to assign based on f_trans rules
     * @param problem the current problem state
     * @return the next tutorial to assign, or null if no tutorials need assignment
     */
    private Tutorial findNextTutorialToAssign(Problem problem) {
        // Rule 2: Active learning tutorials first
        for (Tutorial tut : env.tutorials) {
            if (problem.tutorials[tut.id] == -1 && tut.is_al) {
                return tut;
            }
        }
        
        // Rule 3: All other tutorials
        for (Tutorial tut : env.tutorials) {
            if (problem.tutorials[tut.id] == -1) {
                return tut;
            }
        }
        
        return null;
    }

    /**
     * Check if a lecture is a "DIV 9" evening lecture
     * @param lecture the lecture to check
     * @return true if it's a DIV 9 lecture
     */
    private boolean isDiv9Lecture(Lecture lecture) {
        // Check if this is an evening lecture (lecture number 9 indicates evening)
        return lecture.lec_num == 9 || lecture.is_evng;
    }
}

/**
 * FLeafComparator is used to sort the Problems in the priority queue
 */
class FLeafComparator implements Comparator<Problem>
{
    private Environment env;
    
    public FLeafComparator() {
        // Default constructor
    }
    
    public FLeafComparator(Environment environment) {
        this.env = environment;
    }
    
    /**
     * This methode implements the f_leaf function to sort problems p1 and p2
     * @param p1 the first problem to sort
     * @param p2 the second problem to sort
     * @return negative if the first object should go first in the list, positive otherwise
     */ 
    public int compare(Problem p1, Problem p2)
    {
<<<<<<< Updated upstream
        // Sort on the following priority (from your project proposal):
        // 1: solvable nodes go first
        // 2: deepest nodes go first  
        // 3: lowest score according to MinBoundScore go first
        // 4: tie break on problem unique id

        // Create a temporary environment if none is provided - DECLARE FIRST!
        Environment tempEnv = (env != null) ? env : new Environment();
        
        // NOW use tempEnv in the function calls
        boolean p1_solvable = Functions.Solvable(p1, tempEnv);
        boolean p2_solvable = Functions.Solvable(p2, tempEnv);

        if (p1_solvable && !p2_solvable) {
            return -1;
        }
        if (!p1_solvable && p2_solvable) {
            return 1;
        }

        // Sort by depth (deepest nodes first)
        if (p1.depth > p2.depth) {
            return -1;
        }
        if (p1.depth < p2.depth) {
            return 1;
        }

        // Sort by MinBoundScore (lowest score first)
        int score1 = Functions.MinBoundScore(p1, tempEnv);
        int score2 = Functions.MinBoundScore(p2, tempEnv);
        
        if (score1 < score2) {
            return -1;
        }
        if (score1 > score2) {
            return 1;
        }

        // Sort by closeness to fulfilling lecturemin and tutorialmin
        int p1MinFillDiscrepancy = calculateMinFillDiscrepancy(p1, tempEnv);
        int p2MinFillDiscrepancy = calculateMinFillDiscrepancy(p2, tempEnv);
        
        if (p1MinFillDiscrepancy < p2MinFillDiscrepancy) {
            return -1;
        }
        if (p1MinFillDiscrepancy > p2MinFillDiscrepancy) {
            return 1;
        }

        // Final tie break: use hash code
        return Integer.compare(p1.hashCode(), p2.hashCode());
    }
    
    /**
     * Calculate the sum of discrepancies between actual and needed values for min fill constraints
     * @param problem the problem to evaluate
     * @param env the environment
     * @return the total discrepancy
     */
    private int calculateMinFillDiscrepancy(Problem problem, Environment env) {
        int discrepancy = 0;
        
        // Count lectures in each slot
        int[] lecCount = new int[env.lec_slots_array.length];
        for (int lectureSlot : problem.lectures) {
            if (lectureSlot != -1) {
                lecCount[lectureSlot]++;
            }
        }
        
        // Calculate discrepancies for lecture slots
        for (int i = 0; i < lecCount.length; i++) {
            if (lecCount[i] < env.lec_slots_array[i].min) {
                discrepancy += (env.lec_slots_array[i].min - lecCount[i]);
            }
        }
        
        // Count tutorials in each slot
        int[] tutCount = new int[env.tut_slots_array.length];
        for (int tutorialSlot : problem.tutorials) {
            if (tutorialSlot != -1) {
                tutCount[tutorialSlot]++;
            }
        }
        
        // Calculate discrepancies for tutorial slots
        for (int i = 0; i < tutCount.length; i++) {
            if (tutCount[i] < env.tut_slots_array[i].min) {
                discrepancy += (env.tut_slots_array[i].min - tutCount[i]);
            }
        }
        
        return discrepancy;
=======
        // 1: solvable nodes go first
        boolean p1Solvable = Functions.Solvable(p1);
        boolean p2Solvable = Functions.Solvable(p2);
        if (p1Solvable && !p2Solvable) return -1;
        if (!p1Solvable && p2Solvable) return 1;
        
        // 2: deepest nodes go first (more assignments = closer to solution)
        int p1Depth = Functions.Depth(p1);
        int p2Depth = Functions.Depth(p2);
        if (p1Depth != p2Depth) {
            return p2Depth - p1Depth; // Higher depth first
        }
        
        // 3: lowest score according to MinBoundScore go first
        
        // 4: tie break on problem unique id (use hash code for consistency)
        return Integer.compare(p1.hashCode(), p2.hashCode());
>>>>>>> Stashed changes
    }
}