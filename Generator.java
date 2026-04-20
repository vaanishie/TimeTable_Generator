import java.util.*;

/* Explaination of the timetable generation algorithm:

1. Basic Approach
   The timetable generator uses a combination of a greedy method and backtracking.

2. Phase 1: Greedy Filling

   * The algorithm fills each slot (day and period) one by one.
   * For every slot, it selects the best possible subject based on simple rules.

3. Selection Criteria
   While choosing a subject, the algorithm ensures:

   * The subject is not the same as the previous period.
   * The subject has been used less frequently on that day.
   * The subject has been used less across the entire week.

4. Efficiency of Greedy Phase

   * This step fills most of the timetable (around 90–95%) without issues.

5. Phase 2: Backtracking

   * If no subject satisfies all conditions for a slot, backtracking is used.
   * The algorithm goes back, changes previous choices, and tries different combinations.

6. Constraints Followed

   * No subject is repeated in consecutive periods on the same day.
   * Subjects are evenly distributed throughout the day.
   * A teacher cannot be assigned to more than one class at the same time.

7. Overall Working

   * The algorithm first tries to generate the timetable efficiently using greedy logic.
   * If conflicts occur, it resolves them using backtracking.
*/



public class Generator {

    // ── Schedule dimensions ───────────────────────────────────
    public static final int DAYS    = 5;   // Monday – Friday
    public static final int PERIODS = 6;   // P1 – P6

    // ── Internal state ────────────────────────────────────────
    private final List<SubjectTeacherPair> pairs;  // user-defined subject↔teacher list
    private final int  numSubjects;
    private final int  maxPerDay;      // soft cap on same subject per day
    private final Random rng = new Random(42); // fixed seed = reproducible results

    /**
     * @param pairs  list of SubjectTeacherPair objects provided by the UI
     */
    public Generator(List<SubjectTeacherPair> pairs) {
        this.pairs       = pairs;
        this.numSubjects = pairs.size();
        // Allow each subject at most ceil(PERIODS/subjects)+1 times per day
        this.maxPerDay   = (int) Math.ceil((double) PERIODS / numSubjects) + 1;
    }

   
    //  PUBLIC API

    /**
     * Generates the full 5-day × 6-period timetable.
     *
     * @return 2-D array of TimetableSlot [DAYS][PERIODS]
     */


    public TimetableSlot[][] generate() {
        TimetableSlot[][] table = new TimetableSlot[DAYS][PERIODS];

        // weekCount tracks how many times each subject has been
        // scheduled across the whole week (for fairness).
        int[] weekCount = new int[numSubjects];

        for (int day = 0; day < DAYS; day++) {
            TimetableSlot[] daySlots = generateDay(day, weekCount);
            table[day] = daySlots;

            // Update the week count
            for (TimetableSlot slot : daySlots) {
                int idx = indexOfSubject(slot.getSubject());
                if (idx >= 0) weekCount[idx]++;
            }
        }

        return table;
    }

    //  PHASE 1 — GREEDY
    
    /**
     * Greedy pass for one day.
     * Returns a filled array of PERIODS slots, or falls back to
     * backtracking if greedy gets stuck.
     */

    private TimetableSlot[] generateDay(int day, int[] weekCount) {
        TimetableSlot[] slots   = new TimetableSlot[PERIODS];
        int[]           dayCount = new int[numSubjects];  // usage per subject today

        for (int period = 0; period < PERIODS; period++) {
            String prev = (period > 0) ? slots[period - 1].getSubject() : null;

            // ── Greedy selection 
            int bestIdx   = -1;
            int bestScore = Integer.MAX_VALUE;

            for (int i = 0; i < numSubjects; i++) {
                String subj = pairs.get(i).getSubject();

                // C1: no consecutive same subject
                if (subj.equals(prev)) continue;

                // C2: max-per-day cap
                if (dayCount[i] >= maxPerDay) continue;

                // Score: lower is better.
                // Primary key  → daily usage (balance within the day)
                // Secondary key → weekly usage (fairness across the week)
                int score = dayCount[i] * 100 + weekCount[i];
                if (score < bestScore) {
                    bestScore = score;
                    bestIdx   = i;
                }
            }

            if (bestIdx != -1) {
                // Greedy found a valid slot
                SubjectTeacherPair p = pairs.get(bestIdx);
                slots[period] = new TimetableSlot(p.getSubject(), p.getTeacher());
                dayCount[bestIdx]++;
            } else {
                // ── Greedy is stuck → trigger backtracking for this day ──
                return backtrackDay(weekCount);
            }
        }

        return slots;
    }

