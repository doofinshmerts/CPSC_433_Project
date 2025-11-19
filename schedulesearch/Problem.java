package schedulesearch;

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
}