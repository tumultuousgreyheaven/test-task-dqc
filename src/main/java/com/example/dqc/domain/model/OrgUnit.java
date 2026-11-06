package com.example.dqc.domain.model;

import java.util.Objects;

/** Подразделение. {@code parentId == null} у корневого. */
public record OrgUnit(String id, String name, String parentId) {

    public OrgUnit {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
