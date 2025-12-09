package schedulesearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The InputParser class has static methods for parsing the input file and returning
 * the environment "Env" and the starting state "s0"
 */
public final class Functions {
    
    // Change this to get the parsing details
    private static final boolean PRINT_DATA = false;
    
    // The names of input variables
    private static final String[] HEADINGS = {
        "Name:", "Lecture slots:", "Tutorial slots:", "Lectures:", 
        "Tutorials:", "Not compatible:", "Unwanted:", "Preferences:", 
        "Pair:", "Partial assignments:"
    };
    
    // Constants for parsing
    private static final String TUTORIAL_MARKER = "TUT";
    private static final String LAB_MARKER = "LAB";
    private static final String LECTURE_MARKER = "LEC";
    private static final String TRUE_VALUE = "true";
    
    // Day abbreviations
    private static final Map<String, Integer> DAY_MAPPING = Map.of(
        "MO", Slot.MONDAY,
        "TU", Slot.TUESDAY,
        "WE", Slot.WEDNESDAY,
        "TR", Slot.THURSDAY,
        "FR", Slot.FRIDAY
    );
    
    /**
     * Parses an input file for the environment variables and the starting state.
     * The parser first gets all information from the file and creates the environment.
     * Then it uses any partial assignments to populate the starting state.
     * If there are any errors in parsing the file, or if the partial assignments
     * are unsatisfiable, then return an error.
     * 
     * @param inputFile the input file to parse
     * @param env the environment to return
     * @param s0 the start state to return
     * @return false if there was an error, otherwise true
     */
    public static boolean parseInputFile(String inputFile, Environment env, Problem s0) {
        File file = new File(inputFile);

        // Check that the file exists
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            System.err.println("PARSE ERROR: Could not load from file: " + inputFile);
            return false;
        }

