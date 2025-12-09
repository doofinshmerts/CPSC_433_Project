package schedulesearch;

import java.util.ArrayList;
import java.util.List;

/**
 * Slot class holds information about a time slot for scheduling lectures and tutorials.
 */
public class Slot {
    // Constants for time calculations
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MINUTES_PER_DAY = 1440;
    private static final int FRIDAY_TUTORIAL_OFFSET = 60;
    private static final int TUESDAY_THURSDAY_TUT_OFFSET_1 = 60;
    private static final int TUESDAY_THURSDAY_TUT_OFFSET_2 = 30;
    private static final int FRIDAY_LEC_OFFSET_EVEN = 5760;
    private static final int FRIDAY_LEC_OFFSET_ODD = 5700;
    
    // Day constants for better readability
    public static final int MONDAY = 0;
    public static final int TUESDAY = 1;
    public static final int WEDNESDAY = 2;
    public static final int THURSDAY = 3;
    public static final int FRIDAY = 4;
    
    // Department meeting time constants
    private static final int MEETING_DAY_START = TUESDAY;
    private static final int MEETING_DAY_END = THURSDAY;
    private static final int MEETING_START_HOUR = 11;
    private static final int MEETING_END_HOUR = 12;
    private static final int MEETING_END_MINUTE = 30;
    private static final int EVENING_START_HOUR = 18;

    // The unique id of this slot
    private int id;
    // The location hash of this slot in lecture time
    // If two slots have the same lecHash, then they start at the same time on the same days 
    private int lecHash;
    // The location hash of this slot in tutorial time
    // If two slots have the same tutHash, then they start at the same time on the same days
    private int tutHash; 
    // The maximum number of lectures/tutorials that can be assigned to this slot
    private int max;
    // The maximum number of active learning lectures/tutorials that can be assigned to this slot
    private int almax;
    // The minimum number of lectures/tutorials that can be assigned to this slot
    private int min;
    // Is this an evening slot
    private boolean isEvening = false;
    
    // Time information
    private int day; // The day (0: Monday, 1: Tuesday, 2: Wednesday, 3: Thursday, 4: Friday)
    private int hour; // The hour in military time (0-23)
    private int minute; // The minute (0-59)

    // The name for printing
    private String name;

    /**
     * Constructor for Slot
     */
    public Slot(int id, int day, int hour, int minute, String name, int max, int min, int almax) {
        this.id = id;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.name = name;
        this.max = max;
        this.min = min;
        this.almax = almax;
        setupSlot();
    }

    /**
     * Print formatted slot information
     */
    public void printSlot() {
        String dayName = getDayName(day);
        System.out.printf("Slot: %s (ID: %d)%n", name, id);
        System.out.printf("  Time: %s %02d:%02d%n", dayName, hour, minute);
        System.out.printf("  Lecture Hash: %d, Tutorial Hash: %d%n", lecHash, tutHash);
        System.out.printf("  Capacity: max=%d, min=%d, almax=%d%n", max, min, almax);
        System.out.printf("  Evening slot: %s%n", isEvening ? "Yes" : "No");
    }

    /**
     * Setup the basic slot properties
     */
    private void setupSlot() {
        lecHash = calculateSlotHash(true);
        tutHash = calculateSlotHash(false);
        
        // Determine if this is an evening lecture
        if (hour >= EVENING_START_HOUR) {
            isEvening = true;
        }
    }

    /**
     * Treat this as a tutorial slot, get the lecture hashes of lecture slots that overlap this time
     * @return list of slot lecture hashes
     */
    public List<Integer> getOverlappingLectureHashesForTutorial() {
        List<Integer> overlappingHashes = new ArrayList<>();
        
        if (day == FRIDAY) {
            // A Friday tutorial slot covers two consecutive lecture slots
            overlappingHashes.add(lecHash);
            overlappingHashes.add(lecHash + FRIDAY_TUTORIAL_OFFSET);
        } else if (day == TUESDAY || day == THURSDAY) {
            // A Tuesday or Thursday tutorial can cover different lecture slots based on start time
            if ((hour % 3) == 2) {
                // Same start time as a lecture
                overlappingHashes.add(tutHash);
            } else if ((hour % 3) == 0) {
                // Covers lecture before and after
                overlappingHashes.add(tutHash - TUESDAY_THURSDAY_TUT_OFFSET_1);
                overlappingHashes.add(tutHash + TUESDAY_THURSDAY_TUT_OFFSET_2);
            } else {
                // Covers lecture before
                overlappingHashes.add(tutHash - TUESDAY_THURSDAY_TUT_OFFSET_2);
            }
        } else {
            // Monday or Wednesday - only covers one lecture slot
            overlappingHashes.add(lecHash);
        }
        
        return overlappingHashes;
    }

