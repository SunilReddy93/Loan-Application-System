package com.sunil.loan.eligibility.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserInactiveException extends RuntimeException {

    private final HttpStatus status;

    public UserInactiveException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
