package com.rinco.vnanalytics.api.staff.controller;

import com.rinco.vnanalytics.api.staff.model.StaffPersonsResponse;
import com.rinco.vnanalytics.api.staff.service.StaffService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping("/persons")
    public StaffPersonsResponse getPersons() {
        return staffService.fetchPersons();
    }
}
