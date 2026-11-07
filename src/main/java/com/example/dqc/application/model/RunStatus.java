package com.example.dqc.application.model;

/** Статус прогона ККД. */
public enum RunStatus {
    RUNNING("running"),
    COMPLETED("completed"),
    FAILED("failed");

    private final String code;

    RunStatus(String code) {
        this.code = code;
    }

    /** Значение в БД и в JSON. */
    public String code() {
        return code;
    }

    public static RunStatus fromCode(String code) {
        for (RunStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Неизвестный статус прогона: " + code);
    }
}
