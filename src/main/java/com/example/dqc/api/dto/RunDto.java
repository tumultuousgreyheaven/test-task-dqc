package com.example.dqc.api.dto;

import com.example.dqc.application.model.Run;
import com.example.dqc.application.model.RunListItem;

import java.time.LocalDate;

/** Прогон в списке прогонов и в ответе на запуск. */
public record RunDto(String id, LocalDate snapshotDate, String status, String comparedToRunId, int issueCount) {

    public static RunDto from(RunListItem item) {
        Run run = item.run();
        return new RunDto(run.id(), run.snapshotDate(), run.status().code(), run.comparedToRunId(), item.issueCount());
    }
}
