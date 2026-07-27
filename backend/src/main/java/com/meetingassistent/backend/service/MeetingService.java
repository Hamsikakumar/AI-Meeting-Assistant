package com.meetingassistent.backend.service;

import com.meetingassistent.backend.dto.MeetingResponse;
import com.meetingassistent.backend.model.Meeting;
import com.meetingassistent.backend.model.User;
import com.meetingassistent.backend.repository.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class MeetingService {

    @Autowired private MeetingRepository meetingRepository;
    @Autowired private FileStorageService fileStorageService;

    @Autowired private TranscriptionService transcriptionService;

    public MeetingResponse uploadMeeting(MultipartFile file, User user) {
        String storedFilename = fileStorageService.storeFile(file);

        Meeting meeting = new Meeting();
        meeting.setUser(user);
        meeting.setOriginalFilename(file.getOriginalFilename());
        meeting.setStoredFilename(storedFilename);

        meetingRepository.save(meeting);

        transcriptionService.transcribeMeeting(meeting.getId(), storedFilename);

        return toResponse(meeting);
    }

    public List<MeetingResponse> getMeetingsForUser(User user) {
        return meetingRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private MeetingResponse toResponse(Meeting meeting) {
        return new MeetingResponse(
            meeting.getId(),
            meeting.getOriginalFilename(),
            meeting.getStatus(),
            meeting.getCreatedAt(),
            meeting.getTranscript()
        );
    }
}