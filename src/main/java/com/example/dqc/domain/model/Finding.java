package com.example.dqc.domain.model;

import java.util.Objects;

/**
 * Находка правила: что именно не так и с каким объектом. Код правила и важность в находке
 * не хранятся, их проставляет {@code RuleEngine}, поэтому правило не может их перепутать.
 *
 * @param goalId цель, к которой относится проблема: для находки по цели совпадает с {@code objectId}
 *               (это проверяется), для находки по КР — цель этого КР
 * @param unitId подразделение этой цели
 */
public record Finding(ObjectType objectType, String objectId, String goalId, String unitId, String message) {

    public Finding {
        Objects.requireNonNull(objectType, "objectType");
        Objects.requireNonNull(objectId, "objectId");
        Objects.requireNonNull(goalId, "goalId");
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(message, "message");
        if (objectType == ObjectType.GOAL && !objectId.equals(goalId)) {
            throw new IllegalArgumentException(
                    "У находки по цели objectId и goalId должны совпадать: %s != %s".formatted(objectId, goalId));
        }
    }
}
