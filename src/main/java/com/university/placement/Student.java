package com.university.placement;

public record Student(
        String studentId,
        String name,
        int age,
        double cgpa,
        int activeBacklogs,
        int aptitudeScore,
        String department) {

    public Student {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("Student ID is required.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Student name is required.");
        }
        if (age < 1 || age > 120) {
            throw new IllegalArgumentException("Age must be between 1 and 120.");
        }
        if (!Double.isFinite(cgpa) || cgpa < 0 || cgpa > 10) {
            throw new IllegalArgumentException("CGPA must be between 0 and 10.");
        }
        if (activeBacklogs < 0) {
            throw new IllegalArgumentException("Active backlogs cannot be negative.");
        }
        if (aptitudeScore < 0 || aptitudeScore > 100) {
            throw new IllegalArgumentException("Aptitude score must be between 0 and 100.");
        }
        if (department == null || department.isBlank()) {
            throw new IllegalArgumentException("Department is required.");
        }
    }
}
