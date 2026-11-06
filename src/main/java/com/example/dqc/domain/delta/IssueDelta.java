package com.example.dqc.domain.delta;

import com.example.dqc.domain.model.Issue;

import java.util.List;

/**
 * Изменение набора проблем между прогонами.
 *
 * @param newIssues  есть сейчас, в прошлом прогоне не было
 * @param persisting есть и сейчас, и было (берутся из текущего прогона)
 * @param resolved   было, сейчас нет (берутся из прошлого прогона)
 */
public record IssueDelta(List<Issue> newIssues, List<Issue> persisting, List<Issue> resolved) {

    public IssueDelta {
        newIssues = List.copyOf(newIssues);
        persisting = List.copyOf(persisting);
        resolved = List.copyOf(resolved);
    }
}
