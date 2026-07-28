package com.meetingassistent.backend.service;

import com.meetingassistent.backend.model.Meeting;
import com.meetingassistent.backend.repository.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class SummaryService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Autowired
    private MeetingRepository meetingRepository;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.groq.com/openai/v1")
            .build();

    public Meeting generateSummary(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        if (meeting.getTranscript() == null || meeting.getTranscript().isBlank()) {
            throw new RuntimeException("No transcript available to summarize");
        }

        String prompt = """
                You are analyzing a meeting transcript. Based on the transcript below, provide:
                1. A concise summary (2-4 sentences)
                2. A list of action items (bullet points, or "None identified" if there are none)
                3. Any deadlines or specific dates mentioned (bullet points, or "None identified" if there are none)

                Respond ONLY in this exact format, no extra commentary:
                SUMMARY: <summary text>
                ACTION_ITEMS: <action items, separated by | if multiple>
                DEADLINES: <deadlines, separated by | if multiple>

                Transcript:
                %s
                """.formatted(meeting.getTranscript());

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + groqApiKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        parseAndSave(meeting, content);

        return meeting;
    }

    private void parseAndSave(Meeting meeting, String content) {
        String summary = extractSection(content, "SUMMARY:", "ACTION_ITEMS:");
        String actionItems = extractSection(content, "ACTION_ITEMS:", "DEADLINES:");
        String deadlines = extractSection(content, "DEADLINES:", null);

        meeting.setSummary(summary.trim());
        meeting.setActionItems(actionItems.trim());
        meeting.setDeadlines(deadlines.trim());

        meetingRepository.save(meeting);
    }

    private String extractSection(String content, String startMarker, String endMarker) {
        int start = content.indexOf(startMarker);
        if (start == -1) return "Not available";
        start += startMarker.length();

        int end = (endMarker != null) ? content.indexOf(endMarker, start) : content.length();
        if (end == -1) end = content.length();

        return content.substring(start, end).trim();
    }
}