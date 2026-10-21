package com.university.placement;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PlacementController {

    private final EligibilityService eligibilityService;

    public PlacementController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    @GetMapping("/health")
    public String health() {
        return "Student Placement Portal is running.";
    }

    @PostMapping("/eligibility")
    public EligibilityResult evaluate(@RequestBody Student student) {
        return eligibilityService.evaluate(student);
    }
}
