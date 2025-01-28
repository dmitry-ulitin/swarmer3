package com.swarmer.finance.controllers;

import org.springframework.security.core.Authentication;
import com.swarmer.finance.dto.UserPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swarmer.finance.dto.Dump;
import com.swarmer.finance.services.DataService;

@RestController
@RequestMapping("/api/backup")
public class DataController {
    private final DataService dataService;

    public DataController(DataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping
    public Dump getDump(Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        return dataService.getDump(userId);
    }

    @PutMapping
    public void loadDump(@RequestBody Dump dump, Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        dataService.loadDump(userId, dump);
    }
}
