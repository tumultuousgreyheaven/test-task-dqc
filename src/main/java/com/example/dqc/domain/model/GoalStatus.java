package com.example.dqc.domain.model;

/** Статус цели. Правила ККД применяются только к целям в статусе {@link #ACTIVE}. */
public enum GoalStatus {
    DRAFT("draft"),
    ACTIVE("active"),
    ACHIEVED("achieved"),
    CANCELLED("cancelled");

    private final String code;

    GoalStatus(String code) {
        this.code = code;
    }

    /** Значение в БД. */
    public String code() {
        return code;
    }

    public static GoalStatus fromCode(String code) {
        for (GoalStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Неизвестный статус цели: " + code);
    }
}
