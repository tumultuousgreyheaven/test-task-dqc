package com.example.dqc.application.service;

import com.example.dqc.application.exception.ConflictException;
import com.example.dqc.application.exception.InvalidRequestException;
import com.example.dqc.application.exception.NotFoundException;
import com.example.dqc.application.model.Run;
import com.example.dqc.application.model.RunStatus;
import com.example.dqc.application.repository.RunRepository;
import org.springframework.stereotype.Component;

/** Выбирает прогон для чтения: указанный или, если не указан, последний завершённый. */
@Component
final class RunResolver {

    private final RunRepository runs;

    RunResolver(RunRepository runs) {
        this.runs = runs;
    }

    /**
     * @param runId идентификатор прогона; null — последний завершённый
     * @throws NotFoundException       если прогона нет (или нет ни одного завершённого)
     * @throws ConflictException       если прогон ещё не завершён: результатов у него нет или они неполны
     * @throws InvalidRequestException если идентификатор пустой
     */
    Run resolveCompleted(String runId) {
        if (runId == null) {
            return runs.findLatestCompleted().orElseThrow(NotFoundException::noCompletedRuns);
        }
        if (runId.isBlank()) {
            throw new InvalidRequestException("run_id не должен быть пустым");
        }
        Run run = runs.findById(runId).orElseThrow(() -> NotFoundException.run(runId));
        if (run.status() != RunStatus.COMPLETED) {
            throw ConflictException.runNotCompleted(run.id(), run.status().code());
        }
        return run;
    }
}
