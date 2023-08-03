package com.ark.center.gateway.exception;

import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {

    private final Integer code;

    public AuthException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }

}


