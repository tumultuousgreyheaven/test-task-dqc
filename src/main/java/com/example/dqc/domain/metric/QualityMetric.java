package com.example.dqc.domain.metric;

import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.QualitySnapshot;

import java.util.List;
import java.util.Map;

/**
 * Метрика качества данных: доля объектов, удовлетворяющих условию. Опирается на найденные
 * проблемы (то есть на правила), а не пересчитывает данные заново.
 */
public interface QualityMetric {

    /** Уникальный ключ, например {@code clean_goals_share}; он же {@code kkd_metric_value.metric_key}. */
    String key();

    String title();

    /** Единица измерения для API, например {@code %}. */
    String unit();

    /** true — рост значения это улучшение (определяет признак «лучше / хуже»). */
    boolean higherIsBetter();

    /**
     * Доли по подразделениям, считая <b>только объекты самого подразделения</b> (без дочерних):
     * суммирование по поддереву выполняется при чтении. Подразделения без объектов можно не включать.
     *
     * @param issues проблемы этого же прогона
     */
    Map<String, Ratio> measure(QualitySnapshot snapshot, List<Issue> issues);
}
