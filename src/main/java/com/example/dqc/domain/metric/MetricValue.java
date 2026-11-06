package com.example.dqc.domain.metric;

import java.util.Objects;

/** Доля метрики по одному подразделению (только его собственные объекты) в одном прогоне — строка {@code kkd_metric_value}. */
public record MetricValue(String metricKey, String unitId, Ratio ratio) {

    public MetricValue {
        Objects.requireNonNull(metricKey, "metricKey");
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(ratio, "ratio");
    }
}
