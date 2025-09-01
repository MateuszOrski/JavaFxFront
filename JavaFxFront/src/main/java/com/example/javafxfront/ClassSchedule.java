package com.example.javafxfront;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClassSchedule {
    private String subject;
    private String classroom;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String instructor;
    private String notes;
    private String groupName;
    private LocalDateTime createdDate;

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
    }

    // Getters
    public String getSubject() { return subject; }
    public String getClassroom() { return classroom; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getInstructor() { return instructor; }
    public String getNotes() { return notes; }
    public String getGroupName() { return groupName; }
    public LocalDateTime getCreatedDate() { return createdDate; }

    // Setters
    public void setSubject(String subject) { this.subject = subject; }
    public void setClassroom(String classroom) { this.classroom = classroom; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setInstructor(String instructor) { this.instructor = instructor; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

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

    @Override
    public String toString() {
        return subject + " - " + getFormattedTimeRange();
    }
}