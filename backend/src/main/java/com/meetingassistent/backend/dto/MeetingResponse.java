package com.meetingassistent.backend.dto;

import com.meetingassistent.backend.model.MeetingStatus;
import java.time.Instant;

public record MeetingResponse(
    Long id,
    String originalFilename,
    MeetingStatus status,
    Instant createdAt,
    String transcript,
    String summary,
    String actionItems,
    String deadlines,
    String speakerTranscript
) {}