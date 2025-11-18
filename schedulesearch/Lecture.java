package schedulesearch;

import java.util.ArrayList;
import java.util.HashMap;

public class Lecture
{
    int id; // the unique id of this lecture
    boolean is_al; // is this lecture an active learning lecture
    boolean is_5xx; // is this a 500 level lecture
    boolean is_evng; // is this an evening lecture
    int section; // the section that this lecture belongs to
    int[] tutorials; // the id's of the tutorials that belong to this lecture
    // holds the incompatible lectures/tutorials of this lecture (if hashmap contains key: id, then the lecture/tutorial corresponding to this id is incompatible)
    HashMap<Integer, Boolean> not_compatible = new HashMap<Integer, Boolean>();
    // the map used to implement unwanted, if the id of slot s is contained in unwanted, then slot s is unwanted
    HashMap<Integer, Boolean> unwanted = new HashMap<Integer, Boolean>();
    // the map used to implement preferences, the preference of slot s with id key, is given by preferences[key]
    HashMap<Integer, Integer> preferences = new HashMap<Integer, Integer>();
}