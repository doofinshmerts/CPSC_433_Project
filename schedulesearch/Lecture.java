package schedulesearch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

/**
 * The lecture class type contains all properties of a lecture
 */
public class Lecture
{
    int id; // the unique id of this lecture
    boolean is_al; // is this lecture an active learning lecture
    boolean is_5xx; // is this a 500 level lecture
    boolean is_evng; // is this an evening lecture
    int section; // the section that this lecture belongs to
    int[] tutorials; // the id's of the tutorials that belong to this lecture
    // holds the incompatible lectures/tutorials of this lecture (if hashmap contains key: id, then the lecture/tutorial corresponding to this id is incompatible)
    HashSet<Integer> not_compatible_lec = new HashSet<Integer>(); // for lectures
    HashSet<Integer> not_compatible_tut = new HashSet<Integer>(); // for tutorials
    // the map used to implement unwanted, if the id of slot s is contained in unwanted, then slot s is unwanted
    HashSet<Integer> unwanted = new HashSet<Integer>();
    // the map used to implement preferences, the preference of slot s with id key, is given by preferences[key]
    HashMap<Integer, Integer> preferences = new HashMap<Integer, Integer>();
    // the score of the lectures first choice
    int first_choice = 0;
    
    String course_descriptor; // e.g. CPSC 433
    int lec_num; // the lecture number
    String name; // the full name of the lecture

    /**
     * Print important data for this lecture
     */ 
    public void PrintData()
    {
        System.out.print(String.format("LEC: %s %d, id: %d, sec: %d, ", course_descriptor, lec_num, id, section));
        System.out.print(is_al);
        System.out.print(", ");
        System.out.print(is_5xx);
        System.out.print(", ");
        System.out.println(is_evng); 
    }
}