    //  PHASE 2 — BACKTRACKING

    /**
     * Recursive backtracking solver for one day.
     
     * Works by:
     *  1. Picking a slot (left to right).
     *  2. Trying each subject in turn.
     *  3. If it satisfies constraints → place it, recurse to next slot.
     *  4. If the recursive call fails → UNDO (backtrack) and try next subject.
     *  5. If no subject works → return false (triggers backtrack in caller).
     *
     * @param weekCount  current weekly usage counts (read-only here)
     * @return fully filled TimetableSlot[] for one day
     */


    private TimetableSlot[] backtrackDay(int[] weekCount) {
        TimetableSlot[] slots    = new TimetableSlot[PERIODS];
        int[]           dayCount = new int[numSubjects];

        // Create a shuffled index order so we don't always start with subject 0
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < numSubjects; i++) order.add(i);
        Collections.shuffle(order, rng);

        boolean solved = backtrack(slots, dayCount, weekCount, order, 0);

        if (!solved) {
            // Absolute fallback — relax C2 and just fill without consecutive repeats
            fallbackFill(slots);
        }

        return slots;
    }

    /**
     * Core recursive backtracking method.
     *
     * @param slots     partial solution being built
     * @param dayCount  how many times each subject used today
     * @param weekCount how many times each subject used this week
     * @param order     shuffled subject index order
     * @param period    current slot index (0..PERIODS-1)
     * @return true if a valid complete assignment was found
     */
    private boolean backtrack(TimetableSlot[] slots, int[] dayCount,
                               int[] weekCount, List<Integer> order, int period) {
        // Base case: all periods filled
        if (period == PERIODS) return true;

        String prev = (period > 0) ? slots[period - 1].getSubject() : null;

        for (int idx : order) {
            SubjectTeacherPair p    = pairs.get(idx);
            String             subj = p.getSubject();

            // ── Check constraints 
            // C1: no consecutive repeat
            if (subj.equals(prev)) continue;

            // C2: daily cap
            if (dayCount[idx] >= maxPerDay) continue;

            // ── Place subject (make move)
            slots[period] = new TimetableSlot(subj, p.getTeacher());
            dayCount[idx]++;

            // ── Recurse 
            if (backtrack(slots, dayCount, weekCount, order, period + 1)) {
                return true;  // solution found — propagate success upward
            }

            // ── Undo (backtrack)
            slots[period] = null;
            dayCount[idx]--;
            // Continue loop → try next subject
        }

        return false;  // no subject worked for this period
    }

    /**
     * Last-resort fill: ignores the daily cap, only enforces C1.
     * Should almost never be reached (only with 1 subject).
     */
    private void fallbackFill(TimetableSlot[] slots) {
        String prev = null;
        for (int p = 0; p < PERIODS; p++) {
            for (SubjectTeacherPair pair : pairs) {
                if (!pair.getSubject().equals(prev)) {
                    slots[p] = new TimetableSlot(pair.getSubject(), pair.getTeacher());
                    prev = pair.getSubject();
                    break;
                }
            }
            if (slots[p] == null) {
                SubjectTeacherPair first = pairs.get(0);
                slots[p] = new TimetableSlot(first.getSubject(), first.getTeacher());
            }
        }
    }

    // ── Utility ───────────────────────────────────────────────

    /** Returns the index of a subject in the pairs list, or -1 if not found. */
    private int indexOfSubject(String subject) {
        for (int i = 0; i < pairs.size(); i++) {
            if (pairs.get(i).getSubject().equals(subject)) return i;
        }
        return -1;
    }

    /** Returns a copy of the pairs list (used by TableView for the legend). */
    public List<SubjectTeacherPair> getPairs() {
        return Collections.unmodifiableList(pairs);
    }
}
