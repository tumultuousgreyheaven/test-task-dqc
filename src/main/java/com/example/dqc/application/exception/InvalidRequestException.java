package com.example.dqc.application.exception;

/** Параметры запроса не прошли проверку (HTTP 422). */
public class InvalidRequestException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public InvalidRequestException(String message) {
        super(ErrorCode.INVALID_PARAMETER, message);
    }
}
