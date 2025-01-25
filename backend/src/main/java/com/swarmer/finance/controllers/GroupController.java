package com.swarmer.finance.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swarmer.finance.dto.GroupDto;
import com.swarmer.finance.dto.UserPrincipal;
import com.swarmer.finance.services.GroupService;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    @Transactional
    Iterable<GroupDto> getGroups(Authentication authentication,
            @RequestParam(required = false, defaultValue = "") String opdate) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        return groupService.getGroups(userId, opdate == null || opdate.isBlank() ? null
                : LocalDateTime.parse(opdate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[XXX]")));
    }

    @GetMapping("/{groupId}")
    @Transactional
    GroupDto getGroup(@PathVariable("groupId") Long groupId, Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        return groupService.getGroup(groupId, userId);
    }

    @PostMapping
    @Transactional
    GroupDto createGroup(@RequestBody GroupDto group, Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        return groupService.createGroup(group, userId);
    }

    @PutMapping("/{id}")
    @Transactional
    GroupDto updateGroup(@RequestBody GroupDto group, @PathVariable Long id, Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        return groupService.updateGroup(group, userId);
    }

    @DeleteMapping("/{id}")
    @Transactional
    void deleteGroup(@PathVariable("id") Long id, Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        groupService.deleteGroup(id, userId);
    }

    @GetMapping(value="users")
    public Iterable<String> findUsers(@RequestParam String query) {
        return groupService.findUsers(query);
    }    
}
