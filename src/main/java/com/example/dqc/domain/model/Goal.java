package com.example.dqc.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Цель. {@code owner}, {@code parentGoalId} и {@code dueDate} могут быть null:
 * отсутствие ответственного как раз и ловит правило GOAL_NO_OWNER.
 */
public record Goal(
        String id,
        String unitId,
        String title,
        String owner,
        String parentGoalId,
        GoalStatus status,
        LocalDate dueDate) {

    public Goal {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(status, "status");
    }

    public boolean isActive() {
        return status == GoalStatus.ACTIVE;
    }
}
