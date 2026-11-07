package com.example.dqc.api;

import com.example.dqc.api.dto.ItemsResponse;
import com.example.dqc.api.dto.RunDto;
import com.example.dqc.api.dto.StartRunRequest;
import com.example.dqc.application.service.RunQueryService;
import com.example.dqc.application.service.RunService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Прогоны ККД: список и запуск. Только разбор запроса и формирование ответа, логика в сервисах. */
@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final RunQueryService runQuery;
    private final RunService runService;

    public RunController(RunQueryService runQuery, RunService runService) {
        this.runQuery = runQuery;
        this.runService = runService;
    }

    @GetMapping
    public ItemsResponse<RunDto> list() {
        return new ItemsResponse<>(runQuery.list().stream().map(RunDto::from).toList());
    }

    /** Запускает ККД по текущим данным. Тело необязательно: {@code {"snapshot_date": "2026-10-26"}}. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RunDto start(@RequestBody(required = false) StartRunRequest request) {
        return RunDto.from(runService.start(request == null ? null : request.snapshotDate()));
    }
}
