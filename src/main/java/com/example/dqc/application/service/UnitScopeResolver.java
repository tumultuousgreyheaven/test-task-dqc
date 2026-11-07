package com.example.dqc.application.service;

import com.example.dqc.application.exception.NotFoundException;
import com.example.dqc.application.model.UnitScope;
import com.example.dqc.application.repository.OrgUnitRepository;
import com.example.dqc.domain.model.OrgTree;
import com.example.dqc.domain.model.OrgUnit;
import org.springframework.stereotype.Component;

/** Находит подразделение и определяет, какие подразделения входят в его выборку (оно само и все потомки). */
@Component
final class UnitScopeResolver {

    private final OrgUnitRepository units;

    UnitScopeResolver(OrgUnitRepository units) {
        this.units = units;
    }

    /** @throws NotFoundException если такого подразделения нет */
    UnitScope resolve(String unitId) {
        OrgTree tree = new OrgTree(units.findAll());
        OrgUnit unit = tree.find(unitId).orElseThrow(() -> NotFoundException.unit(unitId));
        return new UnitScope(unit, tree.subtreeIds(unitId));
    }
}
