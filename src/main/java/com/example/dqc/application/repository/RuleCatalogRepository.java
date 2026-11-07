package com.example.dqc.application.repository;

import com.example.dqc.application.model.RuleDefinition;

import java.util.List;

public interface RuleCatalogRepository {

    /** Весь справочник правил, включая выключенные. */
    List<RuleDefinition> findAll();
}
