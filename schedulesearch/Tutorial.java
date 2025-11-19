package schedulesearch;

import java.util.HashMap;
import java.util.HashSet;

public class Tutorial
{
    int id; // the unique id of this tutorial
    boolean is_al; // is this lecture an active learning tutorial
    boolean is_evng; // is this an evening lecture
    int lec_id; // the id of the associated lecture of this tutorial
    // holds the incompatible lectures/tutorials of this lecture (if hashmap contains key: id, then the lecture/tutorial corresponding to this id is incompatible)
    HashSet<Integer> not_compatible_lec = new HashSet<Integer>(); // for lectures
    HashSet<Integer> not_compatible_tut = new HashSet<Integer>(); // for tutorials
    // the map used to implement unwanted, if the id of slot s is contained in unwanted, then slot s is unwanted
    HashSet<Integer> unwanted = new HashSet<Integer>();
    // the map used to implement preferences, the preference of slot s with id key, is given by preferences[key]
    HashMap<Integer, Integer> preferences = new HashMap<Integer, Integer>();

    String course_descriptor; // e.g. CPSC 433
    int lec_num; // e.g. LEC 01 -> lec_num = 1 (defualt to 1 if not included)
    int tut_num; // e.g. TUT/LAB 04 -> tut_num = 4
    String name; // the full name of the tutorial
    /**
     * Print important data for this Tutorial
     */ 
    public void PrintData()
    {
        System.out.print(String.format("TUT: %s %d %d, id: %d, lec_id: %d, ", course_descriptor, lec_num, tut_num, id, lec_id));
        System.out.print(is_al);
        System.out.print(", ");
        System.out.println(is_evng); 
    }
}