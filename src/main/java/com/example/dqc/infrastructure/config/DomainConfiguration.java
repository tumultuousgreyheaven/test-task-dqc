package com.example.dqc.infrastructure.config;

import com.example.dqc.application.catalog.RuleCatalogVerifier;
import com.example.dqc.application.repository.RuleCatalogRepository;
import com.example.dqc.domain.metric.MetricCalculator;
import com.example.dqc.domain.metric.QualityMetric;
import com.example.dqc.domain.rule.QualityRule;
import com.example.dqc.domain.rule.RuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.util.List;

/**
 * Подключает доменную логику к Spring, не добавляя в домен ни одной аннотации.
 *
 * <p>Сканирование подбирает любой конкретный класс, реализующий {@link QualityRule} или
 * {@link QualityMetric}, в пакетах {@code domain.rule} и {@code domain.metric}: чтобы добавить
 * правило или метрику, достаточно написать класс (с конструктором без аргументов).
 * Интерфейсы и абстрактные классы сканирование пропускает само.
 */
@Configuration
@ComponentScan(
        basePackageClasses = {QualityRule.class, QualityMetric.class},
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {QualityRule.class, QualityMetric.class}))
public class DomainConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DomainConfiguration.class);

    /**
     * Движок строится из правил, найденных в коде, после сверки со справочником kkd_rule:
     * при расхождении кода и справочника приложение не стартует, правила с is_active = false не применяются.
     * Состав правил читается один раз при старте, изменение is_active требует перезапуска.
     */
    @Bean
    public RuleEngine ruleEngine(List<QualityRule> rules, RuleCatalogRepository catalog) {
        List<QualityRule> enabled = RuleCatalogVerifier.enabledRules(rules, catalog.findAll());
        log.info("Правила ККД: включено {} из {} реализованных: {}",
                enabled.size(), rules.size(), enabled.stream().map(QualityRule::code).toList());
        return new RuleEngine(enabled);
    }

    @Bean
    public MetricCalculator metricCalculator(List<QualityMetric> metrics) {
        log.info("Метрики ККД: {}", metrics.stream().map(QualityMetric::key).sorted().toList());
        return new MetricCalculator(metrics);
    }
}
