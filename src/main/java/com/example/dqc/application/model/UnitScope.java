package com.example.dqc.application.model;

import com.example.dqc.domain.model.OrgUnit;

import java.util.Set;

/** Выбранное подразделение и идентификаторы его самого и всех потомков. */
public record UnitScope(OrgUnit unit, Set<String> unitIds) {

    public UnitScope {
        unitIds = Set.copyOf(unitIds);
    }
}
