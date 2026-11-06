package com.example.dqc.domain.metric;

import java.math.BigDecimal;

/** Сравнивает текущее значение метрики с прошлым. */
public final class MetricComparator {

    private MetricComparator() {
    }

    /**
     * Дельта считается как разность <b>округлённых</b> значений: тогда цифры в ответе сходятся
     * (62.7 → 56.5 даёт -6.2, а не -6.1 из-за скрытых знаков).
     *
     * @param current  текущая доля подразделения (с учётом дочерних)
     * @param previous доля в прошлом прогоне; {@code null}, если сравнивать не с чем
     *                 (первый прогон или метрика тогда не считалась): прошлое значение и дельта будут null, а не 0
     */
    public static MetricReading compare(Ratio current, Ratio previous, boolean higherIsBetter) {
        BigDecimal value = current.percent().orElse(null);
        BigDecimal previousValue = previous == null ? null : previous.percent().orElse(null);
        if (value == null || previousValue == null) {
            return new MetricReading(value, previousValue, null, null);
        }
        BigDecimal delta = value.subtract(previousValue);
        int sign = delta.signum();
        Trend trend;
        if (sign == 0) {
            trend = Trend.SAME;
        } else {
            trend = (sign > 0) == higherIsBetter ? Trend.BETTER : Trend.WORSE;
        }
        return new MetricReading(value, previousValue, delta, trend);
    }
}
