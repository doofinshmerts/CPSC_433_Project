package schedulesearch;
import java.io.File;

/**
 * the InputParser class has a static methode for parsing the input file and returning the environment "Env" and the starting state "s0"
 */
public final class InputParser
{
    /**
     * parses an input file for the environment variables and the starting state
     * The parser first gets all information from the file and creates the environment
     * Then it uses any partial assignments to populate the starting state. if there are
     * any error in parsing the file, or if the partial assignments are un-satisfiable then return an error
     * @param input_file: the input file to parse
     * @param env: the environment to return
     * @param s0: the start state to return
     * @return: false if there was an error, otherwise true
     */
    public static boolean ParseInputFile(String input_file, Environment env, Problem s0)
    {
        // sudo code:
        // parse the file for the information in environment, return false on any parsing error
        // parse the file for the partial assignments, return false on any parsing error
        // try adding the partial assignments to s0 
        // if any partial assignment leads to an invalid assignment in s0 return false
        // if all partial assignments are valid then return true

        // create the file object
        File file = new File(input_file);

        // check that the file exists
        if(!file.exists() || !file.isFile() || !file.canRead())
        {
            System.out.println("Could not load from file: " + input_file);
          
            return false;
        }

        System.out.println("reading from file: " + input_file); 

        return false;
    }
}