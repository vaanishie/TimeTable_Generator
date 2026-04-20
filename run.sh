#!/bin/bash
echo "Compiling Timetable Generator v2..."
mkdir -p out
javac -d out src/Main.java src/TimetableSlot.java src/SubjectTeacherPair.java \
      src/Generator.java src/ConflictDetector.java src/PDFExporter.java \
      src/TableView.java src/UI.java

if [ $? -ne 0 ]; then
  echo "Compilation failed. Ensure Java JDK is installed."
  exit 1
fi

echo "Launching..."
java -cp out Main
