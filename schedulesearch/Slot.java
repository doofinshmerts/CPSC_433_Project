package schedulesearch;

/**
 * slot class holds information about a slot
 */
public class Slot
{
    // the maximum number of lectrues/tutorials that can be assigned to this slot
    int max;
    // the maximum number of active learning lectures/tutorials that can be assigned to this slot
    int almax;
    // the minimum number of lectures/tutorials/ that can be assigned to this slot
    int min;
    // is this an evening slot
    boolean is_evng;
    
    // stuff for the time:
    int day; // the day (0: monday, 1: tuesday, 2: wednesday, 3: thursday, 4: Friday)
    int hour; // the hour in millitary time (0-24)
    int minute; // the minute (0-60)

    public void SetSlotProperties(int _max, int _almax, int _min, int _day, int _hour, int _minute)
    {
        max = _max;
        almax = _almax;
        min = _min;

        //TODO: determine from hour and minute whether or not this is an evening lecture
    }
}