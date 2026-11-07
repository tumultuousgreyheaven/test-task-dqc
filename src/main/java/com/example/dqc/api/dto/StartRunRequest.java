package com.example.dqc.api.dto;

import java.time.LocalDate;

/** Тело POST /api/runs. Тело и поле необязательны: без даты берётся сегодняшняя. */
public record StartRunRequest(LocalDate snapshotDate) {
}
