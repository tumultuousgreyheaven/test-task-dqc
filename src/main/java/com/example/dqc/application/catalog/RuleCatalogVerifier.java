package com.example.dqc.application.catalog;

import com.example.dqc.application.model.RuleDefinition;
import com.example.dqc.domain.rule.QualityRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Сверяет правила, реализованные в коде, со справочником kkd_rule и выбирает те, что включены.
 * Чистая функция: ни БД, ни Spring здесь нет.
 */
public final class RuleCatalogVerifier {

    private RuleCatalogVerifier() {
    }

    /**
     * Проверяет, что код и справочник согласованы, и возвращает включённые правила.
     * Выключенное в справочнике правило (is_active = false) просто не применяется.
     *
     * @throws IllegalStateException если нарушено любое из условий:
     *                               у каждой реализации есть строка справочника с той же важностью,
     *                               у каждой включённой строки справочника есть реализация
     */
    public static List<QualityRule> enabledRules(Collection<QualityRule> implementations, Collection<RuleDefinition> catalog) {
        Map<String, RuleDefinition> definitions = new HashMap<>();
        for (RuleDefinition definition : catalog) {
            definitions.put(definition.code(), definition);
        }

        List<QualityRule> sorted = new ArrayList<>(implementations);
        sorted.sort(Comparator.comparing(QualityRule::code));

        List<String> problems = new ArrayList<>();
        Set<String> implemented = new HashSet<>();
        for (QualityRule rule : sorted) {
            implemented.add(rule.code());
            RuleDefinition definition = definitions.get(rule.code());
            if (definition == null) {
                problems.add("правило %s реализовано в коде, но его нет в справочнике kkd_rule"
                        .formatted(rule.code()));
            } else if (definition.severity() != rule.severity()) {
                problems.add("важность правила %s в коде (%s) не совпадает со справочником (%s)"
                        .formatted(rule.code(), rule.severity().code(), definition.severity().code()));
            }
        }
        definitions.values().stream()
                .filter(definition -> definition.active() && !implemented.contains(definition.code()))
                .sorted(Comparator.comparing(RuleDefinition::code))
                .forEach(definition -> problems.add(
                        "правило %s включено в справочнике kkd_rule, но не реализовано в коде"
                                .formatted(definition.code())));

        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Справочник правил kkd_rule не соответствует коду:\n - " + String.join("\n - ", problems));
        }
        return sorted.stream()
                .filter(rule -> definitions.get(rule.code()).active())
                .toList();
    }
}
