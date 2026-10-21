package com.university.placement;

import java.util.List;

public record EligibilityResult(
        String studentId,
        String studentName,
        String status,
        List<String> reasons) {
}
