@echo off
echo Compiling Timetable Generator v2...
if not exist out mkdir out
javac -d out src\Main.java src\TimetableSlot.java src\SubjectTeacherPair.java src\Generator.java src\ConflictDetector.java src\PDFExporter.java src\TableView.java src\UI.java
if %errorlevel% neq 0 (
    echo.
    echo Compilation failed. Ensure Java JDK is installed and on PATH.
    pause
    exit /b 1
)
echo Compilation successful. Launching...
java -cp out Main
pause
