package com.meetingassistent.backend.service;

import com.meetingassistent.backend.dto.TeamResponse;
import com.meetingassistent.backend.model.Team;
import com.meetingassistent.backend.model.User;
import com.meetingassistent.backend.repository.TeamRepository;
import com.meetingassistent.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TeamService {

    @Autowired private TeamRepository teamRepository;
    @Autowired private UserRepository userRepository;

    public TeamResponse createTeam(String name, User user) {
        if (user.getTeam() != null) {
            throw new RuntimeException("You are already in a team. Leave your current team first.");
        }

        Team team = new Team();
        team.setName(name);
        team.setInviteCode(generateInviteCode());
        teamRepository.save(team);

        user.setTeam(team);
        userRepository.save(user);

        return toResponse(team);
    }

    public TeamResponse joinTeam(String inviteCode, User user) {
        if (user.getTeam() != null) {
            throw new RuntimeException("You are already in a team. Leave your current team first.");
        }

        Team team = teamRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Invalid invite code"));

        user.setTeam(team);
        userRepository.save(user);

        return toResponse(team);
    }

    public TeamResponse getMyTeam(User user) {
        if (user.getTeam() == null) {
            return null;
        }
        return toResponse(user.getTeam());
    }

    public void leaveTeam(User user) {
        user.setTeam(null);
        userRepository.save(user);
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getInviteCode());
    }
}