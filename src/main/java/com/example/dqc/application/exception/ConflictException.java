package com.example.dqc.application.exception;

/** Запрос противоречит состоянию данных, например прогон ещё не завершён (HTTP 409). */
public class ConflictException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public ConflictException(ErrorCode code, String message) {
        super(code, message);
    }

    public static ConflictException runNotCompleted(String runId, String status) {
        return new ConflictException(ErrorCode.RUN_NOT_COMPLETED,
                "Прогон %s ещё не завершён (статус: %s)".formatted(runId, status));
    }
}
