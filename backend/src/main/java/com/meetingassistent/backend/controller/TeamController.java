package com.meetingassistent.backend.controller;

import com.meetingassistent.backend.dto.*;
import com.meetingassistent.backend.model.User;
import com.meetingassistent.backend.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired private TeamService teamService;

    @PostMapping("/create")
    public ResponseEntity<TeamResponse> createTeam(
            @RequestBody CreateTeamRequest req,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(teamService.createTeam(req.name(), user));
    }

    @PostMapping("/join")
    public ResponseEntity<TeamResponse> joinTeam(
            @RequestBody JoinTeamRequest req,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(teamService.joinTeam(req.inviteCode(), user));
    }

    @GetMapping("/me")
    public ResponseEntity<TeamResponse> getMyTeam(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(teamService.getMyTeam(user));
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leaveTeam(@AuthenticationPrincipal User user) {
        teamService.leaveTeam(user);
        return ResponseEntity.ok().build();
    }
}