    /**
     * Treat this as a lecture slot, get the tutorial hashes of tutorial slots that overlap this time
     * @return list of tutorial slot hashes
     */
    public List<Integer> getOverlappingTutorialHashesForLecture() {
        List<Integer> overlappingHashes = new ArrayList<>();
        
        if (day == MONDAY || day == WEDNESDAY || day == FRIDAY) {
            // For MWF, check direct mapping and Friday overlap
            overlappingHashes.add(lecHash); // For Monday/Wednesday tutorials
            
            // Friday specific logic
            if ((hour % 2) == 0) {
                // Even hour - same as tutorial hash
                overlappingHashes.add(lecHash + FRIDAY_LEC_OFFSET_EVEN);
            } else {
                // Odd hour - offset from tutorial hash
                overlappingHashes.add(lecHash + FRIDAY_LEC_OFFSET_ODD);
            }
        } else {
            // Tuesday or Thursday lecture covers two tutorial slots
            if (minute == 0) {
                // Covers tutorials at current hash and next hash
                overlappingHashes.add(lecHash);
                overlappingHashes.add(lecHash + MINUTES_PER_HOUR);
            } else {
                // Covers tutorials at +30 and -30 minutes
                overlappingHashes.add(lecHash - TUESDAY_THURSDAY_TUT_OFFSET_2);
                overlappingHashes.add(lecHash + TUESDAY_THURSDAY_TUT_OFFSET_2);
            }
        }
        
        return overlappingHashes;
    }

    /**
     * Get the unique hash for this slot, representing the time in minutes from Monday 0:00
     * @param isLectureSlot true for lecture slot (MWF map to same time, TTh map to same time)
     *                     false for tutorial slot (MW map to same time, TTh map to same time, Friday unique)
     * @return the time in minutes from Monday 0:00 that this slot starts at
     */
    private int calculateSlotHash(boolean isLectureSlot) {
        int dayEquivalent;
        
        if (isLectureSlot) {
            // For lectures: Monday, Wednesday, Friday map to same; Tuesday, Thursday map to same
            if (day == MONDAY || day == WEDNESDAY || day == FRIDAY) {
                dayEquivalent = 0;
            } else {
                dayEquivalent = 1;
            }
        } else {
            // For tutorials: Monday, Wednesday map to same; Tuesday, Thursday map to same; Friday is unique
            if (day == MONDAY || day == WEDNESDAY) {
                dayEquivalent = 0;
            } else if (day == TUESDAY || day == THURSDAY) {
                dayEquivalent = 1;
            } else {
                dayEquivalent = 4; // Friday
            }
        }
        
        return minute + hour * MINUTES_PER_HOUR + dayEquivalent * MINUTES_PER_DAY;
    }

    /**
     * Check if this slot overlaps with the department meeting on Tuesday 11:00-12:30
     * @return true if overlapping, false otherwise
     */
    public boolean overlapsTuesdayMeeting() {
        if (day >= MEETING_DAY_START && day <= MEETING_DAY_END) {
            if (hour == MEETING_START_HOUR) {
                return true;
            } else if (hour == MEETING_END_HOUR && minute <= MEETING_END_MINUTE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method to get day name from day number
     */
    private String getDayName(int day) {
        switch (day) {
            case MONDAY: return "Monday";
            case TUESDAY: return "Tuesday";
            case WEDNESDAY: return "Wednesday";
            case THURSDAY: return "Thursday";
            case FRIDAY: return "Friday";
            default: return "Unknown";
        }
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getLecHash() { return lecHash; }
    public int getTutHash() { return tutHash; }
    
    public int getMax() { return max; }
    public void setMax(int max) { this.max = max; }
    
    public int getAlmax() { return almax; }
    public void setAlmax(int almax) { this.almax = almax; }
    
    public int getMin() { return min; }
    public void setMin(int min) { this.min = min; }
    
    public boolean isEvening() { return isEvening; }
    
    public int getDay() { return day; }
    public void setDay(int day) { 
        this.day = day; 
        setupSlot(); // Recalculate hashes if day changes
    }
    
    public int getHour() { return hour; }
    public void setHour(int hour) { 
        this.hour = hour; 
        setupSlot(); // Recalculate hashes if hour changes
    }
    
    public int getMinute() { return minute; }
    public void setMinute(int minute) { 
        this.minute = minute; 
        setupSlot(); // Recalculate hashes if minute changes
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    /**
     * Convenience method to get formatted time string
     */
    public String getFormattedTime() {
        return String.format("%02d:%02d", hour, minute);
    }
    
    /**
     * Convenience method to get day name
     */
    public String getDayName() {
        return getDayName(day);
    }
    
    @Override
    public String toString() {
        return String.format("Slot[%s: %s %02d:%02d]", name, getDayName(), hour, minute);
    }
}