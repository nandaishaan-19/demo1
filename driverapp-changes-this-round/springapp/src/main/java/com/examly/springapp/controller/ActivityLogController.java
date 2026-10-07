package com.examly.springapp.controller;

import com.examly.springapp.dto.ActivityLogDTO;
import com.examly.springapp.repository.ActivityLogRepo;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/** Lets an admin read the activity log written by the logging aspect (newest first). */
@RestController
@RequestMapping("/api/admin")
public class ActivityLogController {

    @Autowired
    private ActivityLogRepo activityLogRepo;

    @Autowired
    private ModelMapper modelMapper;

    /** GET /api/admin/logs?limit=100 - at most 500 rows. */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ActivityLogDTO>> viewLogs(@RequestParam(defaultValue = "100") int limit) {
        int size = Math.max(1, Math.min(limit, 500));
        List<ActivityLogDTO> logs = activityLogRepo.findAllByOrderByLoggedAtDescActivityLogIdDesc(PageRequest.of(0, size))
                .stream()
                .map(entry -> modelMapper.map(entry, ActivityLogDTO.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(logs);
    }
}
