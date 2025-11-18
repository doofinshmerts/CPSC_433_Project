package schedulesearch;

/**
 * slot class holds information about a slot
 */
public class Slot
{
    // the unique id of this slot
    int id;
    // the location hash of this slot in lecture time
    // if two slots have the same lec_hash, then they start at the same time on the same days 
    int lec_hash;
    // the location hash of this slot in tutorial time
    // if two slots have the same tut_hash, then they start at the same time on the same days
    int tut_hash; 
    // the maximum number of lectrues/tutorials that can be assigned to this slot
    int max;
    // the maximum number of active learning lectures/tutorials that can be assigned to this slot
    int almax;
    // the minimum number of lectures/tutorials/ that can be assigned to this slot
    int min;
    // is this an evening slot
    boolean is_evng = false;
    
    // stuff for the time:
    int day; // the day (0: monday, 1: tuesday, 2: wednesday, 3: thursday, 4: Friday)
    int hour; // the hour in millitary time (0-24)
    int minute; // the minute (0-60)

    public void PrintSlot()
    {
        System.out.println(String.format("id: %d, lec_hash: %d, tut_hash: %d, day: %d, time: %d:%d max: %d, min: %d, almax: %d", id, lec_hash, tut_hash, day, hour, minute, max, min, almax)); 
    }
    public void SetupSlot()
    {
        lec_hash = SlotHash(true);
        tut_hash = SlotHash(false);
        
        // is this an evening lecture
        if(hour >= 18)
        {
            is_evng = true;
        }
    }

    /**
     * Get the unique hash for this slot, this number will represent the time in minutes from monday 0:00
     * This is used to see if slots effectively equivalent (e.g. MON 9:00 and WED 9:00 are the same slot)
     * @param is_lecslot: is this a lecture slot, then MWF will map to the same time, TTr will map to the same time
     * if false then MW will map to the same time, TTr will map to the same time, and friday is unique
     * @return: the time in minutes from monday 0:00 that this slot starts at
     */ 
    private int SlotHash(boolean is_lecslot)
    {
        int day_eq = 0;
        if(is_lecslot)
        {
            if( (day == 0)||(day == 2)||(day == 4))
            {
                day_eq = 0;
            }
            else
            {
                day_eq = 1;
            }
        }
        else
        {
            if( (day == 0)||(day == 2))
            {
                day_eq = 0;
            }
            else if( (day == 1) || (day == 3))
            {
                day_eq = 1;
            }
            else
            {
                day_eq = 4;
            }
        }

        return minute + hour*60 + day_eq*1440;
    }

    /**
     * is this slot overlaping the department meeting that happens on Tuesday 11:00-12:30
     * @retrun: true if it is overlapping, false otherwise
     */
    public boolean OverlapTuesdayMeeting()
    {
        if((day == 1) || (day == 3))
        {
            if(hour == 11)
            {
                return true;
            }
            else if((hour == 12) && (minute <= 30))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * does this time slot overlaps a given time slot
     * @param s: the time slot to check against
     * @retrun: if this time slot overlaps timeslot s then return true, false otherwise
     */
    public boolean OverlapTimeslot(Slot s)
    {
        //TODO: implement this
        return false;
    }
}