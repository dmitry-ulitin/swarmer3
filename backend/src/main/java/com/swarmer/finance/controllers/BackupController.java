package com.swarmer.finance.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swarmer.finance.dto.dump.Dump;
import com.swarmer.finance.exceptions.BackupOwnerMismatchException;
import com.swarmer.finance.security.UserPrincipal;
import com.swarmer.finance.services.BackupService;

@RestController
@RequestMapping("/api/backup")
public class BackupController {
    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping
    public ResponseEntity<Dump> getDump(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(backupService.getDump(principal.getUserDto().id()));
    }

    @PutMapping
    public ResponseEntity<Void> loadDump(@AuthenticationPrincipal UserPrincipal principal, @RequestBody Dump dump) {
        var userId = principal.getUserDto().id();
        if (!dump.ownerId().equals(userId)) {
            throw new BackupOwnerMismatchException("Backup owner does not match user");
        }
        backupService.loadDump(userId, dump, false);
        return ResponseEntity.ok().build();
    }
}
