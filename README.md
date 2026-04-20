## Timetable Generator v2 — Java Swing

A B.Tech CS-level desktop scheduling application using a **Greedy + Backtracking**
Constraint Satisfaction Problem (CSP) algorithm, conflict detection, PDF export,
and dark mode.


## Project Structure

```
TimetableV2/
├── src/
│   ├── Main.java              ← Entry point
│   ├── UI.java                ← Main input window (Swing)
│   ├── Generator.java         ← Greedy + Backtracking scheduler ⭐
│   ├── ConflictDetector.java  ← Post-generation conflict analysis
│   ├── TableView.java         ← Output window with JTable
│   ├── PDFExporter.java       ← PDF export via Java2D printing
│   ├── TimetableSlot.java     ← Data model: one timetable cell
│   └── SubjectTeacherPair.java← Data model: subject↔teacher link
├── run.bat                    ← Windows launcher
├── run.sh                     ← macOS/Linux launcher
└── README.md
```

---

## How to Run

### VS Code
1. Open the `TimetableV2` folder.
2. Install **Extension Pack for Java** (Microsoft) if needed.
3. Open `src/Main.java` → click **Run** above `main()`.

### Command Line
```bash
# macOS / Linux
chmod +x run.sh && ./run.sh

# Windows
run.bat
```

---

##  Algorithm: Greedy-First + Backtracking on Conflict

This is a classic **Constraint Satisfaction Problem (CSP)** approach.

### Phase 1 — Greedy Pass
```
For each slot (day × period):
  Score every available subject by:
    1. Daily usage count  (primary key   — prevents monopoly in a day)
    2. Weekly usage count (secondary key — balances across the week)
  Pick the subject with the LOWEST score that doesn't violate:
    C1. No consecutive repeat (same subject back-to-back)
    C2. Daily cap (max ⌈PERIODS/subjects⌉ + 1 appearances per day)
```
This fills ~95% of slots instantly without any backtracking.

### Phase 2 — Backtracking (triggered only if Greedy gets stuck)
```
backtrack(slots[], dayCount[], period):
  if period == PERIODS: return SUCCESS   ← base case

  for each subject (in shuffled order):
    if satisfies C1 and C2:
      place subject at slots[period]     ← make move
      if backtrack(..., period+1):
        return SUCCESS                   ← propagate success
      undo placement                     ← BACKTRACK
  
  return FAILURE  → caller will backtrack
```
The key insight: backtracking **undoes bad choices** recursively rather than
giving up or picking randomly.

---

## Conflict Detection

After generation, `ConflictDetector` scans the timetable for:

| Code | Conflict Type | Effect |
|------|--------------|--------|
| CF1 | Same subject in consecutive periods | Cell highlighted red + ⚠ badge |
| CF3 | Subject appears too many times in one day | Cell highlighted red + ⚠ badge |

A summary bar below the timetable lists all conflicts with day/period info.

---

## Features

| Feature | Detail |
|---------|--------|
| Custom subjects | Add/edit/remove subjects and teachers via editable table |
| Algorithm | Greedy Phase 1 + Backtracking Phase 2 CSP solver |
| Conflict detection | Red highlighting + conflict summary panel |
| Dark mode | Toggle with 🌙 button in both windows |
| PDF export | Uses Java's built-in PrinterJob + Graphics2D (no external lib) |
| Background thread | SwingWorker prevents UI freezing during generation |

---

## OOP Classes

| Class | Responsibility |
|-------|---------------|
| `Main` | Entry point, launches EDT |
| `UI` | Swing input form, theme management |
| `Generator` | Greedy + Backtracking CSP algorithm |
| `ConflictDetector` | Scans timetable, flags conflicts |
| `TableView` | Output JTable window, dark mode, export |
| `PDFExporter` | Printable interface, Graphics2D drawing |
| `TimetableSlot` | Data model: subject, teacher, conflict flag |
| `SubjectTeacherPair` | Data model: subject↔teacher mapping |

---

## Requirements

- Java **11** or newer (Java 17/21 recommended).
- No external libraries — pure Java SE + Swing.
