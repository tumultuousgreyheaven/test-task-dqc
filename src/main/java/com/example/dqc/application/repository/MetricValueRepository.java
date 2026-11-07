package com.example.dqc.application.repository;

import com.example.dqc.domain.metric.MetricValue;

import java.util.Collection;
import java.util.List;

public interface MetricValueRepository {

    /** Сохраняет значения метрик прогона одним пакетом. */
    void saveAll(String runId, Collection<MetricValue> values);

    /** Значения всех метрик прогона по всем подразделениям (только собственные объекты подразделений). */
    List<MetricValue> findByRun(String runId);
}
