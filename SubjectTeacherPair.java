/*  SubjectTeacherPair.java  —  Data Model  
  
  A simple container that links one subject to one teacher.
   Created from the custom input table in the UI so the user
   can define their own subjects and teachers instead of using hardcoded values.
 */
public class SubjectTeacherPair {

    private String subject;
    private String teacher;

    public SubjectTeacherPair(String subject, String teacher) {
        this.subject = subject;
        this.teacher = teacher;
    }

    public String getSubject() { return subject; }
    public String getTeacher() { return teacher; }

    public void setSubject(String subject) { this.subject = subject; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    @Override
    public String toString() {
        return subject + " → " + teacher;
    }
}
