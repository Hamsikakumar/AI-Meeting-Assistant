package com.meetingassistent.backend.service;

import com.meetingassistent.backend.model.Meeting;
import com.meetingassistent.backend.repository.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
public class DiarizationService {

    @Value("${assemblyai.api.key}")
    private String assemblyApiKey;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private MeetingRepository meetingRepository;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.assemblyai.com/v2")
            .defaultHeader("Authorization", "")
            .build();

    public Meeting identifySpeakers(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        try {
            // Step 1: upload the audio file to AssemblyAI
            Path filePath = Paths.get(uploadDir).resolve(meeting.getStoredFilename());
            FileSystemResource fileResource = new FileSystemResource(filePath);

            Map<String, Object> uploadResponse = webClient.post()
                    .uri("/upload")
                    .header("Authorization", assemblyApiKey)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(BodyInserters.fromResource(fileResource))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String audioUrl = (String) uploadResponse.get("upload_url");

            // Step 2: request transcription with speaker labels
            Map<String, Object> transcriptRequest = Map.of(
                    "audio_url", audioUrl,
                    "speaker_labels", true
            );

            Map<String, Object> transcriptResponse = webClient.post()
                    .uri("/transcript")
                    .header("Authorization", assemblyApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(transcriptRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String transcriptId = (String) transcriptResponse.get("id");

            // Step 3: poll until complete
            String status = "processing";
            Map<String, Object> pollResponse = null;

            while (status.equals("processing") || status.equals("queued")) {
                Thread.sleep(3000); // wait 3 seconds between polls

                pollResponse = webClient.get()
                        .uri("/transcript/" + transcriptId)
                        .header("Authorization", assemblyApiKey)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                status = (String) pollResponse.get("status");
            }

            if (status.equals("error")) {
                throw new RuntimeException("AssemblyAI error: " + pollResponse.get("error"));
            }

            // Step 4: format the utterances into readable "Speaker A: text" lines
            List<Map<String, Object>> utterances = (List<Map<String, Object>>) pollResponse.get("utterances");
            StringBuilder formatted = new StringBuilder();

            if (utterances != null) {
                for (Map<String, Object> utterance : utterances) {
                    String speaker = "Speaker " + utterance.get("speaker");
                    String text = (String) utterance.get("text");
                    formatted.append(speaker).append(": ").append(text).append("\n\n");
                }
            }

            meeting.setSpeakerTranscript(formatted.toString().trim());
            meetingRepository.save(meeting);

            return meeting;

        } catch (Exception e) {
            throw new RuntimeException("Speaker identification failed: " + e.getMessage(), e);
        }
    }
}