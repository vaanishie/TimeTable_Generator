import java.util.*;

/*
   ConflictDetector.java  —  Post-Generation Conflict Analysis
   After the timetable is generated, this class scans it for
   any constraint violations and marks affected slots so the
   TableView can highlight them in red.
 
   CONFLICTS DETECTED
   ───────────────────
    CF1. Same subject in consecutive periods on the same day.
    CF2. Same teacher assigned to more than one class in the
         same period on the same day (teacher double-booking).
         (Only relevant when multiple class timetables are
          passed in — future extension.)
    CF3. A single subject appears more than maxAllowed times
         in one day (overload).
    Each detected conflict is recorded in a ConflictReport
    that the UI can display to the user.
 */
public class ConflictDetector {

    //Inner class: one conflict record
    public static class ConflictReport {
        public final int    day;
        public final int    period;
        public final String description;

        public ConflictReport(int day, int period, String description) {
            this.day         = day;
            this.period      = period;
            this.description = description;
        }

        @Override
        public String toString() {
            String[] dayNames    = {"Mon","Tue","Wed","Thu","Fri"};
            return "Day " + dayNames[day] + ", P" + (period + 1) + ": " + description;
        }
    }

    // Public API

    /**
     Scans the timetable, sets the conflict flag on affected slots,
     and returns a list of human-readable conflict reports.
     @param table 2-D array [DAYS][PERIODS] of TimetableSlot objects
     @return list of ConflictReport (empty if no conflicts found)
     */
    public List<ConflictReport> detect(TimetableSlot[][] table) {
        List<ConflictReport> reports = new ArrayList<>();

        for (int day = 0; day < Generator.DAYS; day++) {
            reports.addAll(detectConsecutive(table, day));
            reports.addAll(detectDailyOverload(table, day));
        }

        return reports;
    }

    // Private detector methods 

    // CF1: Flags slots where a subject appears in two consecutive periods.

    private List<ConflictReport> detectConsecutive(TimetableSlot[][] table, int day) {
        List<ConflictReport> found = new ArrayList<>();

        for (int p = 1; p < Generator.PERIODS; p++) {
            TimetableSlot curr = table[day][p];
            TimetableSlot prev = table[day][p - 1];

            if (curr == null || prev == null) continue;

            if (curr.getSubject().equals(prev.getSubject())) {
                curr.setConflict(true);
                prev.setConflict(true);
                found.add(new ConflictReport(day, p,
                    "Consecutive repeat: " + curr.getSubject()));
            }
        }

        return found;
    }

    //CF3: Flags slots when a subject appears more than ceil(PERIODS / numDistinctSubjects) + 1 times in a day.
   
    private List<ConflictReport> detectDailyOverload(TimetableSlot[][] table, int day) {
        List<ConflictReport> found = new ArrayList<>();

        // Count occurrences of each subject today
        Map<String, Integer> count = new HashMap<>();
        for (int p = 0; p < Generator.PERIODS; p++) {
            TimetableSlot slot = table[day][p];
            if (slot == null) continue;
            count.merge(slot.getSubject(), 1, Integer::sum);
        }

        // Find how many distinct subjects appear
        int distinct  = count.size();
        int maxAllowed = (distinct == 0) ? Generator.PERIODS
                         : (int) Math.ceil((double) Generator.PERIODS / distinct) + 1;

        for (Map.Entry<String, Integer> entry : count.entrySet()) {
            if (entry.getValue() > maxAllowed) {
                // Mark all slots with this subject on this day
                for (int p = 0; p < Generator.PERIODS; p++) {
                    TimetableSlot slot = table[day][p];
                    if (slot != null && slot.getSubject().equals(entry.getKey())) {
                        slot.setConflict(true);
                        found.add(new ConflictReport(day, p,
                            "Overloaded: " + entry.getKey()
                            + " appears " + entry.getValue() + "× today"));
                    }
                }
            }
        }

        return found;
    }
}
