package schedulesearch;

public class Problem
{
    // the lecture map is 
    int[] lectures;
    int[] tutorials;
    int depth;
    int score;

    public Problem()
    {

    }

    public void SetupProblem(int num_lectures, int num_tutorials)
    {
        lectures = new int[num_lectures];
        tutorials = new int[num_tutorials];
        depth = 0;
        score = 0;
    }
}