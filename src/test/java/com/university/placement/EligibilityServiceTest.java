package com.university.placement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EligibilityServiceTest {

    private final EligibilityService service = new EligibilityService();

    private Student student(int age, double cgpa, int backlogs, int aptitude) {
        return new Student(
                "STU101", "Aarav Sharma", age, cgpa,
                backlogs, aptitude, "Computer Science");
    }

    @Test
    void qualifiesWhenEveryCriterionIsMet() {
        EligibilityResult result = service.evaluate(student(20, 8.2, 0, 75));

        assertEquals("ELIGIBLE", result.status());
        assertTrue(result.reasons().isEmpty());
    }

    @Test
    void identifiesConditionalEligibilityForAptitudeScoreOf55() {
        EligibilityResult result = service.evaluate(student(20, 7.0, 0, 55));

        assertEquals("CONDITIONALLY_ELIGIBLE", result.status());
        assertEquals(1, result.reasons().size());
    }

    @Test
    void rejectsStudentWithSeveralFailuresAndReportsEveryReason() {
        EligibilityResult result = service.evaluate(student(17, 6.5, 2, 40));

        assertEquals("NOT_ELIGIBLE", result.status());
        assertEquals(4, result.reasons().size());
    }

    @Test
    void acceptsExactEligibilityBoundaries() {
        EligibilityResult result = service.evaluate(student(18, 7.0, 0, 60));

        assertEquals("ELIGIBLE", result.status());
    }

    @Test
    void rejectsInvalidCgpa() {
        assertThrows(
                IllegalArgumentException.class,
                () -> student(20, 10.1, 0, 70));
    }

    @Test
    void rejectsNegativeBacklogs() {
        assertThrows(
                IllegalArgumentException.class,
                () -> student(20, 8.0, -1, 70));
    }
}
