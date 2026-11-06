package com.example.dqc.domain.metric;

import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.OrgUnit;
import com.example.dqc.domain.model.QualitySnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Считает все метрики по снимку и проблемам прогона.
 *
 * <p>Для каждой метрики возвращает значение по <b>каждому</b> подразделению (0/0, если объектов нет).
 * Тогда отсутствие строки у предыдущего прогона однозначно значит "метрика тогда не считалась"
 * (в API это {@code previous = null}), а не "объектов не было".
 */
public final class MetricCalculator {

    private final List<QualityMetric> metrics;

    public MetricCalculator(Collection<? extends QualityMetric> metrics) {
        List<QualityMetric> sorted = new ArrayList<>(metrics);
        sorted.sort(Comparator.comparing(QualityMetric::key));
        Set<String> keys = new HashSet<>();
        for (QualityMetric metric : sorted) {
            if (!keys.add(metric.key())) {
                throw new IllegalArgumentException("Две метрики с ключом " + metric.key());
            }
        }
        this.metrics = List.copyOf(sorted);
    }

    public List<QualityMetric> metrics() {
        return metrics;
    }

    public List<MetricValue> calculate(QualitySnapshot snapshot, List<Issue> issues) {
        List<MetricValue> result = new ArrayList<>();
        for (QualityMetric metric : metrics) {
            Map<String, Ratio> own = metric.measure(snapshot, issues);
            for (String unitId : own.keySet()) {
                if (!snapshot.orgTree().contains(unitId)) {
                    throw new IllegalStateException(
                            "Метрика %s вернула значение для неизвестного подразделения %s"
                                    .formatted(metric.key(), unitId));
                }
            }
            for (OrgUnit unit : snapshot.orgTree().units()) {
                result.add(new MetricValue(metric.key(), unit.id(), own.getOrDefault(unit.id(), Ratio.ZERO)));
            }
        }
        return List.copyOf(result);
    }
}
