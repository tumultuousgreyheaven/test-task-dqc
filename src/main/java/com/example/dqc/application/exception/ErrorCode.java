package com.example.dqc.application.exception;

/** Машиночитаемый код ошибки; попадает в поле {@code error.code} ответа API. */
public enum ErrorCode {
    RUN_NOT_FOUND,
    UNIT_NOT_FOUND,
    RUN_NOT_COMPLETED,
    INVALID_PARAMETER
}
