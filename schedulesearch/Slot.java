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

    /**
     * print formatted slot information
     */
    public void PrintSlot()
    {
        System.out.println(String.format("id: %d, lec_hash: %d, tut_hash: %d, day: %d, time: %d:%d max: %d, min: %d, almax: %d", id, lec_hash, tut_hash, day, hour, minute, max, min, almax)); 
    }

    /**
     * Setup the basic slot properties
     */
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
     * Treat this as a tutorial slot, get the lecture hashes of lecture slots that overlap this time
     * @return array of slot lecture hashes (not ids, do not get this confused)
     */
    public int[] TutSlotToOverLappingLecSlots()
    {
        if(day == 4)
        {
            // a friday tutorial slot covers two consecutive lecture slots
            int[] values = new int[2];
            values[0] = lec_hash;
            values[1] = lec_hash + 60; 
            return values;
        }
        else if((day == 1) || (day == 3))
        {
            // a tuesday or thursday lecture can cover two tutorial slots
            if((hour % 3) == 2)
            {
                // same start time as a tutorial
                int[] values = new int[1];
                values[0] = tut_hash;
                return values;
            }
            else if((hour % 3) == 0)
            {
                // covers tutorial before and after
                int[] values = new int[2];
                values[0] = tut_hash -60;
                values[1] = tut_hash + 30;
                return values;
            }
            else
            {
                // covers tutorial before
                int[] values = new int[1];
                values[0] = tut_hash -30;
                return values;
            }
        }
        else
        {
            // monday or wednesday it only covers one lecture slot
            int[] values = new int[1];
            values[0] = lec_hash;
            return values;
        }
    }

    /**
     * Treat this as a lecture slot, get the tutorial hashes of tutorial slots that overlap this time
     * @return array of slot lecture hashes (not ids, do not get this confused)
     */
    public int[] LecSlotToOverLappingTutSlots()
    {
        if((day == 0) || (day == 2) || (day == 4))
        {
            // if MWF then we need to check dirrect mapping and friday overlap
            int[] values = new int[2];
            values[0] = lec_hash; // for monday wednesday tutorials
            if((hour % 2) == 0)
            {
                // even number so same as tutorial hash
                values[1] = lec_hash + 5760;
            }
            else
            {
                // odd number so -60 from tutorial hash
                values[1] = lec_hash + 5700;
            }
            return values;
        }
        else
        {
            // a tuesday or thursday lecture covers two lecture slots
            int[] values = new int[2];
            if(minute == 0)
            {
                // then it covers lectures at the current hash and the next hash
                values[0] = lec_hash;
                values[1] = lec_hash + 60;
            }
            else
            {
                // then it covers lectures at +30 and -30
                values[0] = lec_hash - 30;
                values[1] = lec_hash + 30;
            }
            return values;
        }
    }

    /**
     * Get the unique hash for this slot, this number will represent the time in minutes from monday 0:00
     * This is used to see if slots effectively equivalent (e.g. MON 9:00 and WED 9:00 are the same slot)
     * @param is_lecslot is this a lecture slot, then MWF will map to the same time, TTr will map to the same time
     * if false then MW will map to the same time, TTr will map to the same time, and friday is unique
     * @return the time in minutes from monday 0:00 that this slot starts at
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
     * @retrun true if it is overlapping, false otherwise
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
}