package com.example.dqc.application.repository;

import com.example.dqc.domain.model.OrgUnit;

import java.util.List;

public interface OrgUnitRepository {

    /** Все подразделения; из них строится иерархия (OrgTree). */
    List<OrgUnit> findAll();
}