        System.out.println("\n\nReading from file: " + inputFile);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return parseFileContents(reader, env, s0);
        } catch (IOException e) {
            System.err.println("PARSE ERROR: Could not read from file: " + inputFile);
            return false;
        }
    }

    /**
     * Main parsing method that orchestrates the parsing of different sections
     */
    private static boolean parseFileContents(BufferedReader reader, Environment env, Problem s0) throws IOException {
        // Parse dataset name
        env.datasetName = parseDatasetName(reader);
        if (env.datasetName == null || env.datasetName.isEmpty()) {
            System.err.println("PARSE ERROR: Could not get dataset name");
            return false;
        }

        // Parse slots and data
        Map<Integer, Slot> lectureSlots = parseSlotSection(reader, HEADINGS[1], true);
        Map<Integer, Slot> tutorialSlots = parseSlotSection(reader, HEADINGS[2], false);
        
        if (lectureSlots == null || tutorialSlots == null) {
            return false;
        }

        // Process lecture slots
        env.lectureSlots = lectureSlots;
        env.lecSlotsArray = new Slot[lectureSlots.size()];
        processLectureSlots(env);

        // Process tutorial slots
        env.tutorialSlots = tutorialSlots;
        env.tutSlotsArray = new Slot[tutorialSlots.size()];
        env.tutSlotLecSlot = new int[env.tutSlotsArray.length][];
        processTutorialSlots(env);

        // Create slot mapping
        createSlotMappings(env);

        // Parse lecture and tutorial data
        CourseDataContainer courseData = parseCourseData(reader);
        if (courseData == null) {
            return false;
        }

        // Handle special courses
        handleSpecialCourses(courseData);

        // Process all course data into environment
        if (!processAllCourseData(env, courseData)) {
            return false;
        }

        // Parse constraints and preferences
        if (!parseConstraints(reader, env, courseData)) {
            return false;
        }

        // Setup environment
        env.SetupEnvironment();

        // Apply partial assignments
        return applyPartialAssignments(reader, env, s0, courseData);
    }

    /**
     * Parses a slot section from the file
     */
    private static Map<Integer, Slot> parseSlotSection(BufferedReader reader, String heading, boolean isLecture) throws IOException {
        skipToHeading(reader, heading);
        
        Map<Integer, Slot> slots = new HashMap<>();
        String line = reader.readLine();
        
        while (line != null && !line.contains(getNextHeading(heading))) {
            if (!line.trim().isEmpty()) {
                Slot slot = parseSlotLine(line);
                if (slot != null) {
                    int hash = isLecture ? slot.getLecHash() : slot.getTutHash();
                    slots.put(hash, slot);
                } else {
                    System.err.println("INPUT WARNING: Invalid slot information in line: " + line);
                }
            }
            line = reader.readLine();
        }
        
        if (line == null || !line.contains(getNextHeading(heading))) {
            System.err.println("PARSE ERROR: Could not find next heading after: " + heading);
            return null;
        }
        
        return slots;
    }

    /**
     * Gets the next heading after the current one
     */
    private static String getNextHeading(String currentHeading) {
        for (int i = 0; i < HEADINGS.length - 1; i++) {
            if (HEADINGS[i].equals(currentHeading)) {
                return HEADINGS[i + 1];
            }
        }
        return "";
    }

    /**
     * Parses a single slot line
     */
    private static Slot parseSlotLine(String line) {
        String[] elements = line.split(",");
        if (elements.length != 5) {
            return null;
        }

        String dayStr = elements[0].trim();
        String timeStr = elements[1].trim();
        
        Integer day = DAY_MAPPING.get(dayStr);
        if (day == null) {
            return null;
        }

        String[] timeParts = timeStr.split(":");
        if (timeParts.length != 2) {
            return null;
        }

        try {
            int hour = Integer.parseInt(timeParts[0].trim());
            int minute = Integer.parseInt(timeParts[1].trim());
            int max = Integer.parseInt(elements[2].trim());
            int min = Integer.parseInt(elements[3].trim());
            int almax = Integer.parseInt(elements[4].trim());

            String name = dayStr + "," + timeStr;
            return new Slot(-1, day, hour, minute, name, max, min, almax);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Processes lecture slots into environment arrays
     */
    private static void processLectureSlots(Environment env) {
        int index = 0;
        for (Slot slot : env.lectureSlots.values()) {
            if (slot.getLecHash() == 2100) { // Tuesday 11:00 AM
                System.out.println("INPUT NOTICE: Tuesday 11:00 am lecture found");
                env.tue11SlotId = index;
            }
            env.lecSlotsArray[index] = slot;
            slot.setId(index);
            index++;
        }
    }

    /**
     * Processes tutorial slots into environment arrays
     */
    private static void processTutorialSlots(Environment env) {
        int index = 0;
        for (Slot slot : env.tutorialSlots.values()) {
            env.tutSlotsArray[index] = slot;
            
            // Find overlapping lecture slots
            List<Integer> lecSlots = slot.getOverlappingLectureHashesForTutorial().stream()
                .filter(hash -> env.lectureSlots.containsKey(hash))
                .map(hash -> env.lectureSlots.get(hash).getId())
                .collect(Collectors.toList());
            
            env.tutSlotLecSlot[index] = lecSlots.stream().mapToInt(i -> i).toArray();
            slot.setId(index);
            index++;
        }
    }

    /**
     * Creates mappings between lecture and tutorial slots
     */
    private static void createSlotMappings(Environment env) {
        List<int[]> lecToTutList = new ArrayList<>();
        
        for (Slot lectureSlot : env.lecSlotsArray) {
            List<Integer> tutSlots = lectureSlot.getOverlappingTutorialHashesForLecture().stream()
                .filter(hash -> env.tutorialSlots.containsKey(hash))
                .map(hash -> env.tutorialSlots.get(hash).getId())
                .collect(Collectors.toList());
            
            lecToTutList.add(tutSlots.stream().mapToInt(i -> i).toArray());
        }
        
        env.lecSlotTutSlot = lecToTutList.toArray(new int[0][]);
    }

    /**
     * Container for course data during parsing
     */
    private static class CourseDataContainer {
        Map<String, Map<Integer, LectureData>> lectureMap = new HashMap<>();
        boolean cpsc413Found = false;
        boolean cpsc351Found = false;
    }

    /**
     * Parses all course data (lectures and tutorials)
     */
    private static CourseDataContainer parseCourseData(BufferedReader reader) throws IOException {
        CourseDataContainer container = new CourseDataContainer();
        
        // Parse lectures
        if (!parseLectureSection(reader, container)) {
            return null;
        }
        
        // Parse tutorials
        if (!parseTutorialSection(reader, container)) {
            return null;
        }
        
        return container;
    }

    /**
     * Handles special courses (CPSC 413 and CPSC 351)
     */
    private static void handleSpecialCourses(CourseDataContainer container) {
        if (container.lectureMap.containsKey("CPSC 413")) {
            System.out.println("INPUT NOTICE: CPSC 413 found");
            container.cpsc413Found = true;
            addSpecialTutorial(container, "CPSC 413", "CPSC 913 TUT 01");
        }
        
        if (container.lectureMap.containsKey("CPSC 351")) {
            System.out.println("INPUT NOTICE: CPSC 351 found");
            container.cpsc351Found = true;
            addSpecialTutorial(container, "CPSC 351", "CPSC 851 TUT 01");
        }
    }

    /**
     * Adds a special tutorial for a course
     */
    private static void addSpecialTutorial(CourseDataContainer container, String course, String tutName) {
        TutorialData tut = new TutorialData();
        tut.courseDescriptor = course;
        tut.tutNum = 0;
        tut.isEvening = false;
        tut.isAl = false;
        tut.useSection = false;
        tut.name = tutName;

        Map<Integer, LectureData> courseLectures = container.lectureMap.get(course);
        if (!courseLectures.isEmpty()) {
            LectureData firstLecture = courseLectures.values().iterator().next();
            tut.lecNum = firstLecture.lecNum;
            firstLecture.tutorials.add(tut);
        }
    }

    /**
     * Processes all course data into the environment
     */
    private static boolean processAllCourseData(Environment env, CourseDataContainer container) {
        // Count total lectures and tutorials
        env.numLectures = container.lectureMap.values().stream()
            .mapToInt(Map::size)
            .sum();
        
        env.numTutorials = container.lectureMap.values().stream()
            .flatMap(m -> m.values().stream())
            .mapToInt(lec -> lec.tutorials.size())
            .sum();

        // Convert to arrays
        env.tutorials = new Tutorial[env.numTutorials];
        env.lectures = new Lecture[env.numLectures];
        
        List<Integer> lec5xxList = new ArrayList<>();
        
        int tutorialId = 0;
        int lectureId = 0;
        int sectionId = 0;
        
        for (Map<Integer, LectureData> courseLectures : container.lectureMap.values()) {
            int[] sectionLectureIds = new int[courseLectures.size()];
            int lectureIndex = 0;
            
            for (LectureData lectureData : courseLectures.values()) {
                // Record 5xx lectures
                if (lectureData.is5xx) {
                    lec5xxList.add(lectureId);
                }
                
                // Convert tutorials
                for (TutorialData tutData : lectureData.tutorials) {
                    tutData.id = tutorialId;
                    env.tutorials[tutorialId] = tutData.convertToTutorial(tutorialId, sectionId, lectureId);
                    tutorialId++;
                }
                
                // Convert lecture
                lectureData.id = lectureId;
                sectionLectureIds[lectureIndex] = lectureId;
                env.lectures[lectureId] = lectureData.convertToLecture(lectureId, sectionId);
                lectureId++;
                lectureIndex++;
            }
            
            env.sections.put(sectionId, sectionLectureIds);
            sectionId++;
        }
        
        // Store 5xx lectures
        env.lectures5xx = lec5xxList.stream().mapToInt(i -> i).toArray();
        
        // Create tutorial mappings
        createTutorialMappings(env);
        
        return true;
    }

    /**
     * Creates tutorial-to-lecture mappings
     */
    private static void createTutorialMappings(Environment env) {
        List<List<Integer>> tutorialMap = new ArrayList<>(env.numLectures);
        for (int i = 0; i < env.numLectures; i++) {
            tutorialMap.add(new ArrayList<>());
        }
        
        for (int i = 0; i < env.tutorials.length; i++) {
            Tutorial tutorial = env.tutorials[i];
            
            if (tutorial.section != -1) {
                int[] sectionLectures = env.sections.get(tutorial.section);
                tutorial.parentLectures = sectionLectures;
                
                for (int lectureId : sectionLectures) {
                    tutorialMap.get(lectureId).add(i);
                }
            } else {
                tutorialMap.get(tutorial.parentLectures[0]).add(i);
            }
        }
        
        // Assign tutorials to lectures
        for (int i = 0; i < env.numLectures; i++) {
            List<Integer> tutorialIds = tutorialMap.get(i);
            env.lectures[i].tutorials = tutorialIds.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    /**
     * Parses constraints (not compatible, unwanted, preferences, pairs)
     */
    private static boolean parseConstraints(BufferedReader reader, Environment env, 
                                          CourseDataContainer container) throws IOException {
        return parseNotCompatible(reader, env, container) &&
               parseUnwanted(reader, env, container) &&
               parsePreferences(reader, env, container) &&
               parsePairs(reader, env, container);
    }

    /**
     * Applies partial assignments to the starting state
     */
    private static boolean applyPartialAssignments(BufferedReader reader, Environment env, 
                                                  Problem s0, CourseDataContainer container) throws IOException {
        List<AssignmentPair> partialAssignLec = new ArrayList<>();
        List<AssignmentPair> partialAssignTut = new ArrayList<>();
        
        if (!parsePartialAssignments(reader, env, container, partialAssignTut, partialAssignLec)) {
            return false;
        }
        
        // Apply special constraints
        applySpecialConstraints(env, container, partialAssignTut);
        
        // Create initial problem
        s0.setupProblem(env.numLectures, env.numTutorials, 
                       env.lecSlotsArray.length, env.tutSlotsArray.length);
        
        // Apply partial assignments
        return applyAllAssignments(s0, env, partialAssignLec, partialAssignTut);
    }

    /**
     * Applies special constraints for CPSC 413/351
     */
    private static void applySpecialConstraints(Environment env, CourseDataContainer container, 
                                               List<AssignmentPair> partialAssignTut) {
        if (container.cpsc351Found) {
            applyCpsc351Constraints(env, container, partialAssignTut);
        }
        
        if (container.cpsc413Found) {
            applyCpsc413Constraints(env, container, partialAssignTut);
        }
    }

    /**
     * Helper method to skip to a specific heading
     */
    private static void skipToHeading(BufferedReader reader, String heading) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(heading)) {
                return;
            }
        }
    }

    /**
     * Parses the dataset name
     */
    private static String parseDatasetName(BufferedReader reader) throws IOException {
        skipToHeading(reader, HEADINGS[0]);
        
        String line;
        while ((line = reader.readLine()) != null && line.trim().isEmpty()) {
            // Skip empty lines
        }
        
        return line != null ? line.trim() : null;
    }

    // Additional parsing methods would follow similar patterns...
    // Due to length constraints, I've shown the main structure and key improvements.

    /**
     * Data class for assignment pairs
     */
    private static class AssignmentPair {
        boolean isLecture;
        int id;
        int slotId;
    }
    /**
     * Parses the lecture section from the file
     */
    private static boolean parseLectureSection(BufferedReader reader, 
                                              CourseDataContainer container) throws IOException {
        skipToHeading(reader, HEADINGS[3]);
        
        String line = reader.readLine();
        while (line != null && !line.contains(HEADINGS[4])) {
            if (!line.trim().isEmpty()) {
                LectureData lectureData = parseLectureLine(line);
                if (lectureData != null) {
                    container.lectureMap
                        .computeIfAbsent(lectureData.courseDescriptor, k -> new HashMap<>())
                        .putIfAbsent(lectureData.lecNum, lectureData);
                } else {
                    System.err.println("INPUT WARNING: (Lectures) Invalid information in line: " + line);
                }
            }
            line = reader.readLine();
        }
        
        return line != null && line.contains(HEADINGS[4]);
    }

    /**
     * Parses a single lecture line
     */
    private static LectureData parseLectureLine(String line) {
        String[] elements = line.split(",", 3);
        if (elements.length < 3) {
            return null;
        }

        try {
            LectureData lecture = new LectureData();
            lecture.name = elements[0].trim();
            
            // Parse course descriptor and lecture number
            String[] nameParts = elements[0].split(LECTURE_MARKER);
            if (nameParts.length != 2) {
                return null;
            }
            
            lecture.courseDescriptor = nameParts[0].trim();
            
            String lecNumStr = nameParts[1].trim().split("\\s+")[0];
            lecture.lecNum = Integer.parseInt(lecNumStr);
            
            // Check for evening lecture
            lecture.isEvening = lecNumStr.startsWith("9");
            
            // Check for 500-level course
            String[] courseParts = lecture.courseDescriptor.split("\\s+");
            if (courseParts.length >= 2) {
                lecture.is5xx = courseParts[1].startsWith("5");
            }
            
            // Parse AL flag
            lecture.isAl = elements[2].trim().equalsIgnoreCase(TRUE_VALUE);
            
            return lecture;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Parses the tutorial section from the file
     */
    private static boolean parseTutorialSection(BufferedReader reader, 
                                               CourseDataContainer container) throws IOException {
        String line = reader.readLine();
        while (line != null && !line.contains(HEADINGS[5])) {
            if (!line.trim().isEmpty()) {
                TutorialData tutorialData = parseTutorialLine(line);
                if (tutorialData != null) {
                    addTutorialToContainer(container, tutorialData);
                } else {
                    System.err.println("INPUT WARNING: (Tutorials) Invalid information in line: " + line);
                }
            }
            line = reader.readLine();
        }
        
        return line != null && line.contains(HEADINGS[5]);
    }

    /**
     * Adds a tutorial to the container, linking it to its lecture
     */
    private static void addTutorialToContainer(CourseDataContainer container, TutorialData tutorial) {
        Map<Integer, LectureData> courseLectures = container.lectureMap.get(tutorial.courseDescriptor);
        if (courseLectures == null) {
            System.err.println("INPUT WARNING: Tutorial without corresponding lecture: " + tutorial.courseDescriptor);
            return;
        }
        
        LectureData lecture = courseLectures.get(tutorial.lecNum);
        if (lecture == null) {
            System.err.println("INPUT WARNING: Tutorial without corresponding lecture number: " + 
                             tutorial.courseDescriptor + " LEC " + tutorial.lecNum);
            return;
        }
        
        lecture.tutorials.add(tutorial);
    }

    /**
     * Parses a single tutorial line
     */
    private static TutorialData parseTutorialLine(String line) {
        String[] elements = line.split(",", 3);
        if (elements.length < 3) {
            return null;
        }

        try {
            TutorialData tutorial = new TutorialData();
            tutorial.name = elements[0].trim();
            
            // Parse course descriptor and numbers
            if (tutorial.name.contains(TUTORIAL_MARKER)) {
                parseTutorialWithMarker(tutorial, TUTORIAL_MARKER);
            } else if (tutorial.name.contains(LAB_MARKER)) {
                parseTutorialWithMarker(tutorial, LAB_MARKER);
            } else {
                return null;
            }
            
            // Parse AL flag
            tutorial.isAl = elements[2].trim().equalsIgnoreCase(TRUE_VALUE);
            
            return tutorial;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Parses tutorial information with a specific marker (TUT or LAB)
     */
    private static void parseTutorialWithMarker(TutorialData tutorial, String marker) {
        String[] parts = tutorial.name.split(marker);
        String leftPart = parts[0].trim();
        String rightPart = parts[1].trim();
        
        // Parse tutorial number
        tutorial.tutNum = Integer.parseInt(rightPart.split("\\s+")[0]);
        
        // Parse lecture information if present
        if (leftPart.contains(LECTURE_MARKER)) {
            String[] lecParts = leftPart.split(LECTURE_MARKER);
            tutorial.courseDescriptor = lecParts[0].trim();
            tutorial.lecNum = Integer.parseInt(lecParts[1].trim().split("\\s+")[0]);
            tutorial.isEvening = lecParts[1].trim().startsWith("9");
            tutorial.useSection = false;
        } else {
            tutorial.courseDescriptor = leftPart;
            tutorial.lecNum = 1; // Default
            tutorial.useSection = true;
        }
    }

    /**
     * Parses the "Not Compatible" section
     */
    private static boolean parseNotCompatible(BufferedReader reader, Environment env,
                                             CourseDataContainer container) throws IOException {
        String line = reader.readLine();
        while (line != null && !line.contains(HEADINGS[6])) {
            if (!line.trim().isEmpty()) {
                Pair pair = parsePairLine(line, container);
                if (pair != null) {
                    applyNotCompatibleConstraint(env, pair);
                } else {
                    System.err.println("INPUT WARNING: (Not Compatible) Invalid information in line: " + line);
                }
            }
            line = reader.readLine();
        }
        
        return line != null && line.contains(HEADINGS[6]);
    }

    /**
     * Parses a pair line for not-compatible constraints
     */
    private static Pair parsePairLine(String line, CourseDataContainer container) {
        String[] elements = line.split(",");
        if (elements.length != 2) {
            return null;
        }
        
        try {
            Pair pair = new Pair();
            pair.id1 = parseEntityId(elements[0].trim(), container);
            pair.id2 = parseEntityId(elements[1].trim(), container);
            
            if (pair.id1 == -1 || pair.id2 == -1) {
                return null;
            }
            
            pair.isLec1 = !(elements[0].contains(TUTORIAL_MARKER) || elements[0].contains(LAB_MARKER));
            pair.isLec2 = !(elements[1].contains(TUTORIAL_MARKER) || elements[1].contains(LAB_MARKER));
            
            return pair;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses an entity ID from a string
     */
    private static int parseEntityId(String entityStr, CourseDataContainer container) {
        if (entityStr.contains(LECTURE_MARKER)) {
            return parseLectureId(entityStr, container);
        } else if (entityStr.contains(TUTORIAL_MARKER) || entityStr.contains(LAB_MARKER)) {
            return parseTutorialId(entityStr, container);
        }
        return -1;
    }

    /**
     * Applies a not-compatible constraint to the environment
     */
    private static void applyNotCompatibleConstraint(Environment env, Pair pair) {
        if (pair.isLec1) {
            if (pair.isLec2) {
                env.lectures[pair.id1].notCompatibleLec.add(pair.id2);
                env.lectures[pair.id2].notCompatibleLec.add(pair.id1);
            } else {
                env.lectures[pair.id1].notCompatibleTut.add(pair.id2);
                env.tutorials[pair.id2].notCompatibleLec.add(pair.id1);
            }
        } else {
            if (pair.isLec2) {
                env.tutorials[pair.id1].notCompatibleLec.add(pair.id2);
                env.lectures[pair.id2].notCompatibleTut.add(pair.id1);
            } else {
                env.tutorials[pair.id1].notCompatibleTut.add(pair.id2);
                env.tutorials[pair.id2].notCompatibleTut.add(pair.id1);
            }
        }
    }

    /**
     * Parses the "Unwanted" section
     */
    private static boolean parseUnwanted(BufferedReader reader, Environment env,
                                        CourseDataContainer container) throws IOException {
        String line = reader.readLine();
        while (line != null && !line.contains(HEADINGS[7])) {
            if (!line.trim().isEmpty()) {
                AssignmentPair pair = parseAssignmentLine(line, container);
                if (pair != null) {
                    applyUnwantedConstraint(env, pair);
                } else {
                    System.err.println("INPUT WARNING: (Unwanted) Invalid information in line: " + line);
                }
            }
            line = reader.readLine();
        }
        
        return line != null && line.contains(HEADINGS[7]);
    }

    /**
     * Parses an assignment line (for unwanted or preferences)
     */
    private static AssignmentPair parseAssignmentLine(String line, CourseDataContainer container) {
        String[] elements = line.split(",", 2);
        if (elements.length != 2) {
            return null;
        }
        
        try {
            AssignmentPair pair = new AssignmentPair();
            String entityStr = elements[0].trim();
            String slotStr = elements[1].trim();
            
            // Parse entity
            if (entityStr.contains(LECTURE_MARKER)) {
                pair.isLecture = true;
                pair.id = parseLectureId(entityStr, container);
            } else {
                pair.isLecture = false;
                pair.id = parseTutorialId(entityStr, container);
            }
            
            if (pair.id == -1) {
                return null;
            }
            
            // Parse slot
            Slot slot = parseBasicSlot(slotStr);
            if (slot == null) {
                return null;
            }
            
            pair.slotId = pair.isLecture ? slot.getLecHash() : slot.getTutHash();
            return pair;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses a basic slot from a string
     */
    private static Slot parseBasicSlot(String slotStr) {
        String[] elements = slotStr.split(",");
        if (elements.length < 2) {
            return null;
        }
        
        String dayStr = elements[0].trim();
        String timeStr = elements[1].trim();
        
        Integer day = DAY_MAPPING.get(dayStr);
        if (day == null) {
            return null;
        }
        
        String[] timeParts = timeStr.split(":");
        if (timeParts.length != 2) {
            return null;
        }
        
        try {
            int hour = Integer.parseInt(timeParts[0].trim());
            int minute = Integer.parseInt(timeParts[1].trim());
            return new Slot(-1, day, hour, minute, dayStr + "," + timeStr, 0, 0, 0);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Applies an unwanted constraint
     */
    private static void applyUnwantedConstraint(Environment env, AssignmentPair pair) {
        if (pair.isLecture) {
            Slot slot = findSlotByHash(env.lectureSlots, pair.slotId);
            if (slot != null) {
                env.lectures[pair.id].unwanted.add(slot.getId());
            }
        } else {
            Slot slot = findSlotByHash(env.tutorialSlots, pair.slotId);
            if (slot != null) {
                env.tutorials[pair.id].unwanted.add(slot.getId());
            }
        }
    }

    /**
     * Parses the "Preferences" section
     */
    private static boolean parsePreferences(BufferedReader reader, Environment env,
                                          CourseDataContainer container) throws IOException {
        String line = reader.readLine();
        while (line != null && !line.contains(HEADINGS[8])) {
            if (!line.trim().isEmpty()) {
                Preference preference = parsePreferenceLine(line, container);
                if (preference != null) {
                    applyPreference(env, preference);
                } else {
                    System.err.println("INPUT WARNING: (Preferences) Invalid information in line: " + line);
                }
            }
            line = reader.readLine();
        }
        
        return line != null && line.contains(HEADINGS[8]);
    }

    /**
     * Data class for preferences
     */
    private static class Preference {
        boolean isLecture;
        int id;
        int slotId;
        int value;
    }

    /**
     * Parses a preference line
     */
    private static Preference parsePreferenceLine(String line, CourseDataContainer container) {
        String[] elements = line.split(",", 4);
        if (elements.length != 4) {
            return null;
        }
        
        try {
            Preference preference = new Preference();
            
            // Parse slot
            Slot slot = parseBasicSlot(elements[0].trim() + "," + elements[1].trim());
            if (slot == null) {
                return null;
            }
            
            // Parse entity
            String entityStr = elements[2].trim();
            if (entityStr.contains(LECTURE_MARKER)) {
                preference.isLecture = true;
                preference.id = parseLectureId(entityStr, container);
                preference.slotId = slot.getLecHash();
            } else {
                preference.isLecture = false;
                preference.id = parseTutorialId(entityStr, container);
                preference.slotId = slot.getTutHash();
            }
            
            if (preference.id == -1) {
                return null;
            }
            
            // Parse preference value
            preference.value = Integer.parseInt(elements[3].trim());
            
            return preference;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Applies a preference to the environment
     */
    private static void applyPreference(Environment env, Preference preference) {
        Slot slot = findSlotByHash(
            preference.isLecture ? env.lectureSlots : env.tutorialSlots, 
            preference.slotId
        );
        
        if (slot != null) {
            int slotId = slot.getId();
            if (preference.isLecture) {
                env.lectures[preference.id].preferences.put(slotId, preference.value);
            } else {
                env.tutorials[preference.id].preferences.put(slotId, preference.value);
            }
        }
    }

    /**
     * Parses the "Pairs" section
     */
    private static boolean parsePairs(BufferedReader reader, Environment env,
                                     CourseDataContainer container) throws IOException {
        List<Pair> pairs = new ArrayList<>();
        String line = reader.readLine();
        
        while (line != null && !line.contains(HEADINGS[9])) {
            if (!line.trim().isEmpty()) {
                Pair pair = parsePairLine(line, container);
                if (pair != null) {
                    pairs.add(pair);
                } else {
                    System.err.println("INPUT WARNING: (Pairs) Invalid information in line: " + line);
                }
            }
            line = reader.readLine();
        }
        
        if (line == null || !line.contains(HEADINGS[9])) {
            return false;
        }
        
        env.pairs = pairs.toArray(new Pair[0]);
        return true;
    }

    /**
     * Parses the "Partial Assignments" section
     */
    private static boolean parsePartialAssignments(BufferedReader reader, Environment env,
                                                  CourseDataContainer container,
                                                  List<AssignmentPair> partialAssignTut,
                                                  List<AssignmentPair> partialAssignLec) throws IOException {
        String line = reader.readLine();
        while (line != null) {
            if (!line.trim().isEmpty()) {
                AssignmentPair pair = parseAssignmentLine(line, container);
                if (pair != null) {
                    if (pair.isLecture) {
                        Slot slot = findSlotByHash(env.lectureSlots, pair.slotId);
                        if (slot != null) {
                            pair.slotId = slot.getId();
                            partialAssignLec.add(pair);
                        }
                    } else {
                        Slot slot = findSlotByHash(env.tutorialSlots, pair.slotId);
                        if (slot != null) {
                            pair.slotId = slot.getId();
                            partialAssignTut.add(pair);
                        }
                    }
                } else {
                    System.err.println("INPUT WARNING: (Partial Assignments) Invalid information in line: " + line);
                }
            }
            line = reader.readLine();
        }
        
        return true;
    }

    /**
     * Applies special constraints for CPSC 351
     */
    private static void applyCpsc351Constraints(Environment env, CourseDataContainer container,
                                               List<AssignmentPair> partialAssignTut) {
        applySpecialCourseConstraints(env, container, "CPSC 351", "CPSC 851 TUT 01", partialAssignTut);
    }

    /**
     * Applies special constraints for CPSC 413
     */
    private static void applyCpsc413Constraints(Environment env, CourseDataContainer container,
                                               List<AssignmentPair> partialAssignTut) {
        applySpecialCourseConstraints(env, container, "CPSC 413", "CPSC 913 TUT 01", partialAssignTut);
    }

    /**
     * Applies special constraints for a course
     */
    private static void applySpecialCourseConstraints(Environment env, CourseDataContainer container,
                                                     String courseCode, String tutorialName,
                                                     List<AssignmentPair> partialAssignTut) {
        // Find tutorial
        int tutorialId = findTutorialByName(env.tutorials, tutorialName);
        if (tutorialId == -1) {
            System.err.println("PARSE ERROR: " + courseCode + " exists but " + tutorialName + " not found");
            return;
        }
        
        // Find Tuesday 18:00 slot
        int slotId = findSlotByTime(env.tutSlotsArray, Slot.TUESDAY, 18, 0);
        if (slotId == -1) {
            System.err.println("PARSE ERROR: " + courseCode + " exists but TU 18:00 slot not found");
            return;
        }
        
        // Add partial assignment
        AssignmentPair assignment = new AssignmentPair();
        assignment.isLecture = false;
        assignment.id = tutorialId;
        assignment.slotId = slotId;
        partialAssignTut.add(assignment);
        
        // Add incompatibilities
        Map<Integer, LectureData> courseLectures = container.lectureMap.get(courseCode);
        if (courseLectures != null) {
            for (LectureData lectureData : courseLectures.values()) {
                // Incompatible with lecture
                env.lectures[lectureData.id].notCompatibleTut.add(tutorialId);
                env.tutorials[tutorialId].notCompatibleLec.add(lectureData.id);
                
                // Incompatible with other tutorials
                for (TutorialData tutorialData : lectureData.tutorials) {
                    if (tutorialData.id != tutorialId) {
                        env.tutorials[tutorialData.id].notCompatibleTut.add(tutorialId);
                        env.tutorials[tutorialId].notCompatibleTut.add(tutorialData.id);
                    }
                }
            }
        }
    }

    /**
     * Applies all partial assignments to the starting state
     */
    private static boolean applyAllAssignments(Problem s0, Environment env,
                                              List<AssignmentPair> partialAssignLec,
                                              List<AssignmentPair> partialAssignTut) {
        // Apply lecture assignments
        for (AssignmentPair pair : partialAssignLec) {
            if (!assignLecture(s0, env, pair.id, pair.slotId)) {
                return false;
            }
        }
        
        // Apply tutorial assignments
        for (AssignmentPair pair : partialAssignTut) {
            if (!assignTutorial(s0, env, pair.id, pair.slotId)) {
                return false;
            }
        }
        
        System.out.println("Initial problem after partial assignments");
        Functions.printProblem(s0, env);
        
        return true;
    }

    /**
     * Assigns a lecture to a slot
     */
    private static boolean assignLecture(Problem s0, Environment env, int lectureId, int slotId) {
        int[] validSlots = Functions.validLectureSlots(env, lectureId, s0);
        if (validSlots == null) {
            System.err.println("Invalid partial assignment: assigning lecture: " + lectureId + ", to slot: " + slotId);
            return false;
        }
        
        if (Arrays.stream(validSlots).anyMatch(s -> s == slotId)) {
            s0.assignLecture(lectureId, slotId, env.lectures[lectureId].isAl);
            return true;
        }
        
        System.err.println("Invalid partial assignment: assigning lecture: " + lectureId + ", to slot: " + slotId);
        return false;
    }

    /**
     * Assigns a tutorial to a slot
     */
    private static boolean assignTutorial(Problem s0, Environment env, int tutorialId, int slotId) {
        int[] validSlots = Functions.validTutSlots(env, tutorialId, s0);
        if (validSlots == null) {
            System.err.println("Invalid partial assignment: assigning tutorial: " + tutorialId + ", to slot: " + slotId);
            return false;
        }
        
        if (Arrays.stream(validSlots).anyMatch(s -> s == slotId)) {
            s0.assignTutorial(tutorialId, slotId, env.tutorials[tutorialId].isAl);
            return true;
        }
        
        System.err.println("Invalid partial assignment: assigning tutorial: " + tutorialId + ", to slot: " + slotId);
        return false;
    }

    // Helper methods
    private static Slot findSlotByHash(Map<Integer, Slot> slots, int hash) {
        return slots.get(hash);
    }
    
    private static int findTutorialByName(Tutorial[] tutorials, String name) {
        for (int i = 0; i < tutorials.length; i++) {
            if (tutorials[i].name.equals(name)) {
                return i;
            }
        }
        return -1;
    }
    
    private static int findSlotByTime(Slot[] slots, int day, int hour, int minute) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].getDay() == day && slots[i].getHour() == hour && slots[i].getMinute() == minute) {
                return i;
            }
        }
        return -1;
    }
    

}
}