package com.example.dqc.domain.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Иерархия подразделений. Неизменяемая; построение и обход защищены от циклов. */
public final class OrgTree {

    private final Map<String, OrgUnit> unitsById = new LinkedHashMap<>();
    private final Map<String, List<String>> childrenByParentId = new HashMap<>();

    public OrgTree(Collection<OrgUnit> units) {
        for (OrgUnit unit : units) {
            if (unitsById.putIfAbsent(unit.id(), unit) != null) {
                throw new IllegalArgumentException("Подразделение указано дважды: " + unit.id());
            }
        }
        for (OrgUnit unit : unitsById.values()) {
            if (unit.parentId() != null) {
                childrenByParentId.computeIfAbsent(unit.parentId(), key -> new ArrayList<>()).add(unit.id());
            }
        }
    }

    public Collection<OrgUnit> units() {
        return Collections.unmodifiableCollection(unitsById.values());
    }

    public boolean contains(String unitId) {
        return unitsById.containsKey(unitId);
    }

    public Optional<OrgUnit> find(String unitId) {
        return Optional.ofNullable(unitsById.get(unitId));
    }

    /**
     * Идентификаторы подразделения и всех его потомков на любой глубине (само подразделение — первым).
     *
     * @throws IllegalArgumentException если подразделения нет в иерархии; наличие проверяют через {@link #contains}
     */
    public Set<String> subtreeIds(String unitId) {
        if (!unitsById.containsKey(unitId)) {
            throw new IllegalArgumentException("Неизвестное подразделение: " + unitId);
        }
        Set<String> result = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(unitId);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (result.add(current)) { // повторный заход = цикл в данных, второй раз не обходим
                queue.addAll(childrenByParentId.getOrDefault(current, List.of()));
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
