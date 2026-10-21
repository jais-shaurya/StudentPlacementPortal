package com.university.placement;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EligibilityService {

    public EligibilityResult evaluate(Student student) {
        List<String> reasons = new ArrayList<>();

        if (student.age() < 18) {
            reasons.add("Age must be at least 18.");
        }
        if (student.cgpa() < 7.0) {
            reasons.add("CGPA must be at least 7.0.");
        }
        if (student.activeBacklogs() > 0) {
            reasons.add("Active backlogs must be zero.");
        }
        if (student.aptitudeScore() < 60) {
            reasons.add("Aptitude score must be at least 60.");
        }

        String status;

        if (reasons.isEmpty()) {
            status = "ELIGIBLE";
        } else if (student.age() >= 18
                && student.cgpa() >= 7.0
                && student.activeBacklogs() == 0
                && student.aptitudeScore() >= 55
                && student.aptitudeScore() < 60) {
            status = "CONDITIONALLY_ELIGIBLE";
        } else {
            status = "NOT_ELIGIBLE";
        }

        return new EligibilityResult(
                student.studentId(),
                student.name(),
                status,
                List.copyOf(reasons));
    }
}
