package com.example.dqc.domain.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Неизменяемый снимок данных, на котором работают правила и метрики. Загружается из БД
 * несколькими запросами; сами правила ни БД, ни Spring не знают.
 *
 * <p>Отбор "только активные цели" делается здесь, а не в правилах: правило перебирает
 * {@link #activeGoals()} и по построению не может проверить неактивную цель или её КР.
 */
public final class QualitySnapshot {

    private final LocalDate asOf;
    private final OrgTree orgTree;
    private final List<Goal> goals;
    private final List<Goal> activeGoals;
    private final Map<String, Goal> goalsById;
    private final Map<String, List<KeyResult>> keyResultsByGoalId;

    /**
     * @param asOf дата снимка; правила, зависящие от времени (например, "факт не обновлялся 45 дней"),
     *             должны брать её отсюда, а не из системных часов
     */
    public QualitySnapshot(
            LocalDate asOf,
            Collection<OrgUnit> units,
            Collection<Goal> goals,
            Collection<KeyResult> keyResults) {
        this.asOf = Objects.requireNonNull(asOf, "asOf");
        this.orgTree = new OrgTree(units);
        this.goals = List.copyOf(goals);

        Map<String, Goal> byId = new LinkedHashMap<>();
        for (Goal goal : this.goals) {
            if (byId.putIfAbsent(goal.id(), goal) != null) {
                throw new IllegalArgumentException("Цель указана дважды: " + goal.id());
            }
        }
        this.goalsById = Collections.unmodifiableMap(byId);
        this.activeGoals = this.goals.stream().filter(Goal::isActive).toList();

        Map<String, List<KeyResult>> grouped = new HashMap<>();
        for (KeyResult keyResult : keyResults) {
            grouped.computeIfAbsent(keyResult.goalId(), key -> new ArrayList<>()).add(keyResult);
        }
        grouped.replaceAll((goalId, list) -> List.copyOf(list));
        this.keyResultsByGoalId = Collections.unmodifiableMap(grouped);
    }

    public LocalDate asOf() {
        return asOf;
    }

    public OrgTree orgTree() {
        return orgTree;
    }

    /** Все цели независимо от статуса (нужны, чтобы пройти вверх по цепочке родителей). */
    public List<Goal> goals() {
        return goals;
    }

    /** Цели в статусе active, в порядке загрузки. */
    public List<Goal> activeGoals() {
        return activeGoals;
    }

    public Optional<Goal> goal(String goalId) {
        return Optional.ofNullable(goalsById.get(goalId));
    }

    /** КР цели; пустой список, если у цели их нет. */
    public List<KeyResult> keyResultsOf(String goalId) {
        return keyResultsByGoalId.getOrDefault(goalId, List.of());
    }
}
