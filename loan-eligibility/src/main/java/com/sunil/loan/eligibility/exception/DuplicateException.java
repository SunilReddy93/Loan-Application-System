package com.sunil.loan.eligibility.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DuplicateException extends RuntimeException {

    private final HttpStatus status;

    public DuplicateException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
