package com.example.dqc.api.dto;

import com.example.dqc.domain.model.Issue;

/** Проблема в списке GET /api/kkd/issues. */
public record IssueDto(
        String ruleCode,
        String severity,
        String objectType,
        String objectId,
        String goalId,
        String unitId,
        String message) {

    public static IssueDto from(Issue issue) {
        return new IssueDto(issue.ruleCode(), issue.severity().code(), issue.objectType().code(),
                issue.objectId(), issue.goalId(), issue.unitId(), issue.message());
    }
}
