/**
 * ============================================================
 *  TimetableSlot.java  —  Data Model
 * ============================================================
 *  Represents one cell in the timetable grid.
 *
 *  A slot holds:
 *   - the subject name
 *   - the teacher assigned to that subject
 *   - a conflict flag (set by ConflictDetector)
 *
 *  Using a model class instead of raw Strings makes it easy
 *  to pass data between layers without parsing HTML or strings.
 * ============================================================
 */
public class TimetableSlot {

    private final String subject;   // e.g. "Mathematics"
    private final String teacher;   // e.g. "Mr. Sharma"
    private boolean hasConflict;    // true  → cell will be highlighted red

    public TimetableSlot(String subject, String teacher) {
        this.subject     = subject;
        this.teacher     = teacher;
        this.hasConflict = false;
    }

    // ── Getters ──────────────────────────────────────────────
    public String  getSubject()     { return subject; }
    public String  getTeacher()     { return teacher; }
    public boolean hasConflict()    { return hasConflict; }

    // ── Setter (used by ConflictDetector) ────────────────────
    public void setConflict(boolean conflict) { this.hasConflict = conflict; }

    @Override
    public String toString() {
        return subject + " / " + teacher;
    }
}
