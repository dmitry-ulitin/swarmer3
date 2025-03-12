package com.swarmer.finance.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class BackupOwnerMismatchException extends RuntimeException {
    public BackupOwnerMismatchException(String message) {
        super(message);
    }
} 