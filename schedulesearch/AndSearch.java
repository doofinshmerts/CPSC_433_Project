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
     * @param _env: the environment
     * @param _s0: the starting state
     */
    public AndSearch(Environment _env, Problem _s0)
    {
        env = _env;
        tree.add(_s0);
    }

    /**
     * run the and tree search
     * @param sf: the final state to return
     * @return: true if a solution could be found, otherwise false
     */
    public boolean RunSearch(Problem sf)
    {
        // sudo code:
        // get the problem from the top of the priority queue "tree"
        // use ftrans to select the transition 
        // use div to get the new problems
        // push the new problems onto the priority queue "tree"
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
     * @param p1: the first problem to sort
     * @param p2: the second problem to sort
     * @return: negative if the first object should go first in the list, positive otherwise
     */ 
    public int compare(Problem p1, Problem p2)
    {
        // sudo code:
        // sort on the following priority
        // 1: solvable nodes go first
        // 2: deepest nodes go first
        // 3: lowest score according to MinBoundScore go first
        // 4: tie break on problem unique id 
        return 0;
    }
}