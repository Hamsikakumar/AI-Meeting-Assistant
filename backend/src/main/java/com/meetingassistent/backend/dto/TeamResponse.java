package com.meetingassistent.backend.dto;

public record TeamResponse(
    Long id,
    String name,
    String inviteCode
) {}
