package com.example.dqc.domain.model;

/** Важность проблемы. Константы объявлены от самой серьёзной к наименее серьёзной. */
public enum Severity {
    CRITICAL("critical"),
    MAJOR("major"),
    MINOR("minor");

    private final String code;

    Severity(String code) {
        this.code = code;
    }

    /** Значение в БД и в JSON. */
    public String code() {
        return code;
    }

    public static Severity fromCode(String code) {
        for (Severity value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Неизвестная важность: " + code);
    }
}
