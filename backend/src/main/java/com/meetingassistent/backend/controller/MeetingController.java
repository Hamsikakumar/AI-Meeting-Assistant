package com.meetingassistent.backend.controller;

import com.meetingassistent.backend.dto.MeetingResponse;
import com.meetingassistent.backend.model.Meeting;
import com.meetingassistent.backend.model.User;
import com.meetingassistent.backend.service.DiarizationService;
import com.meetingassistent.backend.service.MeetingService;
import com.meetingassistent.backend.service.SummaryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    @Autowired private MeetingService meetingService;

    @Autowired private SummaryService summaryService;

    @PostMapping("/{id}/summarize")
    public ResponseEntity<MeetingResponse> summarizeMeeting(@PathVariable Long id) {
        Meeting meeting = summaryService.generateSummary(id);
        return ResponseEntity.ok(meetingService.toResponseDto(meeting));
    }

    @PostMapping("/upload")
    public ResponseEntity<MeetingResponse> uploadMeeting(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(meetingService.uploadMeeting(file, user));
    }

    @GetMapping
    public ResponseEntity<List<MeetingResponse>> getMeetings(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(meetingService.getMeetingsForUser(user));
    }

    @Autowired private DiarizationService diarizationService;

    @PostMapping("/{id}/identify-speakers")
    public ResponseEntity<MeetingResponse> identifySpeakers(@PathVariable Long id) {
        Meeting meeting = diarizationService.identifySpeakers(id);
        return ResponseEntity.ok(meetingService.toResponseDto(meeting));
    }
}