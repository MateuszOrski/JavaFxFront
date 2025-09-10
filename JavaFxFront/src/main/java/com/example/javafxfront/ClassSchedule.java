package com.example.javafxfront;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ClassSchedule {
    private Long id;
    private String subject;
    private String classroom;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String instructor;
    private String notes;
    private String groupName;
    private LocalDateTime createdDate;
    private List<Attendance> attendances; // DODANE - Lista uczestnictwa

    // Konstruktor dla nowych terminów (bez ID)
    public ClassSchedule(String subject, String classroom, LocalDateTime startTime,
                         LocalDateTime endTime, String instructor, String notes, String groupName) {
        this.subject = subject;
        this.classroom = classroom;
        this.startTime = startTime;
        this.endTime = endTime;
        this.instructor = instructor;
        this.notes = notes;
        this.groupName = groupName;
        this.createdDate = LocalDateTime.now();
        this.attendances = new ArrayList<>(); // DODANE
    }

    // Konstruktor dla terminów z serwera (z ID)
    public ClassSchedule(Long id, String subject, String classroom, LocalDateTime startTime,
                         LocalDateTime endTime, String instructor, String notes, String groupName,
                         LocalDateTime createdDate) {
        this.id = id;
        this.subject = subject;
        this.classroom = classroom;
        this.startTime = startTime;
        this.endTime = endTime;
        this.instructor = instructor;
        this.notes = notes;
        this.groupName = groupName;
        this.createdDate = createdDate;
        this.attendances = new ArrayList<>(); // DODANE
    }

    public Long getId() { return id; }
    public String getSubject() { return subject; }
    public String getClassroom() { return classroom; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getInstructor() { return instructor; }
    public String getNotes() { return notes; }
    public String getGroupName() { return groupName; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public List<Attendance> getAttendances() { return attendances; } // DODANE

    public void setId(Long id) { this.id = id; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setClassroom(String classroom) { this.classroom = classroom; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setInstructor(String instructor) { this.instructor = instructor; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public void setAttendances(List<Attendance> attendances) { this.attendances = attendances; } // DODANE

    public String getFormattedStartTime() {
        return startTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    public String getFormattedEndTime() {
        return endTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String getFormattedTimeRange() {
        return getFormattedStartTime() + " - " + getFormattedEndTime();
    }

    public String getFormattedCreatedDate() {
        return createdDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    public boolean isFromServer() {
        return id != null;
    }

    //Metody do zarządzania uczestnictwem
    public void addAttendance(Attendance attendance) {
        attendances.removeIf(a -> a.getStudent().getIndexNumber().equals(attendance.getStudent().getIndexNumber()));
        attendances.add(attendance);
    }

    public void removeAttendance(Student student) {
        attendances.removeIf(a -> a.getStudent().getIndexNumber().equals(student.getIndexNumber()));
    }

    public Attendance getAttendanceForStudent(Student student) {
        return attendances.stream()
                .filter(a -> a.getStudent().getIndexNumber().equals(student.getIndexNumber()))
                .findFirst()
                .orElse(null);
    }

    public boolean hasAttendanceForStudent(Student student) {
        return getAttendanceForStudent(student) != null;
    }

    public int getPresentCount() {
        return (int) attendances.stream().filter(a -> a.getStatus() == Attendance.Status.PRESENT).count();
    }

    public int getLateCount() {
        return (int) attendances.stream().filter(a -> a.getStatus() == Attendance.Status.LATE).count();
    }

    public int getAbsentCount() {
        return (int) attendances.stream().filter(a -> a.getStatus() == Attendance.Status.ABSENT).count();
    }

    public int getTotalAttendanceCount() {
        return attendances.size();
    }

    public String getAttendanceSummary() {
        int present = getPresentCount();
        int late = getLateCount();
        int absent = getAbsentCount();
        int total = getTotalAttendanceCount();

        if (total == 0) {
            return "Brak wpisów frekwencji";
        }

        return String.format("Obecni: %d | Spóźnieni: %d | Nieobecni: %d | Razem: %d",
                present, late, absent, total);
    }

    @Override
    public String toString() {
        return subject + " - " + getFormattedTimeRange();
    }
}