package com.example.dqc.api.dto;

/** Единый формат ошибки: {@code {"error": {"code": "...", "message": "..."}}}. */
public record ErrorResponse(Body error) {

    public record Body(String code, String message) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new Body(code, message));
    }
}
