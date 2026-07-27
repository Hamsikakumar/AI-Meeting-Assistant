package com.meetingassistent.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "meetings")
@Getter
@Setter
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storedFilename; // the actual name on disk, to avoid collisions

    @Enumerated(EnumType.STRING)
    private MeetingStatus status = MeetingStatus.UPLOADED;

    private Instant createdAt = Instant.now();
}
