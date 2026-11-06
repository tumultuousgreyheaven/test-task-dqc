package com.example.dqc.domain.metric;

/** Признак "лучше / хуже" относительно прошлого прогона. */
public enum Trend {
    BETTER("better"),
    WORSE("worse"),
    SAME("same");

    private final String code;

    Trend(String code) {
        this.code = code;
    }

    /** Значение в JSON. */
    public String code() {
        return code;
    }
}
