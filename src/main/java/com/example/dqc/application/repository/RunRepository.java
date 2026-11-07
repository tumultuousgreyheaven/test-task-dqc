package com.example.dqc.application.repository;

import com.example.dqc.application.model.Run;
import com.example.dqc.application.model.RunListItem;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RunRepository {

    /**
     * Создаёт прогон в статусе running. Идентификатор вида RUN-008 выдаёт последовательность БД.
     *
     * @param comparedToRunId прогон для сравнения; null, если сравнивать не с чем
     */
    Run create(LocalDate snapshotDate, String comparedToRunId);

    /** Переводит прогон в статус completed. */
    void markCompleted(String runId);

    Optional<Run> findById(String runId);

    /** Последний завершённый прогон (по дате снимка, затем по времени запуска). */
    Optional<Run> findLatestCompleted();

    /** Последний завершённый прогон со снимком строго раньше указанной даты. */
    Optional<Run> findLatestCompletedBefore(LocalDate snapshotDate);

    /** Все прогоны, новые первыми, с числом найденных проблем. */
    List<RunListItem> findAllWithIssueCount();
}
