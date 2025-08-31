package com.example.javafxfront;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Group {
    private String name;
    private String specialization;
    private LocalDateTime createdDate;

    public Group(String name, String specialization) {
        this.name = name;
        this.specialization = specialization;
        this.createdDate = LocalDateTime.now();
    }

    // Getters
    public String getName() { return name; }
    public String getSpecialization() { return specialization; }
    public LocalDateTime getCreatedDate() { return createdDate; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getFormattedDate() {
        return createdDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    @Override
    public String toString() {
        return name + " (" + specialization + ")";
    }
}