package com.meetingassistent.backend.repository;

import com.meetingassistent.backend.model.Meeting;
import com.meetingassistent.backend.model.Team;
import com.meetingassistent.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByUserOrderByCreatedAtDesc(User user);
    List<Meeting> findByTeamOrderByCreatedAtDesc(Team team);
}