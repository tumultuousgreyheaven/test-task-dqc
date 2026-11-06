package com.example.dqc.domain.model;

/** Тип объекта, к которому относится проблема. */
public enum ObjectType {
    GOAL("goal"),
    KEY_RESULT("key_result");

    private final String code;

    ObjectType(String code) {
        this.code = code;
    }

    /** Значение в БД и в JSON. */
    public String code() {
        return code;
    }

    public static ObjectType fromCode(String code) {
        for (ObjectType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Неизвестный тип объекта: " + code);
    }
}
