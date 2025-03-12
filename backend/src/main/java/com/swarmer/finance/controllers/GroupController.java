package com.swarmer.finance.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.swarmer.finance.dto.GroupDto;
import com.swarmer.finance.security.UserPrincipal;
import com.swarmer.finance.services.GroupService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    @Autowired
    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ResponseEntity<List<GroupDto>> getGroups(@AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime opdate) {
        Long userId = principal.getUserDto().id();
        return ResponseEntity.ok(groupService.getGroups(userId, opdate));
    }

    @PostMapping
    public ResponseEntity<GroupDto> createGroup(@AuthenticationPrincipal UserPrincipal principal,
            @RequestBody GroupDto groupDto) {
        Long userId = principal.getUserDto().id();
        return ResponseEntity.ok(groupService.createGroup(groupDto, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupDto> updateGroup(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody GroupDto groupDto) {
        if (!id.equals(groupDto.id())) {
            throw new IllegalArgumentException("Group id doesn't match");
        }
        Long userId = principal.getUserDto().id();
        return ResponseEntity.ok(groupService.updateGroup(groupDto, userId));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserDto().id();
        groupService.deleteGroup(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("users")
    public ResponseEntity<List<String>> findUsers(@RequestParam String query) {
        return ResponseEntity.ok(groupService.findUsers(query));
    }
}