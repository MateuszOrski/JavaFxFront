package com.example.javafxfront;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Student {
    private String firstName;
    private String lastName;
    private String indexNumber;
    private String groupName; // Przypisana grupa
    private LocalDateTime addedDate;

    public Student(String firstName, String lastName, String indexNumber, String groupName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.indexNumber = indexNumber;
        this.groupName = groupName;
        this.addedDate = LocalDateTime.now();
    }

    // Getters
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getIndexNumber() { return indexNumber; }
    public String getGroupName() { return groupName; }
    public LocalDateTime getAddedDate() { return addedDate; }

    // Setters
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setIndexNumber(String indexNumber) { this.indexNumber = indexNumber; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getFormattedDate() {
        return addedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    @Override
    public String toString() {
        return getFullName() + " (" + indexNumber + ") - " + groupName;
    }
}