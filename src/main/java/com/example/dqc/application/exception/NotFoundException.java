package com.example.dqc.application.exception;

/** Запрошенный прогон или подразделение не существует (HTTP 404). */
public class NotFoundException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(ErrorCode code, String message) {
        super(code, message);
    }

    public static NotFoundException run(String runId) {
        return new NotFoundException(ErrorCode.RUN_NOT_FOUND, "Прогон не найден: " + runId);
    }

    public static NotFoundException noCompletedRuns() {
        return new NotFoundException(ErrorCode.RUN_NOT_FOUND, "Нет ни одного завершённого прогона");
    }

    public static NotFoundException unit(String unitId) {
        return new NotFoundException(ErrorCode.UNIT_NOT_FOUND, "Подразделение не найдено: " + unitId);
    }
}
