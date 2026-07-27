package com.meetingassistent.backend.service;

import com.meetingassistent.backend.model.Meeting;
import com.meetingassistent.backend.model.MeetingStatus;
import com.meetingassistent.backend.repository.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class TranscriptionService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private MeetingRepository meetingRepository;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.groq.com/openai/v1")
            .build();

    @Async
    public void transcribeMeeting(Long meetingId, String storedFilename) {
        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        if (meeting == null) return;

        try {
            meeting.setStatus(MeetingStatus.PROCESSING);
            meetingRepository.save(meeting);

            Path filePath = Paths.get(uploadDir).resolve(storedFilename);

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new FileSystemResource(filePath));
            builder.part("model", "whisper-large-v3");

            Map<String, Object> response = webClient.post()
                    .uri("/audio/transcriptions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(org.springframework.web.reactive.function.BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String transcript = (String) response.get("text");

            meeting.setTranscript(transcript);
            meeting.setStatus(MeetingStatus.COMPLETED);
            meetingRepository.save(meeting);

        } catch (Exception e) {
            meeting.setStatus(MeetingStatus.FAILED);
            meetingRepository.save(meeting);
            System.err.println("Transcription failed for meeting " + meetingId + ": " + e.getMessage());
        }
    }
}