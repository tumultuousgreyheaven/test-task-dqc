package com.example.dqc.application.service;

import com.example.dqc.application.model.RunListItem;
import com.example.dqc.application.repository.RunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Чтение списка прогонов. */
@Service
public class RunQueryService {

    private final RunRepository runs;

    public RunQueryService(RunRepository runs) {
        this.runs = runs;
    }

    /** Все прогоны, новые первыми, с числом найденных проблем. */
    @Transactional(readOnly = true)
    public List<RunListItem> list() {
        return runs.findAllWithIssueCount();
    }
}
