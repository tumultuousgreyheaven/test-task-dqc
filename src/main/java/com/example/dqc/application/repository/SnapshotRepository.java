package com.example.dqc.application.repository;

import com.example.dqc.domain.model.QualitySnapshot;

import java.time.LocalDate;

public interface SnapshotRepository {

    /**
     * Загружает текущие данные (подразделения, цели, КР) в снимок для правил и метрик.
     * Для согласованности трёх запросов вызывать внутри одной транзакции с изоляцией REPEATABLE READ.
     *
     * @param asOf дата снимка, которая будет доступна правилам
     */
    QualitySnapshot load(LocalDate asOf);
}
