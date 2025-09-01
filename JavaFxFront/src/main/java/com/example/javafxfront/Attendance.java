package com.example.javafxfront;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Attendance {
    public enum Status {
        PRESENT("Obecny", "#38A169"),
        LATE("Spóźniony", "#F56500"),
        ABSENT("Nieobecny", "#E53E3E");

        private final String displayName;
        private final String color;

        Status(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }

    private Student student;
    private ClassSchedule schedule;
    private Status status;
    private String notes;
    private LocalDateTime markedAt;

    public Attendance(Student student, ClassSchedule schedule, Status status) {
        this.student = student;
        this.schedule = schedule;
        this.status = status;
        this.notes = "";
        this.markedAt = LocalDateTime.now();
    }

    public Attendance(Student student, ClassSchedule schedule, Status status, String notes) {
        this.student = student;
        this.schedule = schedule;
        this.status = status;
        this.notes = notes;
        this.markedAt = LocalDateTime.now();
    }

    // Getters
    public Student getStudent() { return student; }
    public ClassSchedule getSchedule() { return schedule; }
    public Status getStatus() { return status; }
    public String getNotes() { return notes; }
    public LocalDateTime getMarkedAt() { return markedAt; }

    // Setters
    public void setStudent(Student student) { this.student = student; }
    public void setSchedule(ClassSchedule schedule) { this.schedule = schedule; }
    public void setStatus(Status status) {
        this.status = status;
        this.markedAt = LocalDateTime.now(); // Aktualizuj czas gdy zmieniany jest status
    }
    public void setNotes(String notes) { this.notes = notes; }
    public void setMarkedAt(LocalDateTime markedAt) { this.markedAt = markedAt; }

    public String getFormattedMarkedTime() {
        return markedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    @Override
    public String toString() {
        return student.getFullName() + " - " + status.getDisplayName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Attendance that = (Attendance) obj;
        return student.getIndexNumber().equals(that.student.getIndexNumber()) &&
                schedule.equals(that.schedule);
    }

    @Override
    public int hashCode() {
        return student.getIndexNumber().hashCode() + schedule.hashCode();
    }
}