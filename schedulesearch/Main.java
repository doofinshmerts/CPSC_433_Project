package schedulesearch;

/**
 * program entry point
 */
public class Main
{
    // the names of input variables
    private static final String[] INPUT_VARS = {"w_minfilled", "w_pref", "w_par", "w_secdiff", "pen_lecturemin", "pen_tutorialmin", "pen_notpaired", "pen_section", "max_iterations", "time_limit"};

    public static void main(String[] args)
    {
        // check that the number of input arguments is correct
        if(args.length < 9)
        {
            System.out.println("Not enough input variables, usage java -jar ./build/Build.jar <input_file_name.txt> <w_minfilled> <w_pref> <w_pair> <w_secdiff> <pen_lecturemin> <pen_tutorialmin> <pen_notpaired> <pen_section> <max_iterations> <time_limit>");
            System.out.println("If max_iterations or time_limit is not set, then they will both be set to infinity");
            return;
        }
    
        // the input variables
        int[] input_values = new int[INPUT_VARS.length];
        int[] buffer = new int[1];

        // if there were 11 input arguments then all the input values are given,
        // so try to get all the input variables
        if(args.length == 11)
        {
            for(int i = 0; i < 10; i++)
            {
                if(!GetSafeIntFromString(args[i+1], buffer))
                {
                    System.out.println(String.format("Error, invalid input to %s", INPUT_VARS[i]));
                    return;
                }
                input_values[i] = buffer[0];
                System.out.println(String.format("%s set to: %5d", INPUT_VARS[i], input_values[i]));
            }
        }
        // otherwise, only get the first 8 input variables
        else
        {
            for(int i = 0; i < 8; i++)
            {
                if(!GetSafeIntFromString(args[i+1], buffer))
                {
                    System.out.println(String.format("Error, invalid input to %s", INPUT_VARS[i]));
                    return;
                }
                input_values[i] = buffer[0];
                System.out.println(String.format("%s set to: %5d", INPUT_VARS[i], input_values[i]));
            }    

            // set the 2 last variables to large numbers
            input_values[8] = 1000000000;
            input_values[9] = 1000000000;
            System.out.println(String.format("%s set to: %5d", INPUT_VARS[8], input_values[8]));
            System.out.println(String.format("%s set to: %5d", INPUT_VARS[9], input_values[9]));
        }

        // setup the environment and starting state
        Environment env = new Environment();
        // set the calculation variables for the environment
        env.SetWeights(input_values[0],input_values[1],input_values[2],input_values[3],input_values[4],input_values[5],input_values[6],input_values[7],input_values[8],input_values[9]); 
        Problem s0 = new Problem();

        // use the input parser to create the environment and starting state
        if(InputParser.ParseInputFile(args[0], env, s0))
        {
            AndSearch search = new AndSearch(env, s0);
            Problem pr = new Problem();
            search.RunSearch(pr);
        }
    }

    /**
     * Get an integer from a string 
     * @param input_string the input string to parse
     * @param int_return the array to put the integer in (will be put into index 0)
     * @return true if a number was found, false if not
     */
    private static boolean GetSafeIntFromString(String input_string, int[] int_return)
    {
        // check that the given array is not null and has at least one index for assigning values
        if(int_return == null || int_return.length == 0)
        {
            return false;
        }

        // try to get an integer from the string
        try{
            int_return[0] = Integer.parseInt(input_string);
            // if the string was a float then return the float by reference and return true
            return true;
        }catch(NumberFormatException e)
        {
            // if the string was not a float then return false and an error message to the user
            int_return[0] = -1;
            return false;
        }
    }
